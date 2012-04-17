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
package org.cocktail.planning;

import java.text.ParseException;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.Categories;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.Comment;
import net.fortuna.ical4j.model.property.Contact;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.RecurrenceId;
import net.fortuna.ical4j.model.property.Resources;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Uid;

import org.cocktail.fwkcktlwebapp.common.util.StringCtrl;

import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSTimestamp;

import fr.univlr.cri.planning.Application;
import fr.univlr.cri.planning.SPOccupation;
import fr.univlr.cri.planning._imports.DateCtrl;
import fr.univlr.cri.planning.constant.LocalSPConstantes;

/**
 * Surcharge de la classe de ical4j VEvent pour avoir acces à des methodes
 * particulieres
 * 
 * @author ctarade
 */
public class SPOccupationVEvent {

	private VEvent vEvent;
	private String urlCalendar;
	private SPOccupation _spOccupation;
	private NSMutableArray<SPOccupation> _spOccupationArray;
	private Calendar calendar;

	private final static String DATE_FORMAT = "%Y%m%dT%H%M%S";

	public SPOccupationVEvent(String anUrlCalendar, VEvent aVEvent, Calendar aCalendar) {
		super();
		vEvent = aVEvent;
		urlCalendar = anUrlCalendar;
		calendar = aCalendar;
	}

	public SPOccupation getSPOccupation() {
		if (_spOccupation == null) {
			Date dateDebut = ((DtStart) vEvent.getProperties().getProperty(Property.DTSTART)).getDate();
			Date dateFin = ((DtEnd) vEvent.getProperties().getProperty(Property.DTEND)).getDate();
			_spOccupation = new SPOccupation(
					getNSTimestampForDate(dateDebut),
					getNSTimestampForDate(dateFin),
					icsSuffix(),
					details(),
					getUid());
			_spOccupation.setAffichage(classDisplay());
			if (!isClassePublic()) {
				_spOccupation.setDetailsTemps("<Evènement privé>");
			}
		}
		return _spOccupation;
	}

	/**
	 * test si le debut est bien avant la fin (pour eviter les erreurs
	 * java.lang.IllegalArgumentException: Range start must be before range end
	 * 
	 * @return
	 */
	private boolean isDebutAvantFin() {
		boolean isDebutAvantFin = false;

		Date dateDebut = ((DtStart) vEvent.getProperties().getProperty(Property.DTSTART)).getDate();
		Date dateFin = ((DtEnd) vEvent.getProperties().getProperty(Property.DTEND)).getDate();

		NSTimestamp tsDebut = getNSTimestampForDate(dateDebut);
		NSTimestamp tsFin = getNSTimestampForDate(dateFin);

		if (DateCtrl.isBeforeEq(tsDebut, tsFin)) {
			isDebutAvantFin = true;
		}

		return isDebutAvantFin;
	}

