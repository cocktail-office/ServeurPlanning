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
package fr.univlr.cri.planning;

import org.cocktail.fwkcktlwebapp.common.CktlLog;
import org.cocktail.fwkcktlwebapp.common.util.StringCtrl;
import org.cocktail.fwkcktlwebapp.server.CktlDataResponse;
import org.cocktail.fwkcktlwebapp.server.CktlWebAction;
import org.cocktail.fwkcktlwebapp.server.CktlWebApplication;
import org.cocktail.fwkcktlwebapp.server.components.CktlAlertPage;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSComparator;
import com.webobjects.foundation.NSComparator.ComparisonException;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;

import fr.univlr.cri.planning.components.PageLogin;
import fr.univlr.cri.planning.components.PagePlanningPersonnel;
import fr.univlr.cri.planning.constant.LocalSPConstantes;
import fr.univlr.cri.planning.datacenter.ICalendarBus;
import fr.univlr.cri.planning.datacenter.SharedPlanningBus;

public class DirectAction extends CktlWebAction {

	public DirectAction(WORequest aRequest) {
		super(aRequest);
	}

	public WOActionResults defaultAction() {
		return pageWithName(PageLogin.class.getName());
	}

	// - DEBUT CAS

	private Session spSession() {
		Session session = (Session) existingSession();
		if (session == null) {
			session = (Session) session();
		}
		return (Session) session;
	}

	/**
	 * CAS : traitement authentification OK
	 */
	public WOActionResults loginCasSuccessPage(String netid) {
		spSession().setConnectedUser(netid);
		return hiddenRepartitionAction();
	}

	/**
	 * CAS : traitement authentification en echec
	 */
	public WOActionResults loginCasFailurePage(String errorMessage, String arg1) {
		StringBuffer msg = new StringBuffer();
		msg.append("Une erreur s'est produite lors de l'authentification de l'uilisateur&nbsp;:<br><br>");
		if (errorMessage != null)
			msg.append("&nbsp;:<br><br>").append(errorMessage);
		return getErrorPage(msg.toString());
	}

	/**
	 * CAS : page par defaut si CAS n'est pas parametre
	 */
	public WOActionResults loginNoCasPage() {
		return pageWithName(PageLogin.class.getName());
	}

	/**
	 * affiche une page avec un message d'erreur
	 */
	private WOComponent getErrorPage(String errorMessage) {
		CktlAlertPage page = (CktlAlertPage) cktlApp.pageWithName(
				CktlAlertPage.class.getName(), context());
		page.showMessage(null, "ERREUR", errorMessage,
							null, null, null, CktlAlertPage.ERROR, null);
		return page;
	}

	public WOActionResults loginCasSuccessPage(String arg0, NSDictionary arg1) {
		return loginCasSuccessPage(arg0);
	}

	public WOActionResults loginNoCasPage(NSDictionary arg0) {
		return loginNoCasPage();
	}

	// - FIN CAS

	/**
	 * Pointeur sur la session a utiliser
	 */
	private Session plngSession() {
		Session plngSession = (Session) existingSession();
		if (plngSession == null)
			plngSession = (Session) session();
		return plngSession;
	}

	// ** raccourcis vers les bus de donnees **

	private SharedPlanningBus sharedPlanningBus() {
		return plngSession().dataCenter().sharedPlanningBus();
	}

	private ICalendarBus iCalendarBus() {
		return plngSession().dataCenter().iCalendarBus();
	}

	// --------------------------------------------------------------------
	// --------------- DirectAction du Serveur de planning ----------------

	// --------------- DA pour lecture de planning ------------------------

	/**
	 * Point d'entree pour lire les occupations. L'application determine quels
	 * sont les methodes serveur a interroger d'apres la methode appelante
	 * (methode cliente)
	 */
	public WOActionResults servPlanAction() {
		logStart("ask read plannings.");
		long startTime = System.currentTimeMillis();
		WOResponse aResponse = new WOResponse();
		aResponse.setContent(sharedPlanningBus().getPropertiesOccupationFromRequest(context().request()).toString());
		logEnd(startTime);
		return aResponse;
	}

	// **************** DA pour l'ecriture de planning ****************

