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
package fr.univlr.cri.planning.components;

import org.cocktail.fwkcktlwebapp.server.components.CktlWebComponent;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.eocontrol.EOEditingContext;

import fr.univlr.cri.planning.Application;
import fr.univlr.cri.planning.Session;

/**
 * Classe regroupant toutes les méthodes communes a un composant. 
 * Idéalement, tous les composants doivent héritant directement ou
 * indirectement de cette classe.
 * 
 * @author Cyril Tarade <cyril.tarade at univ-lr.fr>
 */

public class SPComponent extends CktlWebComponent {
	/**
	 * Pointeur vers l'instance de la session
	 */
	private Session _session;

	/**
	 * Pointeur vers l'instance de l'application.
	 */
	private Application _app;

	/**
	 * Pointeur vers le EOEditingContext global.
	 */
	private EOEditingContext _ec;
	
	/**
	 * L'ancre a laquelle se rendre au prochain rechargement 
	 * de la page
	 */
	public String targetPosition;
	
	public SPComponent(WOContext context) {
		super(context);
		initObject();
	}

	/**
	 * Initialisation des variables communes
	 */
	private void initObject() {
		_session = (Session)session();
		_ec = _session.defaultEditingContext();
		_app = (Application)application();
	}
	
	// accesseurs
	
	protected EOEditingContext ec() {
		return _ec;
	}
	
	protected Session spSession() {
		return _session;
	}
	
	public Application spApp() {
		return _app;
	}

	/**
	 * Surcharge du setter pour ignorer tout �crasement
	 * venu des conteneurs
	 */
	public void setTargetPosition(String value) {
	}
    
	/**
	 * M�thode par d�faut qui permet de poster les
	 * donn�es du formulaire
	 */
	public WOComponent doNothing() {
		return null;
	}
}
