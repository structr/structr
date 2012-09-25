package org.structr;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.jetty.servlet.ServletHolder;
import org.structr.server.Structr;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.common.UiResourceProvider;
import org.structr.web.servlet.HtmlServlet;
import org.structr.websocket.servlet.WebSocketServlet;

/**
 *
 * @author Christian Morgner
 */
public class Ui implements org.structr.server.StructrServer {

	public static void main(String[] args) {

		try {

			// HTML Servlet
			HtmlServlet htmlServlet = new HtmlServlet();
			ServletHolder htmlServletHolder = new ServletHolder(htmlServlet);
			Map<String, String> htmlInitParams = new HashMap<String, String>();

			htmlInitParams.put("Authenticator", "org.structr.web.auth.HttpAuthenticator");
			htmlInitParams.put("IdProperty", "uuid");
			htmlServletHolder.setInitParameters(htmlInitParams);

			// WebSocket Servlet
			WebSocketServlet wsServlet = new WebSocketServlet();
			ServletHolder wsServletHolder = new ServletHolder(wsServlet);
			Map<String, String> wsInitParams = new HashMap<String, String>();

			wsInitParams.put("Authenticator", "org.structr.web.auth.UiAuthenticator");
			wsInitParams.put("IdProperty", "uuid");
			wsServletHolder.setInitParameters(wsInitParams);

			Structr.createServer(Ui.class, "structr UI", 8082)
				
				.addServlet("/structr/html/*", htmlServletHolder)
				.addServlet("/structr/ws/*", wsServletHolder)
			    
				.addResourceHandler("src/main/resources", true, new String[] { "index.html"})
			    
				.enableRewriteFilter()
				
				.resourceProvider(UiResourceProvider.class)
				.authenticator(UiAuthenticator.class)
				
			    
				.start(true);

		} catch(Throwable t) {

			t.printStackTrace();
		}
	}
}
