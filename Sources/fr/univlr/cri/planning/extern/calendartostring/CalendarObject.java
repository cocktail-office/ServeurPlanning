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
package fr.univlr.cri.planning.extern.calendartostring;

import com.webobjects.foundation.NSMutableArray;

import fr.univlr.cri.planning.datacenter.SPVEvent;

/**
 * Code modifi� � partir de la classe PublishCalendarPage de Johan Carlberg,
 * version 1.0, 2002-09-30.
 * <p>
 */

public class CalendarObject extends Object {

	protected String calendarName;

	protected NSMutableArray events;

	public CalendarObject(String name) {
		super();
		calendarName = name;
		events = new NSMutableArray();
	}

	/**
	 * @deprecated
	 * @param event
	 */
	public void addEvent(SimpleEvent event) {
		events.addObject(event);
	}
	
	public void addSPVEvent(SPVEvent sPVEvent) {
		events.addObject(sPVEvent);
	}

	/**
	 * @deprecated
	 * @param event
	 */
	public void removeEvent(SimpleEvent event) {
		events.removeObject(event);
	}

	public NSMutableArray events() {
		return events;
	}

	public String calendarName() {
		return calendarName;
	}

}
