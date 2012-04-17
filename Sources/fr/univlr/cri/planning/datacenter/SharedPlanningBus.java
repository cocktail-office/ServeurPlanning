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

import java.math.BigDecimal;
import java.util.Properties;

import org.cocktail.fwkcktlwebapp.common.CktlLog;
import org.cocktail.fwkcktlwebapp.common.database.CktlRecord;
import org.cocktail.fwkcktlwebapp.common.util.DateCtrl;
import org.cocktail.fwkcktlwebapp.common.util.NSArrayCtrl;
import org.cocktail.fwkcktlwebapp.common.util.StringCtrl;

import com.webobjects.appserver.WORequest;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestamp;

import fr.univlr.cri.planning.DemandePlanning;
import fr.univlr.cri.planning.SPConstantes;
import fr.univlr.cri.planning.SPOccupation;
import fr.univlr.cri.planning.constant.LocalSPConstantes;
import fr.univlr.cri.planning.factory.LecturePlanning;
import fr.univlr.cri.planning.factory.ModifPlanning;
import fr.univlr.cri.planning.thread.RemoteApplicationAccessor;

/**
 * Bus rassemblant tout les traitements "haut-niveau" du serveur de planning
 * 
 * @author cmenetre, ctarade
 */

public class SharedPlanningBus extends A_SPDataBusCaching {

	public SharedPlanningBus(EOEditingContext arg0) {
		super(arg0);
	}

	/**
	 * Lire les occupations du SI, en prenant en parametre un objet du type
	 * <code>WORequest</code>
	 */
	public Properties getPropertiesOccupationFromRequest(WORequest request) {
		LecturePlanning sp = new LecturePlanning(paramBus(), request); // pour acces
																																		// methodes
		return getPropertiesOccupationFromParams(sp);
	}

	/**
	 * Lire les occupations du SI, en prenant en parametre un objet du type
	 * <code>NSDictionary</code>
	 */
	public Properties getPropertiesOccupationFromDictionary(NSDictionary dico) {
		LecturePlanning sp = new LecturePlanning(paramBus(), dico.hashtable()); // pour
																																						// acces
																																						// methodes
		return getPropertiesOccupationFromParams(sp);
	}

