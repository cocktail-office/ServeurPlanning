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
package fr.univlr.cri.planning.factory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

import org.cocktail.fwkcktlwebapp.common.CktlLog;
import org.cocktail.fwkcktlwebapp.common.database.CktlRecord;
import org.cocktail.fwkcktlwebapp.common.util.DateCtrl;
import org.cocktail.fwkcktlwebapp.common.util.StringCtrl;
import org.cocktail.fwkcktlwebapp.server.CktlWebApplication;

import com.webobjects.appserver.WOHTTPConnection;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSTimestamp;

import fr.univlr.cri.planning.SPConstantes;
import fr.univlr.cri.planning.SPMethodes;
import fr.univlr.cri.planning.SPOccupation;
import fr.univlr.cri.planning.constant.LocalSPConstantes;
import fr.univlr.cri.planning.datacenter.ParamBus;
import fr.univlr.cri.planning.thread.SPHTTPConnection;
import fr.univlr.cri.planning.timer.TimerCheckRemoteApplications;

/** **************************************************************************** */
// ---------- METHODES POUR ServeurPlanning utilisant DIRECT ACTION
/** **************************************************************************** */

public class LecturePlanning {

	private static int nbOcc;
	private int nbNum = 0;
	private int nbBool = 0;
	private int nbPar = 0;
	private ParamBus paramBus;
	private NSArray params;

	/**
	 * Constructeur minimaliste pour l'utilisation excluse de la methode
	 * tabApplisNonAccessibles()
	 * 
	 * @param aParamBus
	 */
	public LecturePlanning(ParamBus aParamBus) {
		super();
		paramBus = aParamBus;
		nbOcc = 0;
	}

	/**
	 * Constructeur a partir d'un <code>Properties</code>
	 */
	public LecturePlanning(ParamBus aParamBus, Hashtable hashtable) {
		super();
		paramBus = aParamBus;
		nbOcc = 0;
		extractParams(hashtable);
		traceParams();
	}

	/**
	 * Constructeur de l'objet avec une <code>WORequest</code>
	 */
	public LecturePlanning(ParamBus aParamBus, WORequest request) {
		super();
		paramBus = aParamBus;
		nbOcc = 0;
		// recup parametres
		String all = request.contentString();
		extractParams(SPMethodes.stringToProperties(all));
		traceParams();
	}

	/**
	 * Extraire les parametres d'une hashtable
	 */
	private void extractParams(Hashtable hashtable) {

		NSMutableArray arr = new NSMutableArray();

		// recup nom Methode Cliente
		String nomMClient = (String) hashtable.get(SPConstantes.PAR1_METHCLIENT);
		if (nomMClient == null) {
			// manque param
			log("no client method specified");
			arr.addObject("isNull");
		} else {
			arr.addObject(nomMClient);
		}

		int i = 0;
		if (hashtable.get(SPConstantes.PROP_OC_IDKEY + i) != null) {
			// requete multiple

			String idk = (String) hashtable.get(SPConstantes.PROP_OC_IDKEY + i);
			try {
				while (idk != null) {
					String idv = (String) hashtable.get(SPConstantes.PROP_OC_IDVAL + i);
					String debut = (String) hashtable.get(SPConstantes.PROP_OC_DEB + i);
					String fin = (String) hashtable.get(SPConstantes.PROP_OC_FIN + i);
					if (DateCtrl.stringToDate(debut, SPConstantes.DATE_FORMAT) != null &&
							DateCtrl.stringToDate(fin, SPConstantes.DATE_FORMAT) != null) {
						arr.addObject(idk);
						arr.addObject(idv);
						arr.addObject(debut);
						arr.addObject(fin);
						nbPar++;
					}
					i++;
					idk = (String) hashtable.get(SPConstantes.PROP_OC_IDKEY + i);
				}
			} catch (Exception e) {
				log("exception while extracting parameters : " + e.getMessage());
				e.printStackTrace();
			}
		} else {
			try {
				// requete simple sans numero
				String idk = (String) hashtable.get(SPConstantes.PROP_OC_IDKEY);
				String idv = (String) hashtable.get(SPConstantes.PROP_OC_IDVAL);
				String debut = (String) hashtable.get(SPConstantes.PROP_OC_DEB);
				String fin = (String) hashtable.get(SPConstantes.PROP_OC_FIN);

				if (DateCtrl.stringToDate(debut, SPConstantes.DATE_FORMAT) != null &&
						DateCtrl.stringToDate(fin, SPConstantes.DATE_FORMAT) != null) {
					arr.addObject(idk);
					arr.addObject(idv);
					arr.addObject(debut);
					arr.addObject(fin);
					nbPar++;
				}
			} catch (Exception e) {
				log("exception while extracting parameters : " + e.getMessage());
				e.printStackTrace();
			}
		}
		params = arr.immutableClone();

	}