	public NSMutableArray<SPOccupation> getSpOccupationArray() {
		if (_spOccupationArray == null) {
			_spOccupationArray = new NSMutableArray<SPOccupation>();

			if (isDebutAvantFin()) {

				try {
					// Create the date range which is desired.
					DateTime from = new DateTime(DateCtrl.dateToString(app().icsDateDebut(), "%Y%m%dT000000Z"));
					DateTime to = new DateTime(DateCtrl.dateToString(app().icsDateFin(), "%Y%m%dT000000Z"));

					Period periodInterrogation = new Period(from, to);
					PeriodList pl = vEvent.calculateRecurrenceSet(periodInterrogation);
					for (Object po : pl) {
						Period period = (Period) po;
						NSTimestamp tsStart = getNSTimestampForDate(period.getStart());
						NSTimestamp tsEnd = getNSTimestampForDate(period.getEnd());
						if (DateCtrl.isBeforeEq(tsStart, tsEnd)) {
							SPOccupation spOcc = new SPOccupation(
									tsStart,
									tsEnd,
									icsSuffix(),
									details(),
									getUid());
							_spOccupationArray.addObject(spOcc);
						}
					}

				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
		return _spOccupationArray;
	}

	/**
	 * Le UID de l'objet {@link #vEvent} associé
	 * 
	 * @return
	 */
	public String getUid() {
		String uid = null;

		Uid propUid = (Uid) vEvent.getProperties().getProperty(Property.UID);
		if (!StringCtrl.isEmpty(propUid.toString())) {
			uid = propUid.getValue();
		}

		return uid;
	}

	/**
	 * La date de début de l'occupation parmi une liste à soustraire si
	 * {@link #vEvent} est une reccurence
	 * 
	 * @return
	 */
	public NSTimestamp getDateDebutReccurence() {
		NSTimestamp date = null;

		RecurrenceId propDate = (RecurrenceId) vEvent.getProperties().getProperty(Property.RECURRENCE_ID);
		if (!StringCtrl.isEmpty(propDate.toString())) {
			date = getNSTimestampForDate(propDate.getDate());
		}

		return date;
	}

	/**
	 * Le typage de l'occupation (pour affichage entre crochets)
	 * 
	 * @return
	 */
	private String icsSuffix() {
		String icsSuffix = "";

		String icsSuffixPattern = app().icsSuffixPattern();
		if (icsSuffixPattern.equals(LocalSPConstantes.ICS_SUFFIX_PATTERN_VALUE_FICHIER)) {
			// nom de fichier
			String icsFileName = urlCalendar;
			if (StringCtrl.containsIgnoreCase(urlCalendar, "/") && !urlCalendar.endsWith("/")) {
				icsFileName = urlCalendar.substring(urlCalendar.lastIndexOf("/") + 1);
			}
			icsSuffix = icsFileName;
		} else if (icsSuffixPattern.equals(LocalSPConstantes.ICS_SUFFIX_PATTERN_VALUE_DOMAINE)) {
			// nom de domaine
			String domainName = urlCalendar;
			icsSuffix = urlCalendar;
			if (StringCtrl.containsIgnoreCase(urlCalendar, "://")) {
				domainName = urlCalendar.substring(urlCalendar.lastIndexOf("://") + 3, urlCalendar.length());
				icsSuffix = domainName;
				if (StringCtrl.containsIgnoreCase(domainName, "/")) {
					domainName = domainName.substring(0, domainName.indexOf("/"));
					icsSuffix = domainName;
					// ne conserver que xxx.yyy
					if (StringCtrl.containsIgnoreCase(domainName, ".")) {
						String extension = domainName.substring(domainName.lastIndexOf("."));
						domainName = domainName.substring(0, domainName.lastIndexOf(extension));
						if (StringCtrl.containsIgnoreCase(domainName, ".")) {
							domainName = domainName.substring(domainName.lastIndexOf(".") + 1, domainName.length());
						}
						icsSuffix = domainName + extension;
					}
				}
			}
		} else if (icsSuffixPattern.equals(LocalSPConstantes.ICS_SUFFIX_PATTERN_VALUE_X_WR_CALNAME)) {
			// clé X-WR-CALNAME
			Property propCalName = calendar.getProperty("X-WR-CALNAME");
			if (propCalName != null && !StringCtrl.isEmpty(propCalName.toString())) {
				icsSuffix = propCalName.getValue();
			}
		} else {
			// chaine statique
			icsSuffix = icsSuffixPattern;
		}

		return icsSuffix;
	}

	/**
	 * Raccourcis vers les paramètres de l'application
	 * 
	 * @return
	 */
	private Application app() {
		return ((Application) Application.application());
	}

	/**
	 * Indique si {@link #vEvent} est un evenement à répétition, auquel cas il
	 * faudra faire un traitement particulier
	 */
	public boolean isRepetition() {
		boolean isRepetition = false;

		if (vEvent.getProperties().getProperty(Property.RRULE) != null ||
				vEvent.getProperties().getProperty(Property.RDATE) != null ||
				vEvent.getProperties().getProperty(Property.EXRULE) != null ||
				vEvent.getProperties().getProperty(Property.EXDATE) != null) {
			isRepetition = true;
		}

		return isRepetition;
	}

	/**
	 * Indique si {@link #vEvent} est une occupation dérogatoire d'une répétition
	 * d'autres occupations (précédentes)
	 * 
	 * @return
	 */
	public boolean isRecurrence() {
		boolean isRecurrence = false;

		if (vEvent.getProperties().getProperty(Property.RECURRENCE_ID) != null) {
			isRecurrence = true;
		}

		return isRecurrence;
	}

	private NSTimestamp getNSTimestampForDate(Date date) {
		NSTimestamp tsMaison = DateCtrl.stringToDate(date.toString(), DATE_FORMAT);

		if (tsMaison == null) {
			tsMaison = new NSTimestamp(date.getTime());
		}
		return tsMaison;
	}

	/*
	 * private NSTimestamp dateDebut(Date date) { DtStart dtStartDebut = (DtStart)
	 * aVevent.getProperties().getProperty(Property.DTSTART);
	 * 
	 * NSTimestamp tsMaison =
	 * DateCtrl.stringToDate(dtStartDebut.getDate().toString(), DATE_FORMAT);
	 * 
	 * if (tsMaison == null) { tsMaison = new
	 * NSTimestamp(dtStartDebut.getDate().getTime()); } return tsMaison; }
	 * 
	 * private NSTimestamp dateFin(VEvent aVevent) {
	 * 
	 * DtEnd dtEndFin = (DtEnd)
	 * aVevent.getProperties().getProperty(Property.DTEND);
	 * 
	 * NSTimestamp tsMaison = DateCtrl.stringToDate(dtEndFin.getDate().toString(),
	 * DATE_FORMAT);
	 * 
	 * if (tsMaison == null) { tsMaison = new
	 * NSTimestamp(dtEndFin.getDate().getTime()); }
	 * 
	 * return tsMaison; }
	 */

	// -------- Motif de l'occupation

	private String type() {
		// ajout Enseignement et Examen ???
		String type = "BLOCAGE";
		String titreMAJ = (summary() != null ? summary().getValue().toUpperCase() : "");
		if (titreMAJ.startsWith("REUNION") ||
				titreMAJ.startsWith("R�UNION") ||
				titreMAJ.startsWith("RéUNION")) {
			type = "REUNION";
		} else if (titreMAJ.startsWith("FORMATION")) {
			type = "FORMATION";
		} else if (categories() != null) {
			if (StringCtrl.containsIgnoreCase(categories().getValue(), "REUNION") ||
					StringCtrl.containsIgnoreCase(categories().getValue(), "R�UNION") ||
					StringCtrl.containsIgnoreCase(categories().getValue(), "RéUNION")) {
				type = "REUNION";
			} else if (StringCtrl.containsIgnoreCase(categories().getValue(), "FORMATION")) {
				type = "FORMATION";
			}
		}

		return type;
	}

	private final static int DETAILS_MAX_SIZE = 245;

	/**
	 * 
	 * @return
	 */
	private String details() {
		String details = "";
		if (summary() != null) {
			details += summary().getValue();
		}
		checkSizeAndAppend(details, "\\n description : ", description(), DETAILS_MAX_SIZE);
		checkSizeAndAppend(details, "\\n organisateur : ", organizer(), DETAILS_MAX_SIZE);
		checkSizeAndAppend(details, "\\n lieu : ", location(), DETAILS_MAX_SIZE);
		checkSizeAndAppend(details, "\\n resources : ", resources(), DETAILS_MAX_SIZE);
		checkSizeAndAppend(details, "\\n invites : ", attendee(), DETAILS_MAX_SIZE);
		checkSizeAndAppend(details, "\\n contact : ", contact(), DETAILS_MAX_SIZE);
		checkSizeAndAppend(details, "\\n commentaire : ", comment(), DETAILS_MAX_SIZE);

		return details;
	}

	/**
	 * Methode interne de controle de la taille des champs.
	 * 
	 * @param stringOrginal
	 * @param prefix
	 * @param stringToAppend
	 * @param maxSize
	 * @return <em>false</em> si la taille est depasse ou bien si
	 *         <code>stringToAppend</code> est vide
	 */
	private String checkSizeAndAppend(String stringOrginal, String prefix, Property propertyToAppend, int maxSize) {
		return (propertyToAppend != null && !StringCtrl.isEmpty(propertyToAppend.getValue()) && stringOrginal.length() + prefix.length() + propertyToAppend.getValue().length() < maxSize) ?
						stringOrginal + prefix + propertyToAppend.getValue() : "";
	}

	// etat

	/**
	 * Un event est visible si le statut est : - sans statut - statut confirme
	 */
	private boolean isStatusVisible() {
		return status() == null ||
				status().equals(Status.VEVENT_CONFIRMED);
	}

	// classe

	/**
	 * Un event est visible si la classe est : - sans classe - classe publique ou
	 * privee
	 */
	private boolean isClazzVisible() {
		return clazz() == null ||
				clazz().equals(Clazz.PUBLIC) ||
				clazz().equals(Clazz.PRIVATE);
	}

	/**
	 * Un event est visible si la classe est : - sans classe - classe publique ou
	 * privee
	 */
	private boolean isClassePublic() {
		return clazz() == null ||
				clazz().equals(Clazz.PUBLIC);
	}

	//

	/**
	 * Visibilite event
	 */
	private boolean isVisible() {
		return isStatusVisible() && isClazzVisible();
	}

	//

	private String classDisplay() {
		return (clazz() == null ? Clazz.PUBLIC.toString() : clazz().getValue());
	}

	// raccourcis vers les proprietes

	private Summary summary() {
		return (Summary) vEvent.getProperties().getProperty(Property.SUMMARY);
	}

	private Categories categories() {
		return (Categories) vEvent.getProperties().getProperty(Property.CATEGORIES);
	}

	private Description description() {
		return (Description) vEvent.getProperties().getProperty(Property.DESCRIPTION);
	}

	private Location location() {
		return (Location) vEvent.getProperties().getProperty(Property.LOCATION);
	}

	private Organizer organizer() {
		return (Organizer) vEvent.getProperties().getProperty(Property.ORGANIZER);
	}

	private Resources resources() {
		return (Resources) vEvent.getProperties().getProperty(Property.RESOURCES);
	}

	private Attendee attendee() {
		return (Attendee) vEvent.getProperties().getProperty(Property.ATTENDEE);
	}

	private Contact contact() {
		return (Contact) vEvent.getProperties().getProperty(Property.CONTACT);
	}

	private Comment comment() {
		return (Comment) vEvent.getProperties().getProperty(Property.COMMENT);
	}

	private Clazz clazz() {
		return (Clazz) vEvent.getProperties().getProperty(Property.CLASS);
	}

	private Status status() {
		return (Status) vEvent.getProperties().getProperty(Property.STATUS);
	}
}
