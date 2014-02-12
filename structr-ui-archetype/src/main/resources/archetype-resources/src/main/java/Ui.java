/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import java.util.HashMap;
import java.util.Map;
import org.eclipse.jetty.servlet.ServletHolder;
import org.structr.common.PropertyView;
import org.structr.core.entity.AbstractNode;
import org.structr.rest.servlet.CsvServlet;
import org.structr.server.DefaultResourceProvider;
import org.structr.server.StructrServer;
import org.structr.server.Structr;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.common.UiResourceProvider;
import org.structr.web.servlet.HtmlServlet;
import org.structr.websocket.servlet.WebSocketServlet;

public class Ui implements StructrServer {

        public static void main(String[] args) {

		try {

			// HTML Servlet
			HtmlServlet htmlServlet = new HtmlServlet();
			ServletHolder htmlServletHolder = new ServletHolder(htmlServlet);
			Map<String, String> htmlInitParams = new HashMap<String, String>();

			htmlInitParams.put("Authenticator", "org.structr.web.auth.HttpAuthenticator");
			htmlServletHolder.setInitParameters(htmlInitParams);
			htmlServletHolder.setInitOrder(1);
			
			// CSV Servlet
			CsvServlet csvServlet     = new CsvServlet(DefaultResourceProvider.class.newInstance(), PropertyView.All, AbstractNode.uuid);
			ServletHolder csvServletHolder    = new ServletHolder(csvServlet);
			Map<String, String> servletParams = new HashMap<String, String>();

			servletParams.put("Authenticator", "org.structr.web.auth.HttpAuthenticator");
			csvServletHolder.setInitParameters(servletParams);
			csvServletHolder.setInitOrder(2);

			// WebSocket Servlet
			WebSocketServlet wsServlet = new WebSocketServlet(AbstractNode.uuid);
			ServletHolder wsServletHolder = new ServletHolder(wsServlet);
			Map<String, String> wsInitParams = new HashMap<String, String>();

			wsInitParams.put("Authenticator", "org.structr.web.auth.UiAuthenticator");
			wsInitParams.put("IdProperty", "uuid");
			wsServletHolder.setInitParameters(wsInitParams);
			wsServletHolder.setInitOrder(3);

			Structr.createServer(Ui.class, "${artifactId} ${version}", 8082)
				
				.addServlet("/structr/html/*", htmlServletHolder)
				.addServlet("/structr/ws/*", wsServletHolder)
				.addServlet("/structr/csv/*", csvServletHolder)
			    
				.addResourceHandler("/structr", "target/structr", true, new String[] { "index.html"})
			    
				.enableRewriteFilter()
				//.logRequests(true)
				
				.resourceProvider(UiResourceProvider.class)
				.authenticator(UiAuthenticator.class)
				
			    
				.start(true);

		} catch(Throwable t) {

			t.printStackTrace();
		}
        }
}
