/*
 * Copyright Consortium Coktail, 12 d�c. 06
 * 
 * cyril.tarade at univ-lr.fr
 * 
 * Ce logiciel est un programme informatique servant � [rappeler les
 * caract�ristiques techniques de votre logiciel]. 
 * 
 * Ce logiciel est r�gi par la licence CeCILL soumise au droit fran�ais et
 * respectant les principes de diffusion des logiciels libres. Vous pouvez
 * utiliser, modifier et/ou redistribuer ce programme sous les conditions
 * de la licence CeCILL telle que diffus�e par le CEA, le CNRS et l'INRIA 
 * sur le site "http://www.cecill.info".
 * 
 * En contrepartie de l'accessibilit� au code source et des droits de copie,
 * de modification et de redistribution accord�s par cette licence, il n'est
 * offert aux utilisateurs qu'une garantie limit�e.  Pour les m�mes raisons,
 * seule une responsabilit� restreinte p�se sur l'auteur du programme,  le
 * titulaire des droits patrimoniaux et les conc�dants successifs.
 * 
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
 * 
 * Le fait que vous puissiez acc�der � cet en-t�te signifie que vous avez 
 * pris connaissance de la licence CeCILL, et que vous en avez accept� les
 * termes.
 */

package fr.univlr.cri.planning.thread;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.cocktail.fwkcktlwebapp.common.CktlLog;
import org.cocktail.fwkcktlwebapp.common.util.StringCtrl;

import com.webobjects.foundation.NSArray;


/**
 * Classe permettant de lancer une requete HTTP en 
 * ayant la main sur les timeout. 
 * 
 * @author Cyril Tarade <cyril.tarade at univ-lr.fr>
 */
public class SPHTTPConnection {

	
	/**
	 * La machine proxy
	 */
	private static String proxyHost;

	/**
	 * Le port affecte a la machine proxy
	 */
	private static int proxyPort;
	
	/**
	 * La liste des adresses pour lesquelles il ne faut pas utiliser
	 * le proxy
	 */
	private static NSArray listNonProxyHosts;
	
	private final static String SEP_NON_PROXY_HOSTS = "|";
	private final static String PREFIX_HOST = "http://";
	private final static String SUFFIX_HOST_1 = "/";
	private final static String SUFFIX_HOST_2 = ":";
	
	/**
	 * L'adresse de l'URL a contacter
	 */
	private String serviceAddress;
	
	/**
	 * Le duree maximum de reponse autorisee pour avoir la
	 * reponse de l'URL a contacter (en ms)
	 */
	private static int timeout;
	
	public SPHTTPConnection(String aServiceAddress) {
		super();
		serviceAddress = aServiceAddress;
	}
	
	/**
	 * 
	 * @param aTimeout
	 * @param aProxyHost
	 * @param aStrProxyPort
	 * @param aStrNonProxyHosts
	 */
	public static void initStaticFields(int aTimeout, String aProxyHost, String aStrProxyPort, String aStrNonProxyHosts) {
		timeout = aTimeout;
		if (!StringCtrl.isEmpty(aProxyHost)) {
			proxyHost = aProxyHost;
			if (!StringCtrl.isEmpty(aStrProxyPort)) {
				proxyPort = Integer.parseInt(aStrProxyPort);
				if (!StringCtrl.isEmpty(aStrNonProxyHosts)) {
					listNonProxyHosts = NSArray.componentsSeparatedByString(aStrNonProxyHosts, SEP_NON_PROXY_HOSTS);
				}
			}
		} else {
			proxyHost = StringCtrl.emptyString();
			proxyPort = -1;
		}
	}
	
	/**
	 * Effectue la connection a l'url <code>serviceAddress</code>
	 * et le retourne la contenu de la reponse au format <code>String</code>.
	 * 
	 * Si le timeout est depasse, cette methode retourne <code>null<code>
	 */
	public String connect() {
		long l = System.currentTimeMillis();
		//boolean connectionWasReleased = false;
		HttpClient client = new HttpClient();
		// parametrer le proxy si necessaire
		if (!StringCtrl.isEmpty(proxyHost) && !isNonProxyHosts(serviceAddress)) {
			client.getHostConfiguration().setProxy(proxyHost, proxyPort);
		}
		GetMethod httpget = new GetMethod(serviceAddress);
		httpget.setFollowRedirects( true );
		client.setTimeout(timeout);
		
		String content = null;
		try {
			// Execute the GET method
			int statusCode = client.executeMethod( httpget );
			
			if( statusCode != -1 ) {

				byte[] responseBody = httpget.getResponseBody();
				content = new String(responseBody, httpget.getResponseCharSet());
				
			}
	  }
	  catch( Exception e ) {
	  	log("connection timeout : "+(System.currentTimeMillis()-l)+ " s");
	  }
	  finally {
	  	httpget.releaseConnection();
	  }
	  
		return content;
	}
	
	/**
	 * Indique si l'adresse passee doit etre atteinte par le
	 * proxy ou pas (<code>listNonProxyHosts</code>) 
	 * @param aServiceAddress
	 * @return
	 */
	private boolean isNonProxyHosts(String aServiceAddress) {
		boolean result = false;
		// verifier que l'adresse est bien du type http://
		if (!StringCtrl.isEmpty(aServiceAddress) && aServiceAddress.startsWith(PREFIX_HOST)) {
			// ne conserver que le host
			String hostName = aServiceAddress.substring(PREFIX_HOST.length(), aServiceAddress.length());
			// suppression de ce qu'il y a apres le /
			hostName = (hostName.indexOf(SUFFIX_HOST_1) != -1 ? hostName.substring(0, hostName.indexOf(SUFFIX_HOST_1)) : hostName);
			// suppression eventuelle du port 
			hostName = (hostName.indexOf(SUFFIX_HOST_2) != -1 ? hostName.substring(0, hostName.indexOf(SUFFIX_HOST_2)) : hostName);
			for(int i=0; !result && i<listNonProxyHosts.count(); i++) {
				String nonProxyHost = (String) listNonProxyHosts.objectAtIndex(i);
				// un simple like suffit
				if (StringCtrl.like(hostName, nonProxyHost)) {
					result = true;
				}
			}
		}
		return result;
	}
	
	/**
	 * 
	 * @param message
	 */
	private void log(String message) {
		CktlLog.log(message);
	}
}