	/**
	 * Methode d'affichage des parametres de la classe.
	 */
	private void traceParams() {
		// on plante pas sur un log
		try {
			StringBuffer logBuf = new StringBuffer("LecturePlanning: parameters ");
			logBuf.append("(").append(4).append("x").append(nbPar).append(") : ");
			logBuf.append(SPConstantes.PAR1_METHCLIENT).append("=[").append(params.objectAtIndex(0)).append("]");
			for (int i = 0; i < nbPar; i++) {
				if (i == 0)
					logBuf.append(" - ");
				logBuf.append("[");
				logBuf.append(SPConstantes.PROP_OC_IDKEY).append("=").append(params.objectAtIndex(i + 1)).append(", ");
				logBuf.append(SPConstantes.PROP_OC_IDVAL).append("=").append(params.objectAtIndex(i + 2)).append(", ");
				logBuf.append(SPConstantes.PROP_OC_DEB).append("=").append(params.objectAtIndex(i + 3)).append(", ");
				logBuf.append(SPConstantes.PROP_OC_FIN).append("=").append(params.objectAtIndex(i + 4)).append("]");
				if (i < nbPar - 1)
					logBuf.append(", ");
			}
			log(logBuf.toString());
		} catch (Exception e) {
		}
	}

	/**
	 * Le nom de la directAction contactee sur le serveur de planning qui a pour
	 * role de lire les fichiers ics et d'en renvoyer le contenu sous forme de
	 * <code>SPOccupation</code>
	 */
	private final static String DIRECT_ACTION_READ_ICALENDAR = "/wa/iCalendarPourPeriode?";

	// ------------------------------------------------------------------------
	// Methode qui envoie la requ�te � une methode Serveur par DirectAction
	public static Properties requeteMethodeServeur(
			ParamBus aParamBus, Number keyS, Object[] args, String nomClient, int hashCode) {

		Properties prop = new Properties();

		// recup uri associ�e a keyS
		String uri = aParamBus.fetchUriForKey(keyS);

		if (uri.equals(DIRECT_ACTION_READ_ICALENDAR)) {

			// appel diff car interne au ServeurPlanning
			HugICalendarFactory icsFactory = new HugICalendarFactory(aParamBus);
			Integer noIndividu = new Integer(args[0].toString());
			String d = args[1].toString();
			if (StringCtrl.containsIgnoreCase(d, "%20")) {
				d = StringCtrl.replace(d, "%20", " ");
			}
			String f = args[2].toString();
			if (StringCtrl.containsIgnoreCase(f, "%20")) {
				f = StringCtrl.replace(f, "%20", " ");
			}

			NSTimestamp debut = DateCtrl.stringToDate(d, SPConstantes.DATE_FORMAT);
			NSTimestamp fin = DateCtrl.stringToDate(f, SPConstantes.DATE_FORMAT);
			StringBuffer buffer = icsFactory.parseICalendarFileForPeriodToStringBuffer(
					noIndividu, debut, fin);

			prop = SPMethodes.stringToProperties(
					buffer.toString());

		} else {

			// recup nomAppli puis url de cette Appli
			String idA = (String) aParamBus.fetchInfoForId(
					keyS,
					LocalSPConstantes.BD_SERVEUR_KEY,
					LocalSPConstantes.BD_MET_SERV,
					LocalSPConstantes.BD_SERVEUR_APPLI);

			// VERIF SI APPLI EST DS TAB NON ACCESSIBLE
			NSArray nonOk = TimerCheckRemoteApplications.theUnavailableRemoteApplications();

			if (!nonOk.containsObject(idA)) {

				String urlA = aParamBus.fetchAppliUrl(idA);
				if (urlA.endsWith("JavaClient.jnlp"))
					urlA = urlA.substring(0, urlA.length() - 84);

				// recup parametres attendus
				NSMutableArray param = aParamBus.fetchParamsForKeyMethod(keyS);
				// id, debut, fin, [idkey]

				int pass = Integer.parseInt(param.objectAtIndex(0).toString());
				param.removeObjectAtIndex(0);
				if (pass == 2) {
					// content
					prop = requetePassageContent(nomClient, uri, urlA, args, new NSArray(param));
				} else {
					// url
					prop = requetePassageUrl(uri, urlA, args, new NSArray(param), hashCode);
				}
			} else {
				// appli non démarrée
				log("application [" + idA + "] unreachable");
				prop.setProperty(SPConstantes.PROP_STATUT, "2");
				// Le serveur affiche LOG
				// mais le client recoit statut ok
				// pour pouvoir exploiter les autres resultats
			}
		}

		return prop;
	}