	public WOActionResults modifPlanAction() {
		logStart("detect plannings conflicts.");
		long startTime = System.currentTimeMillis();
		WOResponse aResponse = new WOResponse();
		aResponse.setContent(sharedPlanningBus().modifPlanAction(context().request()).toString());
		logEnd(startTime);
		return aResponse;
	}

	// ********** DA pour le partage de planning perso *************
	// recuperation d'infos contenu dans un iCalendar

	/**
	 * @deprecated Lire le/les fichier(s) iCalendar (.ics) existants sur le
	 *             WebDAV, et retourner le tout en liste de
	 *             <code>SPOccupation</code>.
	 */
	public WOActionResults iCalendarPourPeriodeAction() {
		logStart("parse icalendar file.");
		long startTime = System.currentTimeMillis();
		WOResponse aResponse = new WOResponse();
		aResponse.setContent(iCalendarBus().getBufferOccupationFromICalendar(
				context().request()).toString());
		logEnd(startTime);
		return aResponse;
	}

	// ********** DA pour le partage de planning perso *************
	// creation d'un nouvel iCalendar

	/**
	 * Demande de creation d'un fichier .ics en passant en parametre le chemin ou
	 * creer le fichier ainsi que la liste des occupations a y inserer.
	 */
	public WOActionResults createICalendarAction() {
		logStart("custom icalendar creation.");
		long startTime = System.currentTimeMillis();
		WORequest requete = context().request(); // recup requete
		WOResponse res = new WOResponse();
		res.setContent("statut = " + iCalendarBus().doCreateFileICalendarFromRequest(requete));
		logEnd(startTime);
		return res;
	}

	// TODO a deplacer dans les constantes
	public final static String MET_GET_ALL_OCC_NAME = "iCalendarOccupations";
	private final static String MET_GET_ALL_OCC_HORS_ICS_NAME = "iCalendarOccupationsHorsWebdav";
	private final static String MET_GET_ALL_PRE_NAME = "iCalendarPresence";

	/**
	 * occupations (SI + WebDAV). Utile pour examiner le calendrier d'un autre
	 * agent sans faire la distinction SI / WebDAV.
	 */
	public WOActionResults iCalendarOccupationsAction() {
		return iCalendarForClient(MET_GET_ALL_OCC_NAME);
	}

	/**
	 * occupations (uniquement SI). Utile pour gerer dans son appli personnel de
	 * ical son propre ics et voir a cot� ce qu'il y a dans le SI.
	 */
	public WOActionResults iCalendarOccupationsHorsWebdavAction() {
		return iCalendarForClient(MET_GET_ALL_OCC_HORS_ICS_NAME);
	}

	/**
	 * presence : horaires + hsup
	 */
	public WOActionResults iCalendarPresenceAction() {
		return iCalendarForClient(MET_GET_ALL_PRE_NAME);
	}

	/**
	 * Recuperer un flux iCalendar (.ics) des evenements declares dans le SI et/ou
	 * dans le WebDAV
	 */
	private WOActionResults iCalendarForClient(String metclient) {
		logStart("create icalendar stream - \"" + metclient + "\"");
		long startTime = System.currentTimeMillis();
		CktlDataResponse response = new CktlDataResponse();
		NSData datas = iCalendarBus().getICalendarStream(
				context().request(), metclient);
		response.setContent(datas);
		response.setHeader("text/calendar", "Content-Type");
		response.setHeader("UTF-8", "Content-Encoding");
		response.setHeader(String.valueOf(datas.length()), "Content-Length");
		String uid = iCalendarBus().getUidForRequestDictionary(iCalendarBus().getRequestKeys(context().request()));
		response.setFileName(uid + LocalSPConstantes.ICS_FILE_NAME_EXTENSION);
		logEnd(startTime);
		return response.generateResponse();
	}

	// methodes de log

	private static Application spApp = (Application) CktlWebApplication.application();