	/**
	 * Point d'entree pour lire les occupations. L'application determine quels
	 * sont les methodes serveur a interroger d'apres la methode appelante
	 * (methode cliente)
	 */
	public Properties getPropertiesOccupationFromParams(LecturePlanning lectPlan) {

		// ---------------------- INITIALISATION ------------------------ //
		Properties resultProperties = new Properties();
		String stat = String.valueOf(1);
		String err = null;

		// ----------------- RECUPERATION PARAMETRES ------------------ //
		// param contient : nomMethClient puis (idkey, idval, debut, fin) r�p�t�
		String clientMethodName = lectPlan.getParams().objectAtIndex(0).toString();
		CktlLog.trace("client method : [" + clientMethodName + "]");
		if (clientMethodName.equals("isNull")) {
			// nom methode cliente absent
			stat = String.valueOf(0);
			err = "Erreur, nom de methode Cliente absent.";
		} else {
			// nom methode cliente OK
			if (lectPlan.getParams().count() <= 1) {
				// 1 parametre absent
				stat = String.valueOf(0);
				err = "Erreur, un parametre absent ou invalide.";
			} else {
				// parametres pr�sents
				if (lectPlan.getNbPar() != (lectPlan.getParams().count() - 1) / 4) {
					// certains groupes de parametres ont �t� ignor�s car erron�s.
					stat = String.valueOf(0);
					err = "Erreur, un parametre absent ou invalide. Resultat recu concerne les parametres avant l'erreur.";
				}
				// il reste des params exploitables, on continue l'algo.
				Number clientMethodKey = paramBus().fetchKeyForNom(clientMethodName);

				if (clientMethodKey != null) {

					// controle particulier pour la methode isDisponible
					// on va indiquer si oui ou non des methodes serveur y sont liees
					// pour que le serveur n'indique pas hors periode horaire
					// pour ceux qui n'ont pas HAmAC
					boolean isClientMethodIsDisponible = clientMethodName.equals(SPConstantes.DIRECT_ACTION_IS_DISPONIBLE);

					Number keyServeur = null;
					int nb = lectPlan.getParams().count() - 1;

					// --------- POUR CHAQUE GROUPE DE PARAMS ----------------------- //
					for (int i = 1; i < nb; i += 4) {
						String idk = lectPlan.getParams().objectAtIndex(i).toString(); // ne
																																						// pas
																																						// supprimer
						String idv = lectPlan.getParams().objectAtIndex(i + 1).toString();
						String debut = lectPlan.getParams().objectAtIndex(i + 2).toString();
						String fin = lectPlan.getParams().objectAtIndex(i + 3).toString();

						// creer la cle
						String key = buildKey(clientMethodName, idk, idv,
								DateCtrl.stringToDate(debut, SPConstantes.DATE_FORMAT),
								DateCtrl.stringToDate(fin, SPConstantes.DATE_FORMAT));

						// tenter une recuperation depuis le cache
						Properties threadBuffer = (Properties) getObjectFromCache(key);

						// pas trouve, on recree le flux
						if (threadBuffer == null) {
							threadBuffer = new Properties();

							// recup uri des methodes serv pour nomClient et idKey
							NSArray<Number> serverMethodKeyList = paramBus().fetchServMethodsForClient(clientMethodKey, idk);

							// lancer les appels uniquement si on a trouve des methodes
							// serveur associees
							if (serverMethodKeyList.count() > 0) {

								// affichage du nom des methodes serveur associees
								StringBuffer bufLogServerMethod = new StringBuffer();
								for (int j = 0; j < serverMethodKeyList.count(); j++) {
									Number serverMethodKey = serverMethodKeyList.objectAtIndex(j);
									String serverMethodName = paramBus().fetchUriForKey(serverMethodKey);
									bufLogServerMethod.append("(" + serverMethodKey + ")" + serverMethodName);
									if (j < serverMethodKeyList.count() - 1) {
										bufLogServerMethod.append(", ");
									}
								}
								CktlLog.log("connected servers URIs : [" + bufLogServerMethod.toString() + "]");

								// j'envois les 4 params, selection ensuite selon DA appell�e
								Object[] arguments = { idv, debut, fin, idk };
								CktlLog.log("args : [" + idv + "," + debut + "," + fin + "," + idk + "]");

								// ----------- POUR Chaque Methode Serveur Trouv�e ---------- //

								// on crée un thread par methode serveur
								NSArray<RemoteApplicationAccessor> threads = new NSArray<RemoteApplicationAccessor>();
								for (int j = 0; j < serverMethodKeyList.count(); j++) {
									keyServeur = (Number) serverMethodKeyList.objectAtIndex(j);
									RemoteApplicationAccessor raa = new RemoteApplicationAccessor(
											keyServeur,
											paramBus(),
											clientMethodName,
											idv,
											idk,
											arguments,
											lectPlan,
											resultProperties,
											DateCtrl.stringToDate(debut, SPConstantes.DATE_FORMAT),
											DateCtrl.stringToDate(fin, SPConstantes.DATE_FORMAT),
											clientMethodKey);
									raa.start();
									// ajout a la liste des thread lances
									threads = threads.arrayByAddingObject(raa);
								}

								// on fait une attente bloquante sur la fin de tous les thread
								for (int j = 0; j < serverMethodKeyList.count(); j++) {
									RemoteApplicationAccessor accessor = ((RemoteApplicationAccessor) threads.objectAtIndex(j));
									accessor.join();
								}

								// on construit l'objet buffer final d'apres les resultats
								// de chaque thread
								for (int j = 0; j < threads.count(); j++) {
									Properties localThreadBuffer = ((RemoteApplicationAccessor) threads.objectAtIndex(j)).getBuffer();
									threadBuffer.putAll(localThreadBuffer);
								}

							} else {
								// pas de methode serveur associee ... si c'est la methode
								// isDisponible, alors on l'indique explicitement
								if (isClientMethodIsDisponible) {
									threadBuffer.put(SPConstantes.PROP_IS_DISPONIBLE_CONNECTED, "false");
								}
							}
							putObjectInCache(key, threadBuffer);
						}
						resultProperties.putAll(threadBuffer);

						//
						// -------------- FIN : POUR Chaque Methode Serveur Trouvée
						// -------------
						//

					}
					//
					// --------- FIN : POUR CHAQUE GROUPE DE PARAMS
					// -----------------------
					//

					// recup a type return
					Number typeRetour = (Number) paramBus().fetchInfoForId(clientMethodKey, LocalSPConstantes.BD_CLIENT_KEY,
									LocalSPConstantes.BD_MET_CLIENT, LocalSPConstantes.BD_CLIENT_VAR);

					// recalcul du nombre total d'occupations
					if (typeRetour.intValue() == SPConstantes.TYPE_SP_OCCUPATION) {
						// probleme, le total n'est pas bon s'il est issus du cache (il est
						// a 0)
						// on compte le nombre de fois ou apparait l'occurence "idkey"
						// TODO c'est pas tres tres propre, mais ça suffira pour l'instant
						// :p (ca rame pas)
						java.util.Enumeration enumKeys = resultProperties.propertyNames();
						NSArray checkedKeys = new NSArray();
						int nbOcc = 0;
						while (enumKeys.hasMoreElements()) {
							String currKey = (String) enumKeys.nextElement();
							if (!checkedKeys.containsObject(currKey) && currKey.startsWith(SPConstantes.PROP_OC_IDKEY)) {
								checkedKeys = checkedKeys.arrayByAddingObject(checkedKeys);
								nbOcc++;
							}
						}

						resultProperties.setProperty(SPConstantes.PROP_OC_NB, Integer.toString(nbOcc));
					}

					// fin if nomCliente trouvé
				} else {
					stat = "0";
					err = "Nom de methode Cliente non trouve dans BD (Remplir table sp_Methodes, sp_met_Client).";
				}
			} // fin if nomClient ok
		} // fin if param ok

		// ------------ FINALISATION

		resultProperties.setProperty(SPConstantes.PROP_STATUT, stat);
		if (err != null)
			resultProperties.setProperty(SPConstantes.PROP_ERREUR, err);

		return resultProperties;
	} // ------- fin methode servPlanAction() ----