	// -----------------------------------------------------------
	// ---------------- traitement des resultats -----------------

	// resultat occupation[]

	/*
	 * 0001 1 TempsLibre 0010 2 Horaire + Hsup 0100 4 Occupé 1000 8 Absent (Congé)
	 * avec les 1 prioritaires -> OU bit à bit : |
	 */

	// pour 1 group et 1 methServ : stockage des occupations trouvées
	// OK
	public static NSMutableArray appendArraySpOccupationFromProperties(
			NSMutableArray arraySpOccupation, Properties prop, String idval, String idkey) {

		String strNbOcc = (String) prop.getProperty(SPConstantes.PROP_OC_NB);

		int nb = Integer.parseInt(strNbOcc);

		for (int i = 0; i < nb; i++) {
			SPOccupation occ = new SPOccupation();

			String strI = Integer.toString(i);

			occ.setDateDebutFromString(prop.getProperty(SPConstantes.PROP_OC_DEB + strI));
			occ.setDateFinFromString(prop.getProperty(SPConstantes.PROP_OC_FIN + strI));
			occ.setTypeTemps(prop.getProperty(SPConstantes.PROP_OC_TYPE + strI));

			if (prop.getProperty(SPConstantes.PROP_OC_DETAIL + strI) != null) {
				occ.setDetailsTemps(prop.getProperty(SPConstantes.PROP_OC_DETAIL + strI));
			}

			occ.setIdVal((Number) new Integer(idval));
			occ.setIdKey((Number) new Integer(idkey));

			arraySpOccupation.addObject(occ);
		}

		return arraySpOccupation;
	}

	// pour 1 group : ajout resultat trouvé au WOResponse final

