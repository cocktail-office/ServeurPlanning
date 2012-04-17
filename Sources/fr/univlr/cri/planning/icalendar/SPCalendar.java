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
package fr.univlr.cri.planning.icalendar;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.model.property.XProperty;

import org.cocktail.fwkcktlwebapp.common.util.StringCtrl;

import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimeZone;

import fr.univlr.cri.planning.SPOccupation;
import fr.univlr.cri.planning.datacenter.ParamBus;

/**
 * Surcharge du calendrier ical4j pour les besoin du serveur de plannings. Un
 * certain nombre de methodes utilitaires sont ajoutees.
 * 
 * @author ctarade
 */

public class SPCalendar
		extends Calendar {

	// les noms des proprietes
	private static final String CALENDAR_NAME = Property.EXPERIMENTAL_PREFIX + "WR-CALNAME";
	private static final String CALENDAR_DESCRIPTION = Property.EXPERIMENTAL_PREFIX + "WR-CALDESC";
	private static final String CALENDAR_TIMEZONE = Property.EXPERIMENTAL_PREFIX + "WR-TIMEZONE";

	// les constantes
	private static String spProdId;

	// l'identifiant unique du calendrier
	private String uid;

	// le numero de l'occupation en cours
	private static int noOcc;

	// repreciser le TZ pour toutes les events - en model singleton
	// private static ParameterList paramsDate;

	// la liste des occupations : tableau de <code>VEvent</code>
	private final List<VEvent> events;

	// le bus pour effectuer des fetch sur le libelle de type d'occupation
	private ParamBus paramBus;

	// le dico contenant les associations typeOccupation <-> libelle
	private static NSMutableDictionary<String, String> theDicoLibelle;

	private String calendarName;

	public SPCalendar(String anUid, ParamBus aParamBus, String aCalendarName) {
		super();
		uid = anUid;
		calendarName = aCalendarName;
		events = new ArrayList<VEvent>();
		paramBus = aParamBus;
		initCalendar();
	}

	public static void initStaticFields(String appVer) {
		spProdId = "-//Serveur de planning Cocktail " + appVer + "//iCal4j 1.0";
	}

	/**
	 * Parametrage initial du calendrier
	 */
	private void initCalendar() {
		getProperties().add(new ProdId(spProdId));
		getProperties().add(Version.VERSION_2_0);
		getProperties().add(CalScale.GREGORIAN);
		getProperties().add(Method.PUBLISH);
		setCalendarName(calendarName);
		//
		setTimeZone(NSTimeZone.defaultTimeZone().getID());
		noOcc = 1;
	}

	/**
	 * Ajouter une occupation
	 */
	private void addSPOccupation(SPOccupation occ) {

		DateTime dateTimeDebut = new DateTime(occ.getDateDebut().getTime());
		DateTime dateTimeFin = new DateTime(occ.getDateFin().getTime());

		Date dateDebut = new Date(occ.getDateDebut().getTime());
		Date dateFin = new Date(occ.getDateFin().timestampByAddingGregorianUnits(0, 0, 1, 0, 0, 0).getTime());

		// si le debut est a 00:00 et la fin a 23:59, ou minuit de j+1
		// alors on met la date a null
		// on utilise alors un objet Date
		GregorianCalendar gcDebut = new GregorianCalendar();
		gcDebut.setTime(occ.getDateDebut());
		GregorianCalendar gcFin = new GregorianCalendar();
		gcFin.setTime(occ.getDateFin());

		boolean isFullDay = false;

		if (gcDebut.get(GregorianCalendar.HOUR_OF_DAY) == 0 && gcDebut.get(GregorianCalendar.MINUTE) == 0) {
			dateDebut = new Date(occ.getDateDebut().getTime());
			if (gcFin.get(GregorianCalendar.HOUR_OF_DAY) == 23 && gcFin.get(GregorianCalendar.MINUTE) == 59) {
				isFullDay = true;
			} else if (gcFin.get(GregorianCalendar.HOUR_OF_DAY) == 0 && gcFin.get(GregorianCalendar.MINUTE) == 0) {
				isFullDay = true;
				dateFin = new Date(occ.getDateFin());
			}
		}

		// on essaye de determiner le libelle complet du type
		String typeTemps = libelleForTypeOcc(occ.getTypeTemps());
		if (StringCtrl.isEmpty(typeTemps)) {
			typeTemps = occ.getTypeTemps();
		}

		// on affiche detail + le type
		String detail = "";
		if (!StringCtrl.isEmpty(typeTemps)) {
			detail = "[" + typeTemps + "]";
		}
		if (!StringCtrl.isEmpty(occ.getDetailsTemps()))
			detail = occ.getDetailsTemps() + " " + detail;

		final VEvent event = new VEvent();
		event.getProperties().add(new Uid(uid + "-" + event.hashCode() + "-" + (noOcc++)));

		DtStart dtStart = null;
		if (isFullDay) {
			dtStart = new DtStart(dateDebut);
			// dtStart.getParameters().add(Value.DATE);
		} else {
			dtStart = new DtStart(dateTimeDebut, true);
		}
		event.getProperties().add(dtStart);

		DtEnd dtEnd = null;
		if (isFullDay) {
			dtEnd = new DtEnd(dateFin);
			// dtEnd.getParameters().add(Value.DATE);
		} else {
			dtEnd = new DtEnd(dateTimeFin, true);
		}
		event.getProperties().add(dtEnd);

		event.getProperties().add(new Summary(detail));
		event.getProperties().add(new Description(detail));
		events.add(event);

		try {
			event.validate();
		} catch (ValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Ajouter plusieurs occupations
	 */
	public void addSPOccupations(NSArray<SPOccupation> arrayOcc) {
		for (int i = 0; i < arrayOcc.count(); i++) {
			SPOccupation occ = arrayOcc.objectAtIndex(i);
			addSPOccupation(occ);
		}
	}

	/**
	 * Methode qui permet d'inserer les evenements dans le calendrier, ajoutes
	 * precedement via addSPOccupation() ou addSPOccupations()
	 */
	public void commitEvents() {
		getComponents().addAll(events);
	}

	/**
	 * Le nom affiche pour le calendrier (marche sous ICAL pour mac)
	 */
	public void setCalendarName(String value) {
		setValueForKey(value, CALENDAR_NAME);
	}

	/**
   * 
   */
	public void setDescription(String value) {
		setValueForKey(value, CALENDAR_DESCRIPTION);
	}

	/**
   * 
   */
	public void setTimeZone(String value) {
		setValueForKey(value, CALENDAR_TIMEZONE);
	}

	/**
	 * Modifier une propriete du calendrier
	 */
	private void setValueForKey(String value, String key) {
		XProperty nameProp = (XProperty) getProperties().getProperty(key);
		if (nameProp == null) {
			nameProp = new XProperty(key, value);
			getProperties().add(nameProp);
		} else {
			nameProp.setValue(value);
		}
	}

	/**
	 * L'instance du dico des noms des types d'occupations selon le modele
	 * singleton
	 */
	private NSMutableDictionary<String, String> dicoLibelle() {
		if (theDicoLibelle == null)
			theDicoLibelle = new NSMutableDictionary<String, String>();
		return theDicoLibelle;
	}

	/**
	 * Determiner le libelle d'un type d'occupation. Fonctionne avec et actualise
	 * le dictionaire de cache dicoLibelle()
	 * 
	 * @param value
	 *          : le code du type temps
	 */
	private String libelleForTypeOcc(String value) {
		String libelle = (String) dicoLibelle().valueForKey(value);
		if (libelle == null) {
			libelle = paramBus.fetchLibelleForTypeTemp(value);
			if (libelle == null)
				libelle = "";
			dicoLibelle().setObjectForKey(libelle, value);
		}
		return libelle;
	}
}