	// **************** DA pour l'ecriture de planning ****************

	/**
	 * @deprecated est-ce que ça sert toujours .... ?
	 */
	public Properties modifPlanAction(WORequest requete) {

		/* ---------------------- INITIALISATION ------------------------ */
		ModifPlanning mp = new ModifPlanning(paramBus(), requete);
		String stat = String.valueOf(1);
		String err = null;

		/* ----------------- RECUPERATION PARAMETRES ------------------ */

		NSArray param = mp.getParams();

		// ajout CT : on supprime de la liste les DA pointant sur l'appli appelante
		// pour eviter les inter blocages
		// le nom peut contenu le numero de l'instance, il faut le supprimer pour
		// la comparaison
		// ex : http://www.univ-lr.fr/cgi-bin/WebObjects/DT3.woa/1
		String urlReferer = (String) param.objectAtIndex(1);
		while (!urlReferer.endsWith(".woa")) {
			urlReferer = urlReferer.substring(0, urlReferer.length() - 1);
		}

		Integer idkey = new Integer(param.objectAtIndex(2).toString());
		Integer idval = new Integer(param.objectAtIndex(3).toString());

		StringBuffer bufStr = new StringBuffer("Checking interblocking with [" + urlReferer + "]...");

		// acces bd DA a interroger (methServ renvoyant spocc)-> recup les keyS
		NSArray recsMethServeur = paramBus().findMethServeurForClientAndReturningSPOccupation(idkey);
		NSArray recsMethServeurClean = new NSArray();
		for (int i = 0; i < recsMethServeur.count(); i++) {
			CktlRecord recMethServeur = (CktlRecord) recsMethServeur.objectAtIndex(i);
			String urlToCall = (String) recMethServeur.valueForKeyPath("sp_Appli.urlappli");
			bufStr.append("\n  should connect to [" + urlToCall + "] ? : ");
			if (!StringCtrl.containsIgnoreCase(urlToCall, urlReferer)) {
				recsMethServeurClean = recsMethServeurClean.arrayByAddingObject(recMethServeur);
				bufStr.append("OK");
			} else {
				bufStr.append("IGNORE");
			}
		}
		CktlLog.trace(bufStr.toString());
		recsMethServeur = recsMethServeurClean;

		/* -------------- Recherche de conflits ------------------- */

		NSMutableArray occupsNew = new NSMutableArray();
		for (int i = 4; i < param.count() - 2; i += 3) {
			// debut - fin - motif
			SPOccupation occ = new SPOccupation();
			occ.setDateDebutFromString(param.objectAtIndex(i).toString());
			occ.setDateFinFromString(param.objectAtIndex(i + 1).toString());
			occ.setTypeTemps(param.objectAtIndex(i + 2).toString());
			if (occ.getDateDebut() != null && occ.getDateFin() != null)
				// dates au bon format
				occupsNew.addObject(occ);
		}
		// codage nouvelle occ (methode faite) (trouver premiere et derniere,
		// faire code sur toute la duree en placant les occ)

		NSTimestamp debut = SPOccupation.findFirstObject(occupsNew).getDateDebut();
		// String dd = DateCtrl.dateToString(debut, SPConstantes.DATE_FORMAT);
		NSTimestamp fin = SPOccupation.findLastObject(occupsNew).getDateFin();

		NSArray codageNew = LecturePlanning.codageOccupation(paramBus(), occupsNew, debut, fin);
		NSMutableArray occupsOld = new NSMutableArray();
		Properties prop = new Properties();
		for (int j = 0; j < recsMethServeur.count(); j++) {
			// _ appel DA sur id et periode consern� (reutilise requeteMethodeDA ?)
			// _ occups stock�s
			Number keyS = ((CktlRecord) recsMethServeur.objectAtIndex(j)).numberForKey("key");
			if (keyS != null && keyS.intValue() != LocalSPConstantes.KEY_METHODE_SERVEUR_HORAIRE_POUR_PERIODE) {
				// if
				// (!keyS.toString().equals(LocalSPConstantes.KEY_METHODE_SERVEUR_HORAIRE_POUR_PERIODE.toString()))
				// {
				// on passe la DA horairesPourPeriode,/ n'intervient pas dans conflits
				Object[] args = new Object[4];
				args[0] = idval;
				args[1] = DateCtrl.dateToString(debut, SPConstantes.DATE_FORMAT); // ->
				// string
				args[2] = DateCtrl.dateToString(fin, SPConstantes.DATE_FORMAT);
				args[3] = idkey;
				prop = LecturePlanning.requeteMethodeServeur(paramBus(), keyS, args, "", 0);

				String statut = prop.getProperty(SPConstantes.PROP_STATUT);

				if (statut != null && statut.equals("1")) {
					occupsOld = LecturePlanning.appendArraySpOccupationFromProperties(
							occupsOld, prop, idval.toString(), idkey.toString());
				} else {
					stat = "0";
					if (prop.getProperty(SPConstantes.PROP_ERREUR) != null)
						err = prop.getProperty(SPConstantes.PROP_ERREUR);
				}
			}
		}

		// codage old occups
		NSArray codageOld = LecturePlanning.codageOccupation(paramBus(), new NSArray(occupsOld), debut, fin);

		// -> trouver conflit -> decoder les spocc en conflit, les mettre dans un
		// NSArray d'NSArrays d'SPOcc.
		// (chaque NSArray contenu dans le tableau global repr�sente un conflit et
		// contient les spocc le consernant).

		NSMutableArray zone = mp.zoneConflit(codageNew, codageOld);
		NSArray conflits = new NSArray();
		if (zone.count() != 0)
			conflits = mp.decodeConflit(zone, occupsNew, occupsOld, debut, fin);

		/* ------------ Finalisation reponse ------------------- */

		boolean confli = false;
		String message = null;
		if (conflits.count() != 0) // un ou plusieurs conflits trouv�s
		{
			confli = true; // booleen � true
			message = mp.messageAlert(conflits); // creation message d'alerte
		}

		// creation WOResponse content infos format Properties
		// 1 booleen (true si conflit) + 1 string (message alerte) + statut + erreur

		Properties buffer = new Properties();
		buffer.setProperty(SPConstantes.PROP_STATUT, stat);
		if (err != null)
			buffer.setProperty(SPConstantes.PROP_ERREUR, err);
		buffer.setProperty(SPConstantes.PROP_BOOL, new Boolean(confli).toString());
		if (!StringCtrl.isEmpty(message))
			buffer.setProperty(SPConstantes.PROP_MESSAGE, message);
		// au cas ou interconnection de serveurPlannings
		buffer.setProperty("nommethclient", "modifPlanAction");
		return buffer;
	}