	public synchronized static Properties getPropertiesOccupation(
			int hashCode, ParamBus aParamBus, Number keyClient,
			NSArray occups, String idval, String idkey, String deb, String fin) {

		// passage par Content

		// traitement -------------------
		Number priorite = (Number) aParamBus.fetchInfoForId(
				keyClient,
				LocalSPConstantes.BD_CLIENT_KEY,
				LocalSPConstantes.BD_MET_CLIENT,
				LocalSPConstantes.BD_CLIENT_TRAIT);

		if (priorite != null) {
			short prior = Short.parseShort(priorite.toString());
			if (prior != 0) // traitement a faire
				occups = traitementOcc(hashCode, aParamBus, occups, new Short(prior), idval, idkey, deb, fin);
		}

		// ajout au WOResponse ------------

		String cle = "";

		int nboccAdd = occups.count();
		int nboccOld = nbOcc;
		// statut a 1 par defaut, puis passe a 0 si une erreur et y reste.
		String detailT = null;
		Properties prop = new Properties();
		for (int i = nboccOld; i < nboccOld + nboccAdd; i++) {
			cle = new BigDecimal(i).toString();

			SPOccupation occ = (SPOccupation) occups.objectAtIndex(i - nboccOld);

			String d = DateCtrl.dateToString(occ.getDateDebut(), SPConstantes.DATE_FORMAT);
			prop.setProperty(SPConstantes.PROP_OC_DEB + cle, d);
			String f = DateCtrl.dateToString(occ.getDateFin(), SPConstantes.DATE_FORMAT);
			prop.setProperty(SPConstantes.PROP_OC_FIN + cle, f);
			prop.setProperty(SPConstantes.PROP_OC_TYPE + cle, occ.getTypeTemps());

			if (occ.getAffichage().equals(SPConstantes.AFF_PRIVEE)) {
				prop.setProperty(SPConstantes.PROP_OC_AFF + cle, occ.getAffichage());
			}

			if (occ.getDetailsTemps() != null && i != nboccOld + nboccAdd - 1) {
				prop.setProperty(SPConstantes.PROP_OC_DETAIL + cle, occ.getDetailsTemps());
			} else {
				detailT = occ.getDetailsTemps();
			}

			prop.setProperty(SPConstantes.PROP_OC_IDVAL + cle, idval);
			prop.setProperty(SPConstantes.PROP_OC_IDKEY + cle, idkey);
		}

		// ajout d'1 signe repere dans details dernier spocc du group
		if (detailT != null) {
			prop.setProperty(SPConstantes.PROP_OC_DETAIL + cle, detailT + "   :-)");
		} else {
			detailT = "  :-)";
			prop.setProperty(SPConstantes.PROP_OC_DETAIL + cle, detailT);
		}

		// mise a jour nbOcc
		nbOcc += nboccAdd;

		return prop;
	}

	// -----------------------------------------------------------

	// resultat number
	// a verifier si plusieurs id (aResponse dej� renseign�)

	public synchronized Properties getPropertiesNumber(Properties result,
			String idval, String idkey) {

		Properties prop = new Properties();
		String num = result.getProperty("n");

		nbNum++;
		prop.setProperty(SPConstantes.PROP_NUMB + nbNum, num);

		return prop;
	}

	// resultat boolean
	// a verifier si plusieurs id (aResponse dej� renseign�)

	public synchronized Properties getPropertiesBoolean(Properties result,
			String idval, String idkey) {
		Properties prop = new Properties();
		String bool = result.getProperty("b");

		nbBool++;
		prop.setProperty(SPConstantes.PROP_BOOL + nbBool, bool);

		return prop;
	}

	private final static String CHECK_PREFIX = "sending heartbeat to ";
	private final static String ERR_APP_UNAVAILABLE = "application unavailable";
	private final static String ERR_MAPFORMED_ULR = "malformed URL";
	private final static String ERR_APP_NOTFOUND = "application not found";

	// donne le tableau des applis non accessibles
	public NSArray checkApplisNonAccessibles() {
		NSMutableArray tab = new NSMutableArray();

		// recup id des applis
		NSArray atab = paramBus.fetchAppliWithMethServeur();

		// la liste des URL deja testees
		NSArray listTestedUrl = new NSArray();

		// recup url
		for (int i = 0; i < atab.count(); i++) {
			CktlRecord rec = (CktlRecord) atab.objectAtIndex(i);
			String id = rec.stringForKey(LocalSPConstantes.BD_SERVEUR_APPLI);
			Number key = rec.numberForKey(LocalSPConstantes.BD_METHODE_KEY);
			String url = paramBus.fetchAppliUrl(id);
			String msg = "id=" + id + ", url=" + url + ", error=";
			StringBuffer sb = new StringBuffer();
			sb.append(CHECK_PREFIX).append(url).append(" ... ");
			// on ne teste pas le serveur de planning en lui meme ...
			if (key != null && key.intValue() == LocalSPConstantes.KEY_METHODE_SERVEUR_ICALENDAR_POUR_PERIODE) {
				sb.append("CANCEL");
			} else {
				if (url != null) {
					// on ne teste pas 2 fois les memes URL
					if (listTestedUrl.containsObject(url)) {
						continue;
					}
					if (url.endsWith("JavaClient.jnlp")) {
						url = url.substring(0, url.length() - 84);
					}
					try {
						URL adr = new URL(url);
						if (!appliAccessible(url, adr.getHost(), adr.getPort())) {
							// appli non accessible
							sb.append(ERR_APP_UNAVAILABLE);
							tab.addObject(msg + ERR_APP_UNAVAILABLE);
						} else {
							sb.append("OK");
						}
					} catch (MalformedURLException e) {
						// url appli mal form�e
						sb.append(ERR_MAPFORMED_ULR);
						tab.addObject(msg + ERR_MAPFORMED_ULR);
					}
					listTestedUrl = listTestedUrl.arrayByAddingObject(url);
				} else {
					// url non trouv�e
					sb.append(ERR_APP_NOTFOUND);
					tab.addObject(msg + ERR_APP_NOTFOUND);
				}
			}
			CktlLog.log(sb.toString());
		}
		CktlLog.rawLog("                            ----------------------------");
		return new NSArray(tab);
	}

