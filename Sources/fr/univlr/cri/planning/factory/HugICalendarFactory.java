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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Random;
import java.util.StringTokenizer;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.ExRule;
import net.fortuna.ical4j.model.property.RDate;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Transp;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.webdav.lib.WebdavResource;
import org.cocktail.fwkcktlwebapp.common.CktlLog;
import org.cocktail.fwkcktlwebapp.common.database.CktlRecord;
import org.cocktail.fwkcktlwebapp.common.util.DateCtrl;
import org.cocktail.fwkcktlwebapp.common.util.StringCtrl;
import org.cocktail.fwkcktlwebapp.server.CktlWebApplication;
import org.cocktail.planning.IcsToSPOccupationFactory;

import com.webobjects.appserver.WOResponse;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestamp;

import fr.univlr.cri.planning.Application;
import fr.univlr.cri.planning.PartagePlanning;
import fr.univlr.cri.planning.SPConstantes;
import fr.univlr.cri.planning.SPOccupation;
import fr.univlr.cri.planning.constant.LocalSPConstantes;
import fr.univlr.cri.planning.datacenter.A_SPDataBusCaching;
import fr.univlr.cri.planning.datacenter.ParamBus;
import fr.univlr.cri.planning.datacenter.SPVEvent;
import fr.univlr.cri.planning.extern.calendartostring.CalendarNotFoundException;
import fr.univlr.cri.planning.extern.calendartostring.CalendarObject;

/**
 * @deprecated a remplacer ...
 * @author ctarade
 * 
 */
