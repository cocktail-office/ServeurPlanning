/*
 * Copyright Consortium Coktail, 27 juin 07
 * 
 * cyril.tarade at univ-lr.fr
 * 
 * Ce logiciel est un programme informatique servant � [rappeler les
 * caract�ristiques techniques de votre logiciel]. 
 * 
 * Ce logiciel est r�gi par la licence CeCILL soumise au droit fran�ais et
 * respectant les principes de diffusion des logiciels libres. Vous pouvez
 * utiliser, modifier et/ou redistribuer ce programme sous les conditions
 * de la licence CeCILL telle que diffus�e par le CEA, le CNRS et l'INRIA 
 * sur le site "http://www.cecill.info".
 * 
 * En contrepartie de l'accessibilit� au code source et des droits de copie,
 * de modification et de redistribution accord�s par cette licence, il n'est
 * offert aux utilisateurs qu'une garantie limit�e.  Pour les m�mes raisons,
 * seule une responsabilit� restreinte p�se sur l'auteur du programme,  le
 * titulaire des droits patrimoniaux et les conc�dants successifs.
 * 
 * A cet �gard  l'attention de l'utilisateur est attir�e sur les risques
 * associ�s au chargement,  � l'utilisation,  � la modification et/ou au
 * d�veloppement et � la reproduction du logiciel par l'utilisateur �tant 
 * donn� sa sp�cificit� de logiciel libre, qui peut le rendre complexe � 
 * manipuler et qui le r�serve donc � des d�veloppeurs et des professionnels
 * avertis poss�dant  des  connaissances  informatiques approfondies.  Les
 * utilisateurs sont donc invit�s � charger  et  tester  l'ad�quation  du
 * logiciel � leurs besoins dans des conditions permettant d'assurer la
 * s�curit� de leurs syst�mes et ou de leurs donn�es et, plus g�n�ralement, 
 * � l'utiliser et l'exploiter dans les m�mes conditions de s�curit�. 
 * 
 * Le fait que vous puissiez acc�der � cet en-t�te signifie que vous avez 
 * pris connaissance de la licence CeCILL, et que vous en avez accept� les
 * termes.
 */

package fr.univlr.cri.planning.datacenter;

import org.cocktail.fwkcktlwebapp.common.CktlLog;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;

/**
 * Databus peremettant de faire du stockage d'objet dans des dictionnaires, pour
 * accelerer les acces.
 * 
 * @author Cyril Tarade <cyril.tarade at univ-lr.fr>
 */

public abstract class SPDataBusCaching /* extends SPDataBus */{

	public SPDataBusCaching(EOEditingContext ec) {
		// super(ec);
	}

	// ** les differents cache **

	/**
	 * Le nom des cles de chaque sous-dico du cache
	 */
	private final static String CACHE_KEY_TIMEOUT = "TIMEOUT";
	private final static String CACHE_KEY_OBJECT = "OBJECT";

	/**
	 * TTL d'un objet en cache (ms)
	 */
	public abstract int ttlObject();

	/**
	 * Le dictionnaire contenant tous les objets sous forme de <code>Object</code>
	 * . La structure est :
	 * 
	 * key : cle value : NSDictionary key=CACHE_KEY_TIMEOUT, value=(Long)<duree de
	 * vie max> key=CACHE_KEY_OBJECT value=(Object)<cache>
	 */
	public abstract NSMutableDictionary dicoCache();

	/**
	 * Ajouter un objet <code>Object</code> dans le dictionnaire de cache. La
	 * duree de vie est initialisee.
	 */
	public void putObjectInCache(String calendarKey, Object object) {
		if (object == null) {
			return;
		}
		NSDictionary existingDico = (NSDictionary) dicoCache().objectForKey(calendarKey);
		if (existingDico == null) {
			NSMutableDictionary newDico = new NSMutableDictionary();
			newDico.setObjectForKey(object, CACHE_KEY_OBJECT);
			// duree de vie
			newDico.setObjectForKey(
					new Long(System.currentTimeMillis() + ttlObject()), CACHE_KEY_TIMEOUT);
			dicoCache().setObjectForKey(newDico, calendarKey);
			// CktlLog.trace("caching : ["+calendarKey+"]");
		}
	}

	/**
	 * Recupere l'objet <code>Object</code> a partir du cache. Si ce dernier est
	 * trouve avec une duree de vie maximale non depassee, alors on le retourne.
	 * Si l'objet est trouve mais perime, il est supprime. Si l'objet est non
	 * trouve ou perime, alors on retourne <code>null</code>
	 */
	public Object getObjectFromCache(String key) {
		Object object = null;
		NSDictionary dico = (NSDictionary) dicoCache().objectForKey(key);
		if (dico != null) {
			Long maxTime = (Long) dico.valueForKey(CACHE_KEY_TIMEOUT);
			long timeRemaining = (maxTime != null ? maxTime.longValue() - System.currentTimeMillis() : 0);
			// pas perime
			if (timeRemaining > 0) {
				CktlLog.trace("cache retrevied : [" + key + "] - " + timeRemaining + "ms remaining");
				object = dico.valueForKey(CACHE_KEY_OBJECT);
			} else {
				// obsolete : on efface
				CktlLog.trace("cache removed : [" + key + "] - out-of-date since " + (-timeRemaining) + "ms");
				dicoCache().removeObjectForKey(key);
			}
		} else {
			CktlLog.trace("object not in cache : [" + key + "]");
		}
		return object;
	}

	/**
	 * 
	 */
	public void clearCache() {
		dicoCache().removeAllObjects();
	}
}