	/**
	 * Point d'entree pour le calcul de repartition du temps occupe
	 * 
	 * @return
	 */
	public NSDictionary calculRepartition(int noIndividu, NSTimestamp dateDebut, NSTimestamp dateFin, NSDictionary dicoParam) {

		// construction d'un dico parametre pour interroger les applications
		NSMutableDictionary dico = new NSMutableDictionary();
		dico.setObjectForKey(Integer.toString(noIndividu), SPConstantes.PROP_OC_IDVAL);
		dico.setObjectForKey(SPConstantes.IDKEY_INDIVIDU.toString(), SPConstantes.PROP_OC_IDKEY);
		dico.setObjectForKey(DateCtrl.dateToString(dateDebut, SPConstantes.DATE_FORMAT), SPConstantes.PROP_OC_DEB);
		dico.setObjectForKey(DateCtrl.dateToString(dateFin, SPConstantes.DATE_FORMAT), SPConstantes.PROP_OC_FIN);
		dico.setObjectForKey(fr.univlr.cri.planning.DirectAction.MET_GET_ALL_OCC_NAME, SPConstantes.PAR1_METHCLIENT);
		// recuperation du resultat des occupations
		Properties propOcc = getPropertiesOccupationFromDictionary(dico);
		// transformation en tableau
		NSArray occList = NSArrayCtrl.flattenArray(DemandePlanning.getPlanning(propOcc));

		// recuperer le classement des categories
		NSArray catList = (NSArray) dicoParam.objectForKey(LocalSPConstantes.KEY_DICO_REPART_CAT_SORT_KEY);

		// initialiser le tableau avec des durees nulles
		NSMutableDictionary dicoResult = new NSMutableDictionary();
		// NSArray catList = dicoParam.allKeys();
		for (int i = 0; i < catList.count(); i++) {
			String cat = (String) catList.objectAtIndex(i);
			BigDecimal minutesType = new BigDecimal(0);
			dicoResult.setObjectForKey(minutesType, cat);
		}

		int totalMinutes = 0;

		for (int i = 0; i < occList.count(); i++) {
			SPOccupation occ = (SPOccupation) occList.objectAtIndex(i);

			// calcul de la duree de l'occupation en minutes
			int minutes = (int) ((occ.getDateFin().getTime() - occ.getDateDebut().getTime()) / (1000 * 60));

			// des qu'une occupation est classifiee, on arrete le traitement
			boolean isClassified = false;

			// sur l'ensemble des categories et tant que l'occupation n'est pas
			// classifiee
			for (int j = 0; j < catList.count() && !isClassified; j++) {
				String cat = (String) catList.objectAtIndex(j);

				// parmi tous les mots clefs et tant que l'occupation n'est pas
				// classifiee
				NSArray motList = (NSArray) dicoParam.objectForKey(cat);
				for (int k = 0; k < motList.count() && !isClassified; k++) {
					String mot = (String) motList.objectAtIndex(k);

					// si le mot clef est trouve dans le detail du temps ou le type du
					// temps
					if (StringCtrl.containsIgnoreCase(occ.getTypeTemps(), mot) ||
								(!StringCtrl.isEmpty(occ.getDetailsTemps()) &&
									StringCtrl.containsIgnoreCase(occ.getDetailsTemps(), mot))) {

						// incrementer le compte en minutes pour cette categorie
						BigDecimal minutesType = (BigDecimal) dicoResult.objectForKey(cat);
						minutesType = minutesType.add(new BigDecimal(minutes));
						dicoResult.setObjectForKey(minutesType, cat);
						totalMinutes += minutes;

						// une fois classifiee, on arrete la recherche pour cette occupation
						isClassified = true;
					}
				}
			}
			//
			if (!isClassified) {
				// CktlLog.log("inconnu : " + occ);
			}
		}
		// dicoResult.setObjectForKey(new Integer(totalMinutes), "totalMinutes");
		// dicoResult.setObjectForKey(TimeCtrl.stringForMinutes(totalMinutes),
		// "totalHeures");
		// calcul des pourcentages
		/*
		 * NSArray typeList = dicoType.allKeys(); for (int j=0; j<typeList.count() ;
		 * j++) { String type = (String) typeList.objectAtIndex(j); if (totalMinutes
		 * > 0) { dicoResult.setObjectForKey(new Float((float)(((BigDecimal)
		 * dicoResult.objectForKey(type)).intValue()*100) / (float)totalMinutes),
		 * type+"-%"); } }
		 */
		// recuperation du resultat des presences (horaires et heures supp)
		/*
		 * dico.setObjectForKey(MET_GET_ALL_PRE_NAME, SPConstantes.PAR1_METHCLIENT);
		 * Properties propPre =
		 * sharedPlanningBus().getPropertiesOccupationFromDictionary(dico); //
		 * transformation en tableau NSArray preList =
		 * NSArrayCtrl.flattenArray(DemandePlanning.getPlanning(propPre)); // int
		 * totalMinutePresence = 0; // for (int i=0; i<preList.count(); i++) {
		 * SPOccupation pre = (SPOccupation) preList.objectAtIndex(i); int minutes =
		 * (int) ((pre.getDateFin().getTime() -
		 * pre.getDateDebut().getTime())/(1000*60)); // tempo pour virer les 20
		 * minutes de pause GregorianCalendar gcFin = new GregorianCalendar();
		 * gcFin.setTime(pre.getDateFin()); int oldMinutes = minutes; if
		 * (gcFin.get(GregorianCalendar.HOUR) == 11 &&
		 * gcFin.get(GregorianCalendar.MINUTE) == 50 ) { // 12:50 minutes -= 20; }
		 * totalMinutePresence += minutes; CktlLog.log(" * " + pre.getDateDebut() +
		 * " - " + pre.getDateFin() + " (" + oldMinutes + " -> " +minutes + ")"); }
		 * CktlLog.log("totalMinutePresence="+totalMinutePresence + " > " +
		 * TimeCtrl.stringForMinutes(totalMinutePresence));
		 */

		return dicoResult;
	}

	// ** raccourcis vers les bus de donnees **

	private ParamBus paramBus() {
		return sPDataCenter().paramBus();
	}

	private Integer ttlPlanningRead;

	public int ttlObject() {
		if (ttlPlanningRead == null) {
			ttlPlanningRead = new Integer(app().ttlPlanningRead());
		}
		return ttlPlanningRead.intValue();
	}
	/*
	 * private static NSMutableDictionary theDicoPlanningCache;
	 * 
	 * public NSMutableDictionary dicoCache() { if (theDicoPlanningCache == null)
	 * { theDicoPlanningCache = new NSMutableDictionary(); } return
	 * theDicoPlanningCache; }
	 */
}
