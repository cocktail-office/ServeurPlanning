/*
 * Copyright COCKTAIL (www.cocktail.org), 1995, 2010 This software
 * is governed by the CeCILL license under French law and abiding by the
 * rules of distribution of free software. You can use, modify and/or
 * redistribute the software under the terms of the CeCILL license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability. In this
 * respect, the user's attention is drawn to the risks associated with loading,
 * using, modifying and/or developing or reproducing the software by the user
 * in light of its specific status of free software, that may mean that it
 * is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security. The
 * fact that you are presently reading this means that you have had knowledge
 * of the CeCILL license and that you accept its terms.
 */
package fr.univlr.cri.planning.datacenter;
import java.util.StringTokenizer;

import org.cocktail.fwkcktlwebapp.common.CktlLog;
import org.cocktail.fwkcktlwebapp.common.database.CktlRecord;
import org.cocktail.fwkcktlwebapp.common.util.NSArrayCtrl;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;

import fr.univlr.cri.planning.SPConstantes;
import fr.univlr.cri.planning.constant.LocalSPConstantes;

public class ParamBus extends A_SPDataBus {

	/*------ METHODES D'ACCES A LA BASE DE DONNEES-------*/

	public ParamBus(EOEditingContext arg0) {
		super(arg0);
	}

	/** **************************************************************************** */

	// recup key des methodes serveur associ�es a methode cliente, pour un idkey
	public NSArray fetchServMethodsForClient(Number keyC, String idkey) {

		NSMutableArray reponse = new NSMutableArray();

		if (keyC != null) // key trouv�e
		{
			// recup keyServ associ�es a keyClient de Repartition

			// NSArray ed = fetchArray(Constantes.BD_PARAM,
			// newCondition(Constantes.BD_PARAM_METH +"=%@", new NSArray(key)),
			// null);

			EOQualifier qual = newCondition(
					LocalSPConstantes.BD_REPAR_CLIENTMET + "=%@ AND "+ LocalSPConstantes.BD_REPAR_CLIENTID + "=%@", 
					new NSArray(new Object[]{keyC, new Integer(idkey)}));
			
			NSArray rep2 = fetchArray(LocalSPConstantes.BD_REPARTITION, qual, null);
			
			int in = rep2.count();
			for (int i = 0; i < in; i++) // pour chaque keyServ trouv�e
			{
				CktlRecord rep3 = (CktlRecord) rep2.objectAtIndex(i);
				Number repKeyS = rep3.numberForKey(LocalSPConstantes.BD_REPAR_SERVMET);
				reponse.addObject(repKeyS);
			} // fin For
		} // fin If

		return new NSArray(reponse);
	}

	/** **************************************************************************** */

	// trouve la key pour 1 uri methode
	public Number fetchKeyForNom(String nom) {

		Number repKey = null;

		NSMutableArray args = new NSMutableArray();
		args.addObject(nom);
		NSArray rep = fetchArray(LocalSPConstantes.BD_METHODE, newCondition(
				LocalSPConstantes.BD_METHODE_NOM + "=%@", new NSArray(args)), null);
		if (rep.count() != 0) // uri trouv�e
		{
			CktlRecord rep1 = (CktlRecord) rep.objectAtIndex(0);
			repKey = rep1.numberForKey(LocalSPConstantes.BD_METHODE_KEY);
		}

		return repKey;
	}

	// trouve l'uri pour 1 key methode

	public String fetchUriForKey(Number key) {

		String uri = null;
		NSMutableArray args = new NSMutableArray();
		args.addObject(key);
		NSArray rep = fetchArray(LocalSPConstantes.BD_MET_SERV, newCondition(
				LocalSPConstantes.BD_SERVEUR_KEY + "=%@", new NSArray(args)), null);
		if (rep.count() != 0) // key trouv�e
		{
			CktlRecord rec = (CktlRecord) rep.objectAtIndex(0);
			uri = rec.stringForKey(LocalSPConstantes.BD_SERVEUR_URI);
		}
		return uri;
	}

	/** **************************************************************************** */