	// test si une application est d�marr�e ou non

	private boolean appliAccessible(String adrAppli, String host, int port) {
		boolean ok = false;
		WOHTTPConnection htt = new WOHTTPConnection(host, port);
		htt.setKeepAliveEnabled(false);
		String adress = adrAppli + "/wa/WOAliveCheck";
		WORequest req = new WORequest("GET", adress, "HTTP/1.1", null, null, null);
		boolean bool = htt.sendRequest(req);
		if (bool) // requete bien envoy�e
		{
			String res = htt.readResponse().contentString();
			if (res.equals("OK"))
				ok = true;
		}
		return ok;
	}

	// --------- METHODE D'encodage -----------

	public static Object[] encodeForUrl(Object[] args) {
		// traitement des espaces dans les parametres
		StringTokenizer st = null;
		for (int i = 0; i < args.length; i++) {

			/* URLEncoder.encode(args[i].toString().replaceAll(" ","%20")); */

			st = new StringTokenizer(args[i].toString());
			int intt = st.countTokens();
			if (intt > 1) // espace trouv� -> %20
			{
				String temp = st.nextToken();
				for (int j = 1; j < intt; j++) {
					temp = temp + "%20" + st.nextToken();
				}
				args[i] = temp;
			} // fin if
		} // fin for

		return args;

	} // fin methode myEncode

	// ----------------- traitement Planning ----------------------

	private static NSArray traitementOcc(
			int hashCode, ParamBus aParamBus,
			NSArray occups, Short priorite, String idval,
			String idkey, String deb, String fin) {
		// codage, modif
		NSTimestamp debut = DateCtrl.stringToDate(deb, SPConstantes.DATE_FORMAT);
		NSTimestamp fini = DateCtrl.stringToDate(fin, SPConstantes.DATE_FORMAT);

		// (tab d'SP_Occ final -> tab de Bytes)
		NSArray bytes = codageOccupation(aParamBus, occups, debut, fini);

		NSArray newBytes = traitPrior(bytes, priorite);
		// contient pour la periode demand�e par minute un nombre entre 0 et 15.

		NSArray newOccups = decodageOcc(aParamBus, newBytes, occups, debut, fini);

		return newOccups;
	}

	// OK
	// transforme tab occupations en tab byte (1 octet, 8 bits : 0000 0101)
	public static NSArray codageOccupation(ParamBus aParamBus, NSArray occups, NSTimestamp deb,
			NSTimestamp fin) {
		NSMutableArray occBytes = initPlanCreation(deb, fin);

		for (int nOcc = 0; nOcc < occups.count(); nOcc++) // pour chaque occ
		{
			SPOccupation occ = (SPOccupation) occups.objectAtIndex(nOcc);

			NSTimestamp debOcc = occ.getDateDebut();
			NSTimestamp finOcc = occ.getDateFin();

			long comMilliSec = debOcc.getTime() - deb.getTime();
			long comSec = comMilliSec / 1000;
			long comMin = comSec / 60;

			long durMilliSec = finOcc.getTime() - debOcc.getTime();
			long durSec = durMilliSec / 1000;
			long durMin = durSec / 60;

			long finMin = comMin + durMin;

			if (finMin < 0) // fin occ avant debut periode demand�e
				finMin = 0;
			if (finMin > occBytes.count()) // fin occ apres fin periode demand�e
				finMin = occBytes.count();
			if (comMin < 0) // debut occ avant debut periode demand�e
				comMin = 0;
			if (comMin > occBytes.count()) // debut occ apres fin periode demand�e
				comMin = occBytes.count();

			Number codeTemps = aParamBus.fetchCodeTempForTypeTemp(occ.getTypeTemps());

			// Integer.parseInt(String.valueOf(commence));

			for (int i = (int) comMin; i < finMin; i++) {
				Byte old = (Byte) occBytes.objectAtIndex(i);
				byte neww = (byte) (Short.parseShort(codeTemps.toString()) | Short
						.parseShort(old.toString()));
				occBytes.replaceObjectAtIndex(new Byte(neww), i);
			}
		}
		return new NSArray(occBytes);
	}