public class HugICalendarFactory
		extends A_SPDataBusCaching {

	private int nbEvents;
	private ParamBus paramBus;

	public HugICalendarFactory(ParamBus aParamBus) {
		super(aParamBus.editingContext());
		nbEvents = 0;
		paramBus = aParamBus;
	}

	// private final static String GRHUM_ICAL_TYPE_PRO = "PRO";
	private final static String GRHUM_ICAL_TYPE_PERSO = "PERSO";

	public StringBuffer appendBufferFromICalendarFile(
			StringBuffer buffer, Number noIndividu, String iCalendarPath,
			String typeCal, NSTimestamp deb, NSTimestamp fin, String icsFilename)
			throws CalendarNotFoundException, Exception {

		// CalendarObject cal = newCalendarObjectFromICalendarFile(iCalendarPath);
		CalendarObject cal = newCalendarObjectFromICalendarFileICal4J(iCalendarPath, deb, fin);
		// pour un calendrier inscrit comme "PERSO",
		// on met tous les �v�nements en "CONFIDENTIAL".
		if (typeCal.equals(GRHUM_ICAL_TYPE_PERSO)) {
			NSArray evs = cal.events();
			for (int i = 0; i < evs.count(); i++) {
				VEvent vEvent = (VEvent) evs.objectAtIndex(i);
				vEvent.getProperties().add(Clazz.CONFIDENTIAL);
			}
		}

		// CACHE
		buffer = appendCalendarObjectToStringBuffer(buffer, cal, deb, fin, nbEvents, icsFilename);

		return buffer;

	}

	/**
	 * Lecture d'un fichier icalendar via l'API de la librairie ical4j. Les
	 * occupations contenues dans la fenetre d'interrogation [dateDebut, dateFin]
	 * sont retournees.
	 * 
	 * @param pathICalendar
	 *          : le chemin du fichier ICS
	 * @param dateDebut
	 *          : date de debut de la periode d'interrogation
	 * @param dateFin
	 *          : date de fin de la periode d'interrogation
	 * @return
	 * @throws CalendarNotFoundException
	 */
	private CalendarObject newCalendarObjectFromICalendarFileICal4J(
			String pathICalendar, NSTimestamp dateDebut, NSTimestamp dateFin)
			throws CalendarNotFoundException {

		CalendarObject oCal = null;

		InputStream in = null;
		try {
			// fin = new FileInputStream(pathICalendar);
			URL url = new URL(pathICalendar);
			URLConnection con = url.openConnection();
			con.connect();
			in = con.getInputStream();
		} catch (FileNotFoundException e) {
			throw new CalendarNotFoundException("Calendar " + pathICalendar + " non trouvé ou accès refusé.");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		 * CalendarBuilder calendarBuilder = new CalendarBuilder();
		 * 
		 * try { long l1 = System.currentTimeMillis(); Calendar calendar =
		 * calendarBuilder.build(in); oCal = new CalendarObject(pathICalendar); l1 =
		 * System.currentTimeMillis() - l1; CktlLog.log(">> parsing : " + l1 +
		 * " ms ("+pathICalendar+")");
		 * 
		 * // on passe au evenements for (Iterator i =
		 * calendar.getComponents().iterator(); i.hasNext();) { Component component
		 * = (Component) i.next(); if (component.getName().equals(Component.VEVENT))
		 * { VEvent vEvent = (VEvent) component; oCal.addSPVEvent(new
		 * SPVEvent(vEvent)); } }
		 * 
		 * 
		 * 
		 * } catch (IOException e) { throw new
		 * CalendarNotFoundException("Calendar "+ pathICalendar
		 * +" erreur de lecture " + e.getMessage()); } catch (ParserException e) {
		 * throw new CalendarNotFoundException("Calendar "+ pathICalendar
		 * +" erreur de lecture " + e.getMessage()); }
		 */

		String string = null;

		try {
			long l1 = System.currentTimeMillis();
			URL url = new URL(pathICalendar);
			URLConnection con = url.openConnection();
			con.connect();

			BufferedInputStream bin = new BufferedInputStream(con.getInputStream());
			StringWriter out = new StringWriter();
			int b;
			while ((b = bin.read()) != -1)
				out.write(b);
			out.flush();
			out.close();
			bin.close();
			string = out.toString();
			l1 = System.currentTimeMillis() - l1;
			System.out.println("converting calendar url to string : " + l1 + " ms (" + pathICalendar + ")");
		} catch (IOException ie) {
			ie.printStackTrace();
		}

		long l1 = System.currentTimeMillis();
		StringReader sin = new StringReader(string);
		CalendarBuilder builder = new CalendarBuilder();

		Calendar calendar = null;
		try {
			calendar = builder.build(sin);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		l1 = System.currentTimeMillis() - l1;
		System.out.println("parse calendar string : " + l1 + " ms (" + pathICalendar + ")");

		oCal = new CalendarObject(pathICalendar);

		// on passe au evenements
		for (Iterator i = calendar.getComponents().iterator(); i.hasNext();) {
			Component component = (Component) i.next();
			if (component.getName().equals(Component.VEVENT)) {
				VEvent vEvent = (VEvent) component;
				oCal.addSPVEvent(new SPVEvent(vEvent));
			}
		}

		// on met en cache notre affaire
		if (oCal != null) {
			putCalendarInCache(pathICalendar, oCal);
		}

		return oCal;
	}

	// CalendarObject -> StringBuffer pour WOResponse

	/**
	 * 
	 */
	public StringBuffer appendCalendarObjectToStringBuffer(
			StringBuffer buffer, CalendarObject oCal, NSTimestamp debut, NSTimestamp fin, int nbEv, String icsFilename) {
		NSMutableArray events = oCal.events();
		nbEvents = nbEv;
		for (int i = 0; i < events.count(); i++) {
			SPVEvent vEvent = (SPVEvent) events.objectAtIndex(i);
			buffer = appendBufferSPVEvent(buffer, vEvent, debut, fin, icsFilename);
		}

		return buffer;
	}

	/**
	 * 
	 */
	private StringBuffer appendBufferSPVEvent(StringBuffer buffer, SPVEvent sPVEvent,
			NSTimestamp debutPeriode, NSTimestamp finPeriode, String icsFileName) {

		NSTimestamp debut = sPVEvent.dateDebut();
		NSTimestamp fin = sPVEvent.dateFin();

		if (sPVEvent.isVisible()) {

			if (sPVEvent.isRepetition()) {
				PeriodList consumed = null;
				try {
					consumed = periodeList(sPVEvent.getVEVent(), debutPeriode, finPeriode, true);
				} catch (ParseException e) {
					e.printStackTrace();
				}
				if (consumed != null) {
					NSArray consumedArray = new NSArray(consumed.toArray());
					for (int j = 0; j < consumedArray.count(); j++) {
						Period period = (Period) consumedArray.objectAtIndex(j);

						// si events ni avant, ni apres periode demand�
						if (isIncluded(debut, fin, debutPeriode, finPeriode)) {
							buffer.append("debut" + nbEvents + " = " + DateCtrl.dateToString(
									new NSTimestamp(period.getStart().getTime()), SPConstantes.DATE_FORMAT) + "\n");
							buffer.append("fin" + nbEvents + " = " + DateCtrl.dateToString(
									new NSTimestamp(period.getEnd().getTime()), SPConstantes.DATE_FORMAT) + "\n");
							// buffer.append("type"+nbEvents+" = "+ev.type()+"\n");
							buffer.append("type" + nbEvents + " = " + icsFileName + "\n");
							buffer.append("affichage" + nbEvents + " = " + sPVEvent.classDisplay() + "\n");
							// pas de detail pour les event "PRIVATE"
							if (sPVEvent.isClassePublic()) {
								buffer.append("detail" + nbEvents + " = " + sPVEvent.details() + "\n"); // verifier
																																												// si
																																												// \n
																																												// dans
																																												// description
																																												// par
																																												// ex
							} else {
								buffer.append("detail" + nbEvents + " = <Evenement prive>\n"); // verifier
																																								// si
																																								// \n
																																								// dans
																																								// description
																																								// par
																																								// ex
							}
							nbEvents++;
						}
					}
				}
			} else {
				if (isIncluded(debut, fin, debutPeriode, finPeriode)) {
					buffer.append("debut" + nbEvents + " = " + DateCtrl.dateToString(
							sPVEvent.dateDebut(), SPConstantes.DATE_FORMAT) + "\n");
					buffer.append("fin" + nbEvents + " = " + DateCtrl.dateToString(
							sPVEvent.dateFin(), SPConstantes.DATE_FORMAT) + "\n");
					// buffer.append("type"+nbEvents+" = "+ev.type()+"\n");
					buffer.append("type" + nbEvents + " = " + icsFileName + "\n");
					buffer.append("affichage" + nbEvents + " = " + sPVEvent.classDisplay() + "\n");
					// pas de detail pour les event "PRIVATE"
					if (sPVEvent.isClassePublic()) {
						buffer.append("detail" + nbEvents + " = " + sPVEvent.details() + "\n"); // verifier
																																										// si
																																										// \n
																																										// dans
																																										// description
																																										// par
																																										// ex
					} else {
						buffer.append("detail" + nbEvents + " = <Evenement prive>\n"); // verifier
																																						// si
																																						// \n
																																						// dans
																																						// description
																																						// par
																																						// ex
					}
					nbEvents++;
				}
			}
		}

		return buffer;
	}

	/**
	 * Est-ce les dates sont comprises dans les intervalles. Si les intervalles
	 * sont vide, alors on retourne <code>true</code>.
	 */
	private boolean isIncluded(NSTimestamp debut, NSTimestamp fin,
			NSTimestamp limitDebut, NSTimestamp limitFin) {
		return ((limitFin != null && limitDebut != null && !fin.before(limitDebut) && !debut.after(limitFin)))
				|| (limitFin == null && limitDebut == null);
	}

	// METHODE PRINCIPALE SI Requete interne du ServeurPlanning

	public StringBuffer parseICalendarFileForPeriodToStringBuffer(
			Number noIndividu, NSTimestamp debut, NSTimestamp fin) {
		String err = "";
		Boolean statut = Boolean.TRUE;

		NSArray<SPOccupation> spOccupationList = new NSArray<SPOccupation>();

		if (noIndividu == null) {
			err = "Parametre noIndividu absent.";
			statut = Boolean.FALSE;
			CktlLog.trace("parameter <noIndividu> missing.");
		} else {
			NSArray recsIcal = paramBus.fetchICalendar(noIndividu);
			int nbEvents = 0;
			for (int i = 0; i < recsIcal.count(); i++) {

				CktlRecord recIcal = (CktlRecord) recsIcal.objectAtIndex(i);

				String racine = recIcal.stringForKey(LocalSPConstantes.BD_ICAL_LIEN);
				String fichierIcs = recIcal.stringForKey(LocalSPConstantes.BD_ICAL_NAME);

				String urlIcal = racine;

				if (!StringCtrl.isEmpty(fichierIcs)) {
					if (!racine.endsWith("/")) {
						urlIcal += "/";
					}
					urlIcal += fichierIcs;
				}

				// utiliser le cache
				String strIcalKey = Integer.toString(((Integer) EOUtilities.primaryKeyForObject(recIcal.editingContext(), recIcal).valueForKey("key")).intValue());

				// creer la cle
				String key = buildKey(
						strIcalKey,
						Integer.toString(SPConstantes.IDKEY_INDIVIDU.intValue()),
						Integer.toString(noIndividu.intValue()),
						null,
						null);

				CktlLog.log(">>>>>>>>> " + strIcalKey + " = " + urlIcal);

				// tenter une recuperation depuis le cache
				NSArray<SPOccupation> allSpOccupationList = (NSArray<SPOccupation>) getObjectFromCache(key);

				// si le cache est indisponible, alors on va relire et traiter le
				// fichier depuis la source
				if (allSpOccupationList == null) {
					// toutes les occupations issues du calendrier ICS
					allSpOccupationList = IcsToSPOccupationFactory.parse(urlIcal);
					// sauvegarde de la liste en cache
					putObjectInCache(key, allSpOccupationList);
				}

				// ne conserver que celles qui sont inclues dans la période demandée
				for (int j = 0; j < allSpOccupationList.count(); j++) {
					SPOccupation occupation = allSpOccupationList.objectAtIndex(j);
					if (isIncluded(occupation.getDateDebut(), occupation.getDateFin(), debut, fin)) {
						spOccupationList = spOccupationList.arrayByAddingObject(occupation);
					}
				}

			}

		}

		StringBuffer buffer = new StringBuffer();

		WOResponse responsePlanning = PartagePlanning.reponsePlanning(spOccupationList, 1, "");

		// CktlLog.log("*************** encoding:" +
		// responsePlanning.contentEncoding());

		buffer.append(responsePlanning.contentString());

		return buffer;
	}

	// Creation du fichier ics dans le chemin sp�cifi�

	// fonctionne pour un chemin classic "C:..." ou WebDav de l'univ
	public boolean createFileICalendar(String res, String cheminEtNom) {
		boolean statut = false;

		CktlLog.trace("trying creating .ics file : [" + cheminEtNom + "]");

		// tester si chemin classic ou WebDav
		if (cheminEtNom != null) {
			if (!cheminEtNom.endsWith(LocalSPConstantes.ICS_FILE_NAME_EXTENSION))
				cheminEtNom += LocalSPConstantes.ICS_FILE_NAME_EXTENSION;

			String url = ((CktlWebApplication) CktlWebApplication.application())
					.config().stringForKey(LocalSPConstantes.KEY_ICAL_CHEMIN);

			if (cheminEtNom.startsWith(url)) {
				// url webdav
				// pour trouver chemin
				String chemin = "http://";
				StringTokenizer st = new StringTokenizer(cheminEtNom.substring(7), "/");
				int nb = st.countTokens();
				for (int i = 0; i < nb - 1; i++) {
					chemin += st.nextToken() + "/";
				}

				String log = ((CktlWebApplication) CktlWebApplication.application())
						.config().stringForKey(LocalSPConstantes.KEY_ICAL_LOG);
				String pwd = ((CktlWebApplication) CktlWebApplication.application())
						.config().stringForKey(LocalSPConstantes.KEY_ICAL_PWD);

				try {
					WebdavResource webd = new WebdavResource(
							chemin, new UsernamePasswordCredentials(log, pwd));
					// cr�ation et �criture du fichier
					webd.putMethod(cheminEtNom, res);
					statut = true;
				} catch (Exception e) {
					CktlLog.trace("WebDAV encountered problems while creating .ics file ...");
				}

			} else if (cheminEtNom.startsWith("http")) {
				CktlLog.trace("problems creating .ics file [" + cheminEtNom + "], path must be WebDAV server or local file.");
			} else {
				// chemin local
				File f = new File(cheminEtNom);
				try {
					FileOutputStream stream = new FileOutputStream(f);
					byte[] bits = res.getBytes();
					stream.write(bits);
					statut = true;
					stream.close();
				} catch (FileNotFoundException e) {
					CktlLog.trace("problem while creating local .ics file : FileNotFoundException");
				} catch (IOException io) {
					CktlLog.trace("problem while writing local .ics file : IOException");
				}
			}
		}

		if (statut)
			CktlLog.trace("creation successfull !");
		else
			CktlLog.trace("creation failed !");

		return statut;
	}

	// getters

	public int getNbEvents() {
		return nbEvents;
	}

	// ** les differents cache **

	private static NSMutableDictionary theDicoCacheICalendarObjects;

	/**
	 * Le nom des cles de chaque sous-dico du cache
	 */
	private final static String CACHE_KEY_TIMEOUT = "TIMEOUT";
	private final static String CACHE_KEY_ICALENDAR_OBJECT = "ICALENDAR_OBJECT";

	/**
	 * TTL d'un object <code>CalendarObject</code> en cache (ms)
	 */
	private final static int timeoutCalendarObject = ((Application) Application.application()).ttlICalendarRead();

	/**
	 * Le dictionnaire contenant tous les fichiers ics convertis en objet
	 * <code>CalendarObject</code>. La structure est :
	 * 
	 * key : icsUrl value : NSDictionary key=CACHE_KEY_TIMEOUT, value=(Long)<duree
	 * de vie max> key=CACHE_KEY_ICALENDAR_OBJECT value=(CalendarObject)<cache>
	 */
	private NSMutableDictionary dicoICalendarObjects() {
		if (theDicoCacheICalendarObjects == null)
			theDicoCacheICalendarObjects = new NSMutableDictionary();
		return theDicoCacheICalendarObjects;
	}

	/**
	 * Ajouter un objet <code>CalendarObject</code> dans le dictionnaire de cache.
	 * La duree de vie est initialisee.
	 */
	private void putCalendarInCache(String iCalendarUrl, CalendarObject calendar) {
		NSDictionary existingDico = (NSDictionary) dicoICalendarObjects().objectForKey(iCalendarUrl);
		if (existingDico == null) {
			NSMutableDictionary newDico = new NSMutableDictionary();
			newDico.setObjectForKey(calendar, CACHE_KEY_ICALENDAR_OBJECT);
			// duree de vie
			newDico.setObjectForKey(
					new Long(System.currentTimeMillis() + timeoutCalendarObject), CACHE_KEY_TIMEOUT);
			dicoICalendarObjects().setObjectForKey(newDico, iCalendarUrl);
			// CktlLog.trace("caching : ["+iCalendarUrl+"]");
		}
	}

	/**
	 * Recupere l'objet <code>CalendarObject</code> a partir du cache. Si ce
	 * dernier est trouve avec une duree de vie maximal non depassee, alors on le
	 * retourne. Si l'objet est trouve mais perime, il est supprime. Si l'objet
	 * est non trouve ou perime, alors on retourne <code>null</code>
	 */
	public CalendarObject getCalendarObjectFromCache(String iCalendarUrl) {
		CalendarObject calendar = null;
		NSDictionary dico = (NSDictionary) dicoICalendarObjects().objectForKey(iCalendarUrl);
		if (dico != null) {
			Long maxTime = (Long) dico.valueForKey(CACHE_KEY_TIMEOUT);
			long timeRemaining = (maxTime != null ? maxTime.longValue() - System.currentTimeMillis() : 0);
			// pas perime
			if (timeRemaining > 0) {
				// CktlLog.trace("cache retrevied : ["+iCalendarUrl+"] - " +
				// timeRemaining +"ms remaining");
				calendar = (CalendarObject) dico.valueForKey(CACHE_KEY_ICALENDAR_OBJECT);
			} else {
				// obsolete : on efface
				// CktlLog.trace("cache removed : ["+iCalendarUrl+"] - out-of-date since "
				// + (-timeRemaining) +"ms");
				dicoICalendarObjects().removeObjectForKey(iCalendarUrl);
			}
		} /*
			 * else { CktlLog.trace("calendar not in cache : ["+iCalendarUrl+"]"); }
			 */
		return calendar;
	}

	// traitement des objets de la librairie ical4j

	/**
	 * Permet de transformer un VEVENT decrivant une repetition de dates, avec
	 * eventuellement des exceptions en liste de periode (occupations uniques).
	 * 
	 * @param dateDebut
	 *          : date de debut de la periode d'interrogation
	 * @param dateFin
	 *          : date de fin de la periode d'interrogation
	 * 
	 *          TODO a deplacer dans un structure centralisee
	 */
	public PeriodList periodeList(
			VEvent vEvent, NSTimestamp dateDebut, NSTimestamp dateFin, boolean normalise) throws ParseException {
		DtStart start = vEvent.getStartDate();
		DtEnd end = vEvent.getEndDate();
		Duration duration = vEvent.getDuration();
		Transp transp = vEvent.getTransparency();
		PeriodList periods = new PeriodList();
		if (transp != null && Transp.TRANSPARENT.equals(transp.getValue()))
			return periods;
		if (start == null || duration == null && end == null)
			return periods;
		Dur rDuration;
		if (duration == null)
			rDuration = new Dur(start.getDate(), end.getDate());
		else
			rDuration = duration.getDuration();

		PropertyList rDates = vEvent.getProperties(Property.RDATE);
		for (Iterator i = rDates.iterator(); i.hasNext();) {
			RDate rdate = (RDate) i.next();
			if (Value.PERIOD.equals(rdate.getParameter("VALUE"))) {
				Iterator j = rdate.getPeriods().iterator();
				while (j.hasNext()) {
					periods.add((Period) j.next());
				}
			}
		}

		// la periode de fin d'interrogation est la meme que celle de l'application
		Date periodEnd = new Date(DateCtrl.dateToString(dateFin, "yyyyMMdd"));

		PropertyList rRules = vEvent.getProperties(Property.RRULE);
		for (Iterator i = rRules.iterator(); i.hasNext();) {
			RRule rrule = (RRule) i.next();
			DateList startDates = rrule.getRecur().getDates(start.getDate(), periodEnd, (Value) start.getParameter("VALUE"));
			int j = 0;
			while (j < startDates.size()) {
				Date startDate = (Date) startDates.get(j);
				// on reconstruit un objet DateTime pour conserver la bonne duree en
				// heures
				DateTime startDateTime = new DateTime(startDate.getTime());
				Period newPeriod = new Period(startDateTime, rDuration);
				periods.add(newPeriod);
				j++;
			}
		}

		if (start.getDate().before(periodEnd)) {
			if (end != null && end.getDate().after(start.getDate())) {
				periods.add(new Period(new DateTime(start.getDate()), new DateTime(end.getDate())));
			} else {
				if (duration != null) {
					DateTime startDateTime = new DateTime(start.getDate().getTime());
					Period newPeriod = new Period(startDateTime, duration.getDuration());
					if (newPeriod.getEnd().after(start.getDate())) {
						periods.add(newPeriod);
					}
				}
			}
		}

		PropertyList exDates = vEvent.getProperties(Property.EXDATE);
		for (Iterator i = exDates.iterator(); i.hasNext();) {
			ExDate exDate = (ExDate) i.next();
			Iterator periodIterator = periods.iterator();
			while (periodIterator.hasNext()) {
				Period period = (Period) periodIterator.next();
				if (exDate.getDates().contains(period.getStart()) || exDate.getDates().contains(new Date(period.getStart()))) {
					periodIterator.remove();
				}
			}
		}

		PropertyList exRules = vEvent.getProperties(Property.EXRULE);
		PeriodList exPeriods = new PeriodList();
		for (Iterator i = exRules.iterator(); i.hasNext();) {
			ExRule exrule = (ExRule) i.next();
			DateList startDates = exrule.getRecur().getDates(start.getDate(), periodEnd, (Value) start.getParameter("VALUE"));
			Iterator j = startDates.iterator();
			while (j.hasNext()) {
				Date startDate = (Date) j.next();
				DateTime startDateTime = new DateTime(startDate.getTime());
				Period newPeriod = new Period(startDateTime, rDuration);
				exPeriods.add(newPeriod);
			}
		}

		if (!exPeriods.isEmpty())
			periods = periods.subtract(exPeriods);
		if (!periods.isEmpty() && normalise)
			return periods.normalise();
		else
			return periods;
	}

	private Integer ttlObjectCalendarStream;

	public int ttlObject() {
		if (ttlObjectCalendarStream == null) {
			ttlObjectCalendarStream = new Integer(app().ttlICalendarRead());
		}
		return ttlObjectCalendarStream.intValue();
	}

	public static String getNomFichierPourUrlIcs(String urlIcs) {
		Random random = new Random();

		String nomFichier = urlIcs.substring(urlIcs.lastIndexOf('/') + 1);
		nomFichier = StringCtrl.normalize(nomFichier);
		nomFichier = Integer.toString(random.nextInt(999999999)) + "_" + nomFichier;

		return nomFichier;
	}
}