	// trouve les noms des parametres + types passages d'1 methode
	public NSMutableArray fetchParamsForKeyMethod(Number key) {

		NSArray ed = fetchArray(LocalSPConstantes.BD_PARAM, newCondition(
				LocalSPConstantes.BD_PARAM_METH + "=%@", new NSArray(key)), null);
		int edcount = ed.count();

		NSArray ed2 = fetchArray(LocalSPConstantes.BD_MET_SERV, newCondition(
				LocalSPConstantes.BD_SERVEUR_KEY + "=%@", new NSArray(key)), null);

		// traitement

		NSMutableArray arr = new NSMutableArray();
		CktlRecord par = (CktlRecord) ed2.objectAtIndex(0);
		arr.addObject(par.numberForKey(LocalSPConstantes.BD_SERVEUR_PASSAG));

		for (int i = 0; i < edcount; i++) // le nombre de param
		{
			int j = 0;
			par = (CktlRecord) ed.objectAtIndex(j);
			Number plac = par.numberForKey(LocalSPConstantes.BD_PARAM_PLACE);
			while (plac.intValue() != i + 1 && ++j < edcount) // pour trouver le bon
			{
				par = (CktlRecord) ed.objectAtIndex(j);
				plac = par.numberForKey(LocalSPConstantes.BD_PARAM_PLACE);
			}
			if (j == edcount) {
				CktlLog.trace("Probleme BD : col Place de table Parametre mal remplie");
				// System.out.println("Prob BD : col Place de table Parametre");
			} else {
				String nom = par.stringForKey(LocalSPConstantes.BD_PARAM_NOM);
				arr.addObject(nom);
			}
		}
		// renvoie typePassage, nomParam1, nomParam2, ...
		return arr;
	}

	/** **************************************************************************** */

	// methode qui recup une info string (url ou nom) pour 1 identifiant string
	// prend en param : id = identifiant , col_egal = nom Colonne id, table =
	// nomTable
	// et retour = nom Colonne Info Voulue
	public Object fetchInfoForId(Object id, String col_egal, String table,
			String retour) {
		String reponseS = null;
		Number reponseN;
		Object reponse = new Object();
	
		String cond = col_egal + "=%@";
		NSArray records = fetchArray(table, newCondition(cond, new NSArray(id)), null);
		
		if (records.count() != 0) {
			// ligne trouv�e
			CktlRecord record = (CktlRecord) records.objectAtIndex(0);
			try {
				reponseS = record.stringForKey(retour);
				StringTokenizer st = new StringTokenizer(reponseS, "\"");

				reponseS = st.nextToken();
				if (st.hasMoreTokens()) {
					reponseS = st.nextToken();
				} else // pour num idappli, format (39)
				{
					StringTokenizer st2 = new StringTokenizer(reponseS, "(");
					reponseS = st2.nextToken();
					StringTokenizer st3 = new StringTokenizer(reponseS, ")");
					reponseS = st3.nextToken();
				}
				reponse = (Object) reponseS;
			} catch (Exception e) {
				reponseN = record.numberForKey(retour);
				reponse = (Object) reponseN;
			}

		}
		return reponse;
	}

	/** **************************************************************************** */

	// methode qui recupere l'url d'une appli
	public String fetchAppliUrl(String id) {
		String reponse = null;

		String cond = LocalSPConstantes.BD_APPLI_ID + "=%@";
		NSArray ed = fetchArray(LocalSPConstantes.BD_APPLI, newCondition(cond,
				new NSArray(id)), null);

		if (ed.count() != 0) // ligne trouv�e
		{
			CktlRecord inf = (CktlRecord) ed.objectAtIndex(0);
			reponse = inf.stringForKey(LocalSPConstantes.BD_APPLI_URL);

			if (reponse.endsWith("JavaClient.jnlp"))
				reponse = reponse.substring(0, reponse.length() - 84);

		}
		return reponse;
	}

	// methode qui recup le code typeRetour ou priorit� pour 1 cl� identifiant
	// methode

	/*
	 * public Number fetchInfoForId(Number key, String colReturn) { Number reponse =
	 * null;
	 * 
	 * String cond = Constantes.BD_METHODE_KEY +"=%@"; NSArray ed =
	 * fetchArray(Constantes.BD_METHODE, newCondition(cond, new NSArray(key)),
	 * null);
	 * 
	 * if (ed.count() != 0) // ligne trouv�e { CktlRecord inf = (CktlRecord)
	 * ed.objectAtIndex(0); reponse = inf.numberForKey(colReturn); //
	 * Constantes.BD_METHODE_TRETOUR); } return reponse; }
	 */

	/** **************************************************************************** */

