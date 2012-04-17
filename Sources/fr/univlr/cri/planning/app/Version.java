/*
 * Copyright Universit� de La Rochelle 2008
 *
 * Ce logiciel est un programme informatique permettant la communication 
 * entre applications de gestion de plannings.
 * 
 * Ce logiciel est r�gi par la licence CeCILL soumise au droit fran�ais et
 * respectant les principes de diffusion des logiciels libres. Vous pouvez
 * utiliser, modifier et/ou redistribuer ce programme sous les conditions
 * de la licence CeCILL telle que diffus�e par le CEA, le CNRS et l'INRIA 
 * sur le site "http://www.cecill.info".

 * En contrepartie de l'accessibilit� au code source et des droits de copie,
 * de modification et de redistribution accord�s par cette licence, il n'est
 * offert aux utilisateurs qu'une garantie limit�e.  Pour les m�mes raisons,
 * seule une responsabilit� restreinte p�se sur l'auteur du programme,  le
 * titulaire des droits patrimoniaux et les conc�dants successifs.

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

 * Le fait que vous puissiez acc�der � cet en-t�te signifie que vous avez 
 * pris connaissance de la licence CeCILL, et que vous en avez accept� les
 * termes.
 */
package fr.univlr.cri.planning.app;

import java.util.StringTokenizer;

import org.cocktail.fwkcktlwebapp.server.version.A_CktlVersion;

/**
 * Classe descriptive de la version et dependances de l'application
 * ServeurPlanning
 * 
 * @author ctarade
 */
public class Version extends A_CktlVersion {

	// nom de l'application
	public String name() {
		return "ServeurPlanning";
	}

	// date de publication
	public String date() {
		return "06/02/2012";
	}

	// numéro majeur
	public int versionNumMaj() {
		return 0;
	}

	// numéro mineur
	public int versionNumMin() {
		return 8;
	}

	// numéro de patch
	public int versionNumPatch() {
		return 2;
	}

	// numéro de build
	public int versionNumBuild() {
		return 4;
	}

	// commentaire
	public String comment() {
		return "";
	}

	// liste des dependances
	public CktlVersionRequirements[] dependencies() {
		return new CktlVersionRequirements[] {
				new CktlVersionRequirements(new VersionJakartaSlideWebdavlib(), "2", "2", false),
				new CktlVersionRequirements(new VersionIcal4j(), null, null, false),
				new CktlVersionRequirements(new VersionCommonHttpClient(), "2", "2", false),
				new CktlVersionRequirements(new VersionCommonLogging(), "1", "1", false),
				/*
				 * new CktlVersionRequirements(new CktlVersionServeurPlanningFwk(),
				 * "0.8.0.1", null, true),
				 */
				new CktlVersionRequirements(new org.cocktail.fwkcktlwebapp.server.version.Version(), "4.0.15", null, true),
				new CktlVersionRequirements(new org.cocktail.fwkcktlwebapp.server.version.CktlVersionWebObjects(), "5.2.4", null, false),
				new CktlVersionRequirements(new org.cocktail.fwkcktlwebapp.server.version.CktlVersionJava(), "1.4.2", "1.6", false) };
	}

	/**
	 * Classe de lecture de la version de jakarta-slide-webdavlib
	 * 
	 * @author Cyril Tarade <cyril.tarade at univ-lr.fr>
	 */
	private class VersionJakartaSlideWebdavlib extends CktlVersionJar {

		private static final String WEBDAV_CLASS_NAME = "org.apache.webdav.lib.WebdavResource";

		public VersionJakartaSlideWebdavlib() {
			super(WEBDAV_CLASS_NAME);
		}
	}

	/**
	 * Classe de lecture de la version de ical4j
	 * 
	 * @author Cyril Tarade <cyril.tarade at univ-lr.fr>
	 */
	private class VersionIcal4j extends CktlVersionJar {

		private static final String ICAL4J_CLASS_NAME = "net.fortuna.ical4j.model.property.Version";

		public VersionIcal4j() {
			super(ICAL4J_CLASS_NAME);
		}
	}

	/**
	 * Classe de lecture de la version de common http client
	 * 
	 * @author Cyril Tarade <cyril.tarade at univ-lr.fr>
	 */
	private class VersionCommonHttpClient extends CktlVersionJar {

		private static final String COMMON_HTTP_CLIENT_CLASS_NAME = "org.apache.commons.httpclient.HttpClient";

		public VersionCommonHttpClient() {
			super(COMMON_HTTP_CLIENT_CLASS_NAME);
		}
	}

	/**
	 * Classe de lecture de la version de common logging
	 * 
	 * @author Cyril Tarade <cyril.tarade at univ-lr.fr>
	 */
	private class VersionCommonLogging extends CktlVersionJar {

		private static final String COMMON_LOGGING_CLASS_NAME = "org.apache.commons.logging.Log";

		public VersionCommonLogging() {
			super(COMMON_LOGGING_CLASS_NAME);
		}
	}

	/**
	 * Classe a reintegrer dans CRIWebApp, afin qu'il soit possible d'utiliser
	 * plusieurs fois la classe (virer les static), et lire les formats de version
	 * en 0-rc3
	 * 
	 * @author ctarade
	 */
	private abstract class CktlVersionJar extends A_CktlVersion {

		private String fqdnClassName;

		public CktlVersionJar(String aFqdnClassName) {
			super();
			fqdnClassName = aFqdnClassName;
			try {
				localReadVersionNumber();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public CktlVersionRequirements[] dependencies() {
			return null;
		}

		public String name() {
			return "Jar \"" + fqdnClassName + "\"";
		}

		public int versionNumMaj() {
			return versionNumMaj;
		}

		public int versionNumMin() {
			return versionNumMin;
		}

		public int versionNumPatch() {
			return versionNumPatch;
		}

		public int versionNumBuild() {
			return versionNumBuild;
		}

		private int versionNumMaj;
		private int versionNumMin;
		private int versionNumPatch;
		private int versionNumBuild;

		// la lecture reelle des parametres, qui a que fqdnClassName soit renseignee
		private void localReadVersionNumber() throws ClassNotFoundException {
			final Package jarPackage = Class.forName(fqdnClassName).getPackage();
			final String jarVersion = jarPackage.getImplementationVersion();
			if (jarVersion != null) {
				StringTokenizer tk = new StringTokenizer(jarVersion, "._-rc");
				if (tk.hasMoreTokens()) {
					versionNumMaj = Integer.parseInt(tk.nextToken());
				}
				if (tk.hasMoreTokens()) {
					versionNumMin = Integer.parseInt(tk.nextToken());
				}
				if (tk.hasMoreTokens()) {
					versionNumPatch = Integer.parseInt(tk.nextToken());
				}
				if (tk.hasMoreTokens()) {
					versionNumBuild = Integer.parseInt(tk.nextToken());
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.cocktail.fwkcktlwebapp.server.version.A_CktlVersion#readVersionNumber
		 * ()
		 */
		public void readVersionNumber() throws Exception {

		}
	}
}
