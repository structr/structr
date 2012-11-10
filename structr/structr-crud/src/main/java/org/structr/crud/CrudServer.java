/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.crud;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.jetty.servlet.ServletHolder;
import org.structr.crud.servlet.StructrCrudServlet;
import org.structr.server.Structr;
import org.structr.server.StructrServer;

/**
 *
 * @author Axel Morgner
 */
public class CrudServer implements StructrServer {

	public static void main(String[] args) {

		try {

			// CRUD Servlet
			StructrCrudServlet crudServlet = new StructrCrudServlet();
			ServletHolder crudServletHolder = new ServletHolder(crudServlet);
			Map<String, String> htmlInitParams = new HashMap<String, String>();

			htmlInitParams.put("Authenticator", "org.structr.web.auth.HttpAuthenticator");
			crudServletHolder.setInitParameters(htmlInitParams);


			Structr.createServer(CrudServer.class, "structr CRUD", 8182)
				
				.addServlet("/structr/crud/*", crudServletHolder)
			    
				.addResourceHandler("/structr", "src/main/resources/", false, new String[] { "index.html"})
			    
				.enableRewriteFilter()
				
				.start(true);

		} catch(Throwable t) {

			t.printStackTrace();
		}
	}
}