	// methode qui recup le Nom du type Variable pour 1 cl� var
	public String fetchLibTypeVarForKeyVar(Number key) {
		String reponse = null;

		String cond = LocalSPConstantes.BD_TYPEVAR_KEY + "=%@";
		NSArray ed = fetchArray(LocalSPConstantes.BD_TYPEVAR, newCondition(cond,
				new NSArray(key)), null);

		if (ed.count() != 0) // ligne trouv�e
		{
			CktlRecord inf = (CktlRecord) ed.objectAtIndex(0);
			reponse = inf.stringForKey(LocalSPConstantes.BD_TYPEVAR_LIB);
		}
		return reponse;
	}

	/** *********************************************************** */

	// methode pour savoir code d'un type temps
	// a tester
	public Number fetchCodeTempForTypeTemp(String typeT) {
		Number reponse = new Integer(0);  
		// 0 si type non trouv� dans BD -> indefini

		String cond = LocalSPConstantes.BD_TYPETEMPS_TYPE + "=%@";
		NSArray ed = fetchArray(LocalSPConstantes.BD_TYPETEMPS, newCondition(cond,
				new NSArray(typeT)), null);

		if (ed.count() != 0) {
			// ligne trouv�e
			CktlRecord inf = (CktlRecord) ed.objectAtIndex(0);
			reponse = inf.numberForKey(LocalSPConstantes.BD_TYPETEMPS_CODE);
		}
		return reponse;
	}

	

	/**
	 * Retourner le libelle associe a un type d'occupation
	 */
	public String fetchLibelleForTypeTemp(String value) {
		String libelle = null;
		NSArray records = fetchArray(LocalSPConstantes.BD_TYPETEMPS, 
				newCondition(LocalSPConstantes.BD_TYPETEMPS_TYPE + "='" + value+"'"), null);
		if (records.count() > 0) {
			CktlRecord record = (CktlRecord) records.objectAtIndex(0);
			libelle = record.stringForKey(LocalSPConstantes.BD_TYPETEMPS_DETAIL);
		}
		return libelle;
	}
	
	/** **************************************************** */

	// recup toutes les idappli des applis ayant une methode serveur
	public NSArray fetchAppliWithMethServeur() {
		NSMutableArray result = new NSMutableArray();
		NSArray recsMethServeur = fetchArray(LocalSPConstantes.BD_MET_SERV, newCondition("idappli<>'-1'"), null);
		for (int i = 0; i < recsMethServeur.count(); i++) {
			CktlRecord rec = (CktlRecord) recsMethServeur.objectAtIndex(i);
			result.addObject(rec);
		}
		// garder que les idappli distinctes
		result = valeursDistinctes(result);
		return result;
	}

   /**
     * Retourner les enregistrements <code>CktlRecord</code> de la table
     * de l'entite <b>SP_MethServeur</b> ayant qui sont utilises par
     * la methode passee en parametre (ex: liste des methodes serveur 
     * qu'utilise la methode cliente de DT "addTraitement()")
     * 
     * On ne retourne que les methodes qui retourne des donnees
     * de planning, i.e. des <code>SPOccupation</code>
     */
	public NSArray findMethServeurForClientAndReturningSPOccupation(Number idkey) {
		// methodes serveurs utilis� pour cet idkey
 		NSArray recsMethServeur = (NSArray) (fetchArray(LocalSPConstantes.BD_REPARTITION, newCondition(
 				LocalSPConstantes.BD_REPAR_CLIENTID + "=%@", new NSArray(idkey)), null)).valueForKey("ms");
		// methodes serveurs retournant des SPOccupation (31)
 		recsMethServeur = EOQualifier.filteredArrayWithQualifier(recsMethServeur, newCondition(
 				LocalSPConstantes.BD_SERVEUR_VAR + "=%@", new NSArray(new Integer(SPConstantes.TYPE_SP_OCCUPATION))));
 		// supp des doublons
 		recsMethServeur = NSArrayCtrl.removeDuplicate(recsMethServeur);	

 		return recsMethServeur;
	}

//    public NSArray findRepartitionForIdKey(Number idkey) 
//    {
//        // keyS utilis� pour cet idkey
//        NSMutableArray rep = new NSMutableArray();
//        Number keyS = null;
//        NSMutableArray args = new NSMutableArray();
//        args.addObject(idkey);
//        NSArray arr = fetchArray(Constantes.BD_REPARTITION, newCondition(
//                Constantes.BD_REPAR_CLIENTID + "=%@", new NSArray(args)), null);
//        for (int i = 0; i < arr.count(); i++) 
//            {
//                CktlRecord record = (CktlRecord) arr.objectAtIndex(i);
//                keyS = record.numberForKey(Constantes.BD_REPAR_SERVMET);
//                rep.addObject(keyS);
//                CktlLog.log(">> app serveur : " + record.valueForKeyPath("ms.sp_Appli.urlappli"));
//            }
//        rep = valeursDistinctes(rep);
//        
//        // KeyS retournant planning (31)
//        NSMutableArray rep2 = new NSMutableArray();
//        args = new NSMutableArray();
//        args.addObject(new Integer(31));
//        NSArray arr2 = fetchArray(Constantes.BD_MET_SERV, newCondition(
//                Constantes.BD_SERVEUR_VAR + "=%@", new NSArray(args)), null);
//            for (int i = 0; i < arr2.count(); i++) // key trouv�e
//            {
//                CktlRecord record = (CktlRecord) arr2.objectAtIndex(i);
//                keyS = record.numberForKey(Constantes.BD_SERVEUR_KEY);
//                rep2.addObject(keyS);
//            }
//            
//        // recup keyS en commun 
//        for (int i = 0; i < rep.count(); i++)   
//        {
//            Number num = (Number) rep.objectAtIndex(i);
//            if (! rep2.containsObject(num)) 
//            {
//                // methS idkey ok mais typeVar non ok
//                rep.removeIdenticalObject(num);
//                i++;
//            }
//        }
//            
//        return new NSArray(rep);
//    }

    
	