	// OK
	// modif planning selon priorit�s d�finies
	private static NSArray traitPrior(NSArray plan1, Short priorite) {
		log("Traitement de niveau " + priorite);

		NSMutableArray plann = new NSMutableArray(plan1);
		int prior = Integer.parseInt(priorite.toString());

		for (int i = 0; i < plann.count(); i++) {
			byte nb = Byte.parseByte(plann.objectAtIndex(i).toString());
			// selon niveau priorit�
			if (prior == 2) // un typeTemps par periode
			{
				if (nb >= 8)
					plann.replaceObjectAtIndex(new Byte("8"), i);
				else if (nb >= 4)
					plann.replaceObjectAtIndex(new Byte("4"), i);
				else if (nb >= 2)
					plann.replaceObjectAtIndex(new Byte("2"), i);
				// else if (nb == 0) // si 1 ou 0 -> 1
				// plann.replaceObjectAtIndex(new Byte("1") , i);

			} else if (prior == 1) // on garde les horaires
			{
				// si 0 ou 1, ne change pas
				if (nb >= 2) {
					// if ((nb/2)*2 != nb) // impair (tempslibre + autre) -> pair (vire
					// tempslibre)
					// plann.replaceObjectAtIndex(new Byte(String.valueOf(nb - 1)) , i);
					if (nb >= 12) // absent (cong�) et occup� -> absent
						plann.replaceObjectAtIndex(new Byte(String.valueOf(nb - 4)), i);
				}
			}
		}
		return plann;
	}

	// methode supprimant les occupations non prioritaires

	private static NSArray decodageOcc(ParamBus aParamBus, NSArray newBytes, NSArray occups,
			NSTimestamp deb, NSTimestamp fin) {

		NSMutableArray modifOccups = new NSMutableArray(occups);

		for (int i = 0; i < modifOccups.count(); i++) // pour chaque occ
		{
			SPOccupation occ = (SPOccupation) modifOccups.objectAtIndex(i);

			Number codeT = aParamBus.fetchCodeTempForTypeTemp(occ.getTypeTemps());

			NSTimestamp debOcc = occ.getDateDebut();
			NSTimestamp finOcc = occ.getDateFin();

			long comMilliSec = debOcc.getTime() - deb.getTime();
			long comSec = comMilliSec / 1000;
			long comMin = comSec / 60;

			long durMilliSec = finOcc.getTime() - debOcc.getTime();
			long durSec = durMilliSec / 1000;
			long durMin = durSec / 60;

			long finMin = comMin + durMin;

			boolean hors = false;
			if (finMin < 0 || comMin > newBytes.count()) // hors periode
				// fin occ avant debut periode demand�e OU
				// debut occ apres fin periode demand�e
				hors = true;
			if (finMin > newBytes.count()) // fin occ apres fin periode demand�e
			{
				finMin = newBytes.count();
				occ.setDateFin(fin);// dateFin devient date fin demand�e
			}
			if (comMin < 0) // debut occ avant debut periode demand�e
			{
				occ.setDateDebut(deb);// dateDeb devient date debut demand�e
				comMin = 0;
			}

			if (hors) // hors periode demand�e
			{
				modifOccups.removeObjectAtIndex(i);
				i--; // pour que boucle prenne en compte occ suivante ayant pris
				// le numero de l'occ supprim�
			} else {
				boolean nothing = true;
				boolean all = true;
				int save1 = 0;
				int save2 = 0;
				for (int min = (int) comMin; min < finMin; min++) {
					// on verifie si occupation est gard�e
					if ((Byte.parseByte(newBytes.objectAtIndex(min).toString()) & Byte
							.parseByte(codeT.toString())) != Byte.parseByte(codeT.toString())) // occ
					// vir�
					{
						all = false; // on n'a pas toute l'occ
						save1 = min;
					} else {
						nothing = false; // on retrouve un bout de l'occ
						save2 = min;
					}
				}
				if (all == false) // on n'a pas tout
				{
					if (nothing == true) // on n'a rien -> on vire
					{
						modifOccups.removeObjectAtIndex(i);
						i--; // pour que boucle prenne en compte occ suivante ayant pris
						// le numero de l'occ supprim�
					} else // on n'a qu'un bout -> modif dates
					{
						if (save1 < save2) // bout a la fin -> changer dateDebut
						{
							long newDebMS = deb.getTime() + ((save1 + 1) * 60000);
							NSTimestamp newDeb = new NSTimestamp(newDebMS);
							occ.setDateDebut(newDeb);
						} else // bout au debut -> changer dateFin
						{
							long newFinMS = deb.getTime() + ((save2 + 1) * 60000);
							NSTimestamp newFin = new NSTimestamp(newFinMS);
							occ.setDateFin(newFin);
						}
					}
				}
			}
		}
		return new NSArray(modifOccups);
	}

