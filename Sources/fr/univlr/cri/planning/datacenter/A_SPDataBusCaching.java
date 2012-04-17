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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.cocktail.fwkcktlwebapp.common.CktlLog;
import org.cocktail.fwkcktlwebapp.common.util.DateCtrl;
import org.cocktail.fwkcktlwebapp.common.util.FileCtrl;
import org.cocktail.fwkcktlwebapp.common.util.StringCtrl;
import org.cocktail.fwkcktlwebapp.common.util.SystemCtrl;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestamp;

import fr.univlr.cri.planning.constant.LocalSPConstantes;

/**
 * Databus peremettant de faire du stockage d'objet dans des dictionnaires, pour
 * accelerer les acces.
 * 
 * @author Cyril Tarade <cyril.tarade at univ-lr.fr>
 */

public abstract class A_SPDataBusCaching extends A_SPDataBus {

	public A_SPDataBusCaching(EOEditingContext ec) {
		super(ec);
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

	// /**
	// * Le dictionnaire contenant tous les objets
	// * sous forme de <code>Object</code>. La structure est :
	// *
	// * key : cle
	// * value : NSDictionary
	// * key=CACHE_KEY_TIMEOUT, value=(Long)<duree de vie max>
	// * key=CACHE_KEY_OBJECT value=(Object)<cache>
	// */
	// public abstract NSMutableDictionary dicoCache();

	/**
	 * Ajouter un objet <code>Object</code> dans le dictionnaire de cache. La
	 * duree de vie est initialisee.
	 */
	public void putObjectInCache(String calendarKey, Object object) {
		if (object == null) {
			return;
		}
		NSMutableDictionary dico = (NSMutableDictionary) readFromDisk(calendarKey);
		if (dico == null) {
			dico = new NSMutableDictionary();
			dico.setObjectForKey(object, CACHE_KEY_OBJECT);
			// duree de vie
			dico.setObjectForKey(
					new Long(System.currentTimeMillis() + ttlObject()), CACHE_KEY_TIMEOUT);
		}

		// enregistrer le dico sur le disque
		writeOnDisk(calendarKey, dico);
	}

	/**
	 * Recupere l'objet <code>Object</code> a partir du cache. Si ce dernier est
	 * trouve avec une duree de vie maximale non depassee, alors on le retourne.
	 * Si l'objet est trouve mais perime, il est supprime. Si l'objet est non
	 * trouve ou perime, alors on retourne <code>null</code>
	 */
	public Object getObjectFromCache(String calendarKey) {
		Object object = null;
		NSMutableDictionary dico = (NSMutableDictionary) readFromDisk(calendarKey);
		if (dico != null) {
			Long maxTime = (Long) dico.valueForKey(CACHE_KEY_TIMEOUT);
			long timeRemaining = (maxTime != null ? maxTime.longValue() - System.currentTimeMillis() : 0);
			// pas perime
			if (timeRemaining > 0) {
				CktlLog.trace("cache retrevied : [" + calendarKey + "] - " + timeRemaining + "ms remaining");
				object = dico.valueForKey(CACHE_KEY_OBJECT);
			} else {
				// obsolete : on efface
				FileCtrl.deleteFile(getFileNameCacheFile(calendarKey));
				// CktlLog.trace("cache removed : ["+calendarKey+"] - out-of-date since "
				// + (-timeRemaining) +"ms");
			}
		} else {
			CktlLog.trace("object not in cache : [" + calendarKey + "]");
		}
		return object;
	}

	private static String _fsSeparator;

	/**
	 * Le separateur entre les repertoire et fichier selon l'OS du serveur
	 * 
	 * @return
	 */
	public static String fsSeparator() {
		if (_fsSeparator == null) {
			if (SystemCtrl.systemId() == SystemCtrl.SYSTEM_WIN_9X ||
					SystemCtrl.systemId() == SystemCtrl.SYSTEM_WIN_NT) {
				_fsSeparator = "\\";
			} else {
				_fsSeparator = "/";
			}
		}
		return _fsSeparator;
	}

	/**
	 * Effectue les actions préalables à l'écriture d'un fichier sur le système de
	 * fichier de la machine : - verification du repertoire temporaire, et
	 * creation si besoin
	 * 
	 * @return le nom de base du répertoire de stockage des fichiers
	 */
	protected String doPreStoreDiskStuff() {

		String strTempDirectory = SystemCtrl.tempDir();
		if (!StringCtrl.isEmpty(strTempDirectory) && !strTempDirectory.endsWith(fsSeparator())) {
			strTempDirectory += fsSeparator();
		}
		strTempDirectory += LocalSPConstantes.ICS_CACHE_SUB_DIRECTORY;
		File fileTempDirectory = new File(strTempDirectory);
		if (!fileTempDirectory.isDirectory()) {
			CktlLog.log("creating cache directory : " + strTempDirectory);
			fileTempDirectory.mkdir();
		}

		return strTempDirectory;
	}

	/**
	 * Ecrire le contenu d'un dictionnaire sur le systeme de fichier de la machine
	 * pour une cle donnee
	 * 
	 * @param dico
	 * @return
	 */
	private boolean writeOnDisk(String calendarKey, NSMutableDictionary dico) {
		String fileName = getFileNameCacheFile(calendarKey);
		try {
			long l1 = System.currentTimeMillis();
			FileOutputStream fichier = new FileOutputStream(fileName);
			ObjectOutputStream oos = new ObjectOutputStream(fichier);
			oos.writeObject(dico);
			oos.flush();
			oos.close();
			l1 = System.currentTimeMillis() - l1;
			CktlLog.log("write cache file : " + l1 + " ms (" + fileName + ")");
			return true;
		} catch (java.io.IOException e) {
			e.printStackTrace();
			CktlLog.log("error writing cache file (" + fileName + ")");
			return false;
		}
	}

	/**
	 * Le nom du fichier de cache - suppression des caracteres speciaux qui
	 * pourrait poser probleme lors de l'ecriture sur le filesystem
	 * 
	 * @param calendarKey
	 * @return
	 */
	private String getFileNameCacheFile(String calendarKey) {
		String fullClassName = this.getClass().getName();
		String className = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
		return doPreStoreDiskStuff() + fsSeparator() + StringCtrl.toBasicString(
				className + "_" + calendarKey) + ".bin";
	}

	/**
	 * Lire un dictionnaire depuis la cle sur systeme de fichier de la machine
	 * 
	 * @param calendarKey
	 * @return
	 */
	private Object readFromDisk(String calendarKey) {
		String fileName = getFileNameCacheFile(calendarKey);
		try {
			long l1 = System.currentTimeMillis();
			FileInputStream fichier = new FileInputStream(fileName);
			ObjectInputStream ois = new ObjectInputStream(fichier);
			Object object = ois.readObject();
			l1 = System.currentTimeMillis() - l1;
			CktlLog.log("reading cache file : " + l1 + " ms (" + fileName + ")");
			return object;
		} catch (java.io.FileNotFoundException e) {
			CktlLog.log("cache file missing (" + fileName + ")");
			return null;
		} catch (java.io.IOException e) {
			e.printStackTrace();
			CktlLog.log("error reading cache file (" + fileName + ")");
			return null;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			CktlLog.log("error reading cache file (" + fileName + ")");
			return null;
		} catch (Throwable e) {
			e.printStackTrace();
			CktlLog.log("error reading cache file (" + fileName + ")");
			return null;
		}
	}

	private final static String DATE_FORMAT_KEY = "%d_%m_%Y_%H_%M";

	/**
	 * Construire la cle du dictionnaire pour le stockage du flux ics en cache.
	 * 
	 * @param methClient
	 * @param idKey
	 * @param idVal
	 * @param dDebut
	 * @param dFin
	 * @return
	 */
	protected String buildKey(
			String methClient, String idKey, String idVal, NSTimestamp dDebut, NSTimestamp dFin) {

		String key = methClient + "_" + idKey;

		if (idKey != null) {
			key += "_" + idKey;
		}

		if (idVal != null) {
			key += "_" + idVal;
		}

		if (dDebut != null && dFin != null) {
			key += "_" + DateCtrl.dateToString(dDebut, DATE_FORMAT_KEY) + "_" + DateCtrl.dateToString(dFin, DATE_FORMAT_KEY);
		}

		return key;
	}

	/**
	 * 
	 */
	public static void clearCache() {
		// dicoCache().removeAllObjects();

		String strTempDirectory = SystemCtrl.tempDir();
		if (!StringCtrl.isEmpty(strTempDirectory) && !strTempDirectory.endsWith(fsSeparator())) {
			strTempDirectory += fsSeparator();
		}
		strTempDirectory += LocalSPConstantes.ICS_CACHE_SUB_DIRECTORY;

		FileCtrl.cleanDir(strTempDirectory);

		CktlLog.log("clearing cache directory : " + strTempDirectory);

	}

}