	/**
	 * Affichage du debut d'une directAction - description de la da appelee
	 */
	private void logStart(String description) {
		CktlLog.setLevel(spApp.config().intForKey(LocalSPConstantes.KEY_CKTLLOG)); // permet
																																								// affichage
																																								// des
																																								// CktlLog.trace
		CktlLog.log(logPrefix() + " : " + description);
		// les informations sur le demandeur
		StringBuffer sb = new StringBuffer("");
		sb.append("client ip=").append(spApp.getRequestIPAddress(request()));
		CktlLog.log(logPrefix() + " : " + sb.toString());
	}

	/**
	 * Terminaison de la directAction - affichage d'un separateur
	 */
	private void logEnd(long startTime) {
		long l1 = System.currentTimeMillis() - startTime;
		long l2 = l1 / 1000;
		StringBuffer toLog = new StringBuffer(logPrefix());
		toLog.append(" - total time : ");
		if (l2 == 0)
			toLog.append(l1 + " ms.");
		else
			toLog.append(l2 + " s.");
		toLog.append("\n").append(logSpace());
		CktlLog.log(toLog.toString());
	}

	/**
	 * La chaine precedent chaque log
	 */
	private String logPrefix() {
		return "DirectAction";
	}

	private String logSpace() {
		return "                            ----------------------------";
	}

	// les noms des variables contenant les categories
	private final static String REQUEST_PARAM_CAT = "cat";
	// les noms des variables contenant les mots clefs
	private final static String REQUEST_PARAM_MOT = "mot";

	public WOActionResults repartitionAction() {
		return casLoginAction();
	}

	/**
	 * Acces sur l'ecran qui permet de determiner la repartition du temps
	 * 
	 * @return
	 */
	private WOActionResults hiddenRepartitionAction() {
		// une premiere extraction des parametres
		WORequest request = context().request();
		NSArray varList = (NSArray) request.formValueKeys();
		// classement alphabetique des variables
		try {
			varList = varList.sortedArrayUsingComparator(NSComparator.AscendingCaseInsensitiveStringComparator);
		} catch (ComparisonException e) {
			e.printStackTrace();
		}
		;
		// liste des categories
		NSArray catList = new NSArray();
		// dico de parametre
		NSMutableDictionary dicoParam = new NSMutableDictionary();
		for (int i = 0; i < varList.count(); i++) {
			String var = (String) varList.objectAtIndex(i);
			// extraction des categories
			if (!StringCtrl.isEmpty(var) && var.startsWith(REQUEST_PARAM_CAT)) {
				// on ne conserve que les variables nommant une categorie
				if (!StringCtrl.containsIgnoreCase(var, REQUEST_PARAM_MOT)) {
					String cat = (String) request.formValueForKey(var);
					catList = catList.arrayByAddingObject(cat);
					if (!StringCtrl.isEmpty(cat)) {
						// retrouver les mots clefs associes
						NSArray motList = new NSArray();
						String motPrefix = var + REQUEST_PARAM_MOT;
						for (int j = 0; j < varList.count(); j++) {
							String subVar = (String) varList.objectAtIndex(j);
							if (subVar.startsWith(motPrefix)) {
								String mot = (String) request.formValueForKey(subVar);
								if (!StringCtrl.isEmpty(mot)) {
									motList = motList.arrayByAddingObject(mot);
								}
							}
						}
						// inserer les mots pour cette categorie dans le dico
						if (motList.count() > 0) {
							dicoParam.setObjectForKey(motList, cat);
						}
					}
				}
			}
		}
		// mettre le dico dans la session s'il n'est pas vide
		if (catList.count() > 0) {
			// sauvegarde du classement
			dicoParam.setObjectForKey(catList, LocalSPConstantes.KEY_DICO_REPART_CAT_SORT_KEY);
			spSession().setDicoParam(dicoParam);
		}
		// augmenter la durée de vie de la session (1 min par défaut)
		spSession().setTimeOut(3600);
		return pageWithName(PagePlanningPersonnel.class.getName());
	}

	// RAZ du cache

	public WOActionResults clearCacheAction() {
		iCalendarBus().clearCache();
		return CktlAlertPage.newAlertPageWithCaller(
				pageWithName(PageLogin.class.getName()),
				"Nettoyage du cache", "Les fichiers de cache du serveur ont &eacute;t&eacute; supprim&eacute;s",
				"Page d'accueil", CktlAlertPage.INFO);
	}
}