	// OK
	private static NSMutableArray initPlanCreation(NSTimestamp deb, NSTimestamp fin) {
		NSMutableArray bytes = new NSMutableArray();

		long nbmillisec = fin.getTime() - deb.getTime();
		long nbsec = nbmillisec / 1000;
		long nbmin = nbsec / 60;
		// a verifier

		for (int i = 0; i < nbmin; i++) {
			bytes.addObject(new Byte("0"));
		}
		return bytes;
	}

	/**
	 * envoi des parametres par URL, ie de la forme :
	 * .../wa/methodeServeur?noIndividu=4371&...
	 */
	private static Properties requetePassageUrl(
			String uri, String urlA, Object[] args, NSArray nomsParam, int hashCode) {

		// url complete pour l'acces DA avec parametres
		args = encodeForUrl(args); // parametres sans espace (%20)
		String serviceAdress = urlA + uri;

		// construction de l'url avec les parametres
		for (int k = 0; k < nomsParam.count(); k++) {
			if (k != 0) {
				serviceAdress += "&";
			}
			serviceAdress += nomsParam.objectAtIndex(k) + "=" + args[k].toString();
		}

		log("[#" + hashCode + "] URL created : [" + serviceAdress + "]");

		SPHTTPConnection htt = new SPHTTPConnection(serviceAdress);
		String strContent = htt.connect();
		Properties prop = new Properties();
		if (!StringCtrl.isEmpty(strContent))
			prop = SPMethodes.stringToProperties(strContent);
		else
			prop.setProperty(SPConstantes.PROP_STATUT, "0");

		return prop;
	}

