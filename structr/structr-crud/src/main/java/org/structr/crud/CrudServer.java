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

import org.eclipse.jetty.servlet.ServletHolder;

import org.structr.common.PropertyView;
import org.structr.core.entity.AbstractNode;
import org.structr.rest.servlet.CsvServlet;
import org.structr.server.DefaultResourceProvider;
import org.structr.server.Structr;
import org.structr.server.StructrServer;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashMap;
import java.util.Map;
import org.structr.server.DefaultAuthenticator;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class CrudServer implements StructrServer {

	public static void main(String[] args) {

		try {

			CsvServlet structrRestServlet     = new CsvServlet(DefaultResourceProvider.class.newInstance(), PropertyView.All, AbstractNode.uuid);
			ServletHolder csvServletHolder    = new ServletHolder(structrRestServlet);
			Map<String, String> servletParams = new HashMap<String, String>();

			servletParams.put("PropertyFormat", "FlatNameValue");
			servletParams.put("Authenticator", DefaultAuthenticator.class.getName());
			csvServletHolder.setInitParameters(servletParams);
			csvServletHolder.setInitOrder(0);
			
			Structr.createServer(CrudServer.class, "structr CRUD", 8182)
				.addServlet("/structr/csv/*", csvServletHolder)
				.addResourceHandler("/structr", "src/main/resources/", false, new String[] { "index.html" })
				.enableRewriteFilter().start(true);

		} catch (Throwable t) {

			t.printStackTrace();

		}

	}

}