	/** ********************************************************* */
	// pour recuperer les chemins+nom des icalendar d'une personne
	public NSArray fetchICalendar(Number noIndividu) 
	{
		// keyS utilis� pour cet idkey
		NSMutableArray args = new NSMutableArray();
		args.addObject(noIndividu);
		NSArray arr = fetchArray(LocalSPConstantes.BD_ICAL, newCondition(
				LocalSPConstantes.BD_ICAL_NOINDIVIDU + "=%@", new NSArray(args)), null);
		
//		for (int i = 0; i < arr.count(); i++) 
//			{
//				CktlRecord record = (CktlRecord) arr.objectAtIndex(i);
//				String lien = record.stringForKey(Constantes.BD_ICAL_LIEN);
//				String nom = record.stringForKey(Constantes.BD_ICAL_NAME);
//				if (!lien.endsWith("/"))
//					lien += "/";
//				if (nom != null && !nom.equals(""))
//					rep.addObject(lien+nom);
//			}
			
		return arr/*new NSArray(rep)*/;
	}

	// ------- supprime les doublons dans un tableau --------

	private NSMutableArray valeursDistinctes(NSMutableArray arr) {

	 int init = arr.count();
	 for (int i = 0; i <arr.count(); i++) {
			Object test = arr.objectAtIndex(i);
			arr.removeObject(test);
			arr.addObject(test);
			if (arr.count() != init)
				{
				i = -1;  // on reprend au debut du tab
				init = arr.count();
				}
		}
	 
/*		NSMutableArray rep = new NSMutableArray();
		int init = arr.count();
		for (int i = init-1; i >=0; i--) 
			// par de la fin car ajoute a la fin, sinon pb
		{
			Object test = arr.objectAtIndex(i);
			arr.removeObject(test);
			arr.addObject(test);
			int diff = init - arr.count();
			
		}*/

		return arr;
	}

	/**
	 * Retourne la chaine associée à un numéro de parametre
	 * @param idKeyClient
	 * @return
	 */
	public static String getTypeParamLabelForIdKeyClient(int idKeyClient) {
  	String label = "inconnu";
  	if (idKeyClient == SPConstantes.IDKEY_INDIVIDU.intValue()) {
			label = SPConstantes.LABEL_INDIVIDU;
		} else if (idKeyClient == SPConstantes.IDKEY_OBJET.intValue()) {
			label = SPConstantes.LABEL_OBJET;
		} else if (idKeyClient == SPConstantes.IDKEY_SALLE.intValue()) {
			label = SPConstantes.LABEL_SALLE;
		} else if (idKeyClient == SPConstantes.IDKEY_DIPLOME.intValue()) {
			label = SPConstantes.LABEL_DIPLOME;
		} else if (idKeyClient == SPConstantes.IDKEY_GROUPE_ETUDIANT.intValue()) {
			label = SPConstantes.LABEL_GROUPE_ETUDIANT;
		} 
  	return label;
	}
}