	/**
	 * @deprecated ??? envoi des parametres dans le content de la requete
	 */
	private static Properties requetePassageContent(
			String nomClient, String uri, String urlA,
			Object[] args, NSArray nomsParam) {
		Properties prop = new Properties();
		prop.setProperty(SPConstantes.PROP_STATUT, "0");

		String _service_address = urlA + uri;

		StringBuffer buffer = new StringBuffer();
		for (int k = 0; k < nomsParam.count(); k++) {
			if (k < 3)
				buffer.append(nomsParam.objectAtIndex(k) + " = " + args[k].toString()
						+ "\n");
			else
				buffer.append(nomsParam.objectAtIndex(k) + " = null \n");
		}
		// ---------- APPEL LA DirectAction --------
		try {
			URL adr = new URL(_service_address); // verifie adresse bien form�e

			WORequest req = new WORequest("GET", adr.toString(), "HTTP/1.1", null,
					null, null);

			String univId = ((CktlWebApplication) CktlWebApplication.application())
					.config().stringForKey(LocalSPConstantes.KEY_UNIV_ID);

			buffer.append(SPConstantes.PAR1_METHCLIENT + " = " + univId + nomClient + "\n");
			req.setContent(buffer.toString());
			WOHTTPConnection htt = new WOHTTPConnection(adr.getHost(), adr.getPort());
			htt.setKeepAliveEnabled(false);

			boolean bool = htt.sendRequest(req);
			if (bool) // requete bien envoyee
			{
				WOResponse wores = htt.readResponse();
				wores.setContentEncoding("UTF-8");
				String res = wores.contentString();
				try {
					byte[] b = res.getBytes();
					if (b != null) {
						ByteArrayInputStream ba = new ByteArrayInputStream(b);
						prop.load(ba);
					}
				} catch (IOException io) {
					log("Probleme IOException : WOResponse de format incorrect");
				} catch (NullPointerException nn) {
					log("Probleme NullPointerException : WOResponse de format incorrect");
				}
			} else {
				log("Probleme : Connection not ok");
				prop.setProperty("erreur",
						"prob serveur: connection methode serv echouee");
			}
		} catch (MalformedURLException e) {
			log("Probleme : URL NOT ok");
			prop.setProperty("erreur", "prob serveur: url methode serv non valide");
		}
		log("URI Methode Serveur: " + uri);
		log("Properties renvoyees : " + prop.toString());
		return prop;
	}

	public NSArray getParams() {
		return params;
	}

	public int getNbPar() {
		return nbPar;
	}

	public int getNbOcc() {
		return nbOcc;
	}

	// ---------------------------------------------------------
	// param par header non url

	/*
	 * private Properties requetePassageHeader(String uri, String urlA, Object[]
	 * args, NSArray nomsParam) { Properties prop = new Properties();
	 * prop.setProperty(Constantes.PROP_STATUT, "0");
	 * 
	 * String url = urlA + uri; WOResponse wores = new WOResponse(); // ----------
	 * APPEL LA DirectAction -------- try { URL adr = new URL(url); // verifie
	 * adresse bien form�e
	 * 
	 * WOHTTPConnection htt = new WOHTTPConnection(adr.getHost(), adr.getPort());
	 * htt.setKeepAliveEnabled(false); // indique que connection doit etre ferm�e
	 * apr�s envoit requete
	 * 
	 * Object[] keys = { nomsParam.objectAtIndex(0), nomsParam.objectAtIndex(1),
	 * nomsParam.objectAtIndex(2) }; Object[] vals = { args[0].toString(),
	 * args[1].toString(), args[2].toString() };
	 * 
	 * WORequest req = new WORequest("GET", adr.toString(), "HTTP/1.1", new
	 * NSDictionary(vals, keys), null, null); // dico plac� dans Header boolean
	 * bool = htt.sendRequest(req); if (bool) // requete bien envoy�e { wores =
	 * htt.readResponse(); String res = wores.contentString(); try { byte[] b =
	 * res.getBytes(); if (b != null) { ByteArrayInputStream ba = new
	 * ByteArrayInputStream(b); prop.load(ba); } } catch (IOException io) {
	 * CktlLog.trace("Probleme IOException : WOResponse de format incorrect"); }
	 * catch (NullPointerException nn) { CktlLog.trace("Probleme
	 * NullPointerException : WOResponse de format incorrect"); } } else {
	 * CktlLog.trace("Probleme : Connection not ok"); prop.setProperty("erreur",
	 * "prob serveur: connection methode serv echou�e"); } } catch
	 * (MalformedURLException e) { CktlLog.trace("Probleme : URL NOT ok");
	 * prop.setProperty("erreur", "prob serveur: url methode serv non valide"); }
	 * CktlLog.trace("URI Methode Serveur: " + uri); CktlLog.trace("Properties
	 * renvoy�es : " + prop.toString()); return prop; }
	 */

	/**
	 * Tracer un message dans la console. On indique le hash qui permet
	 * d'identifier l'instance d'appel lors d'un chainage
	 */
	private static void log(String message) {
		try {
			StringBuffer sb = new StringBuffer(message);
			org.cocktail.fwkcktlwebapp.common.CktlLog.log(sb.toString());
		} catch (Exception e) {
		}
	}

}
