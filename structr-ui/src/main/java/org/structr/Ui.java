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
package org.structr;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.PropertyView;
import org.structr.common.StructrConf;
import org.structr.core.Services;
import org.structr.module.JarConfigurationProvider;
import org.structr.rest.service.HttpService;
import org.structr.rest.servlet.CsvServlet;
import org.structr.rest.servlet.JsonRestServlet;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.common.UiResourceProvider;
import org.structr.web.entity.User;
import org.structr.web.servlet.HtmlServlet;
import org.structr.web.servlet.UploadServlet;
import org.structr.websocket.servlet.WebSocketServlet;

/**
 *
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class Ui {

	public static void main(String[] args) {

		try {

			final StructrConf config = Services.getBaseConfiguration();
			
			config.setProperty(Services.BASE_PATH, "./");
			config.setProperty(Services.CONFIGURED_SERVICES, "NodeService LogService FtpService HttpService");
			config.setProperty(Services.CONFIGURATION, JarConfigurationProvider.class.getName());
			config.setProperty(HttpService.APPLICATION_TITLE, "Structr UI");

			// Add this class to config so HttpService can derive the jar file containing the static resources from it
			config.setProperty(HttpService.MAIN_CLASS, Ui.class.getName());

			// Configure servlets
			config.setProperty(HttpService.SERVLETS, "JsonRestServlet WebSocketServlet CsvServlet HtmlServlet");
			
			config.setProperty("JsonRestServlet.class", JsonRestServlet.class.getName());
			config.setProperty("JsonRestServlet.path", "/structr/rest/*");
			config.setProperty("JsonRestServlet.resourceprovider", UiResourceProvider.class.getName());
			config.setProperty("JsonRestServlet.authenticator", UiAuthenticator.class.getName());
			config.setProperty("JsonRestServlet.user.class", User.class.getName());
			config.setProperty("JsonRestServlet.user.autocreate", "false");
			config.setProperty("JsonRestServlet.defaultview", PropertyView.Public);
			config.setProperty("JsonRestServlet.outputdepth", "3");
			
			config.setProperty("WebSocketServlet.class", WebSocketServlet.class.getName());
			config.setProperty("WebSocketServlet.path", "/structr/ws/*");
			config.setProperty("WebSocketServlet.resourceprovider", UiResourceProvider.class.getName());
			config.setProperty("WebSocketServlet.authenticator", UiAuthenticator.class.getName());
			config.setProperty("WebSocketServlet.user.class", User.class.getName());
			config.setProperty("WebSocketServlet.user.autocreate", "false");
			config.setProperty("WebSocketServlet.defaultview", PropertyView.Public);

			config.setProperty("CsvServlet.class", CsvServlet.class.getName());
			config.setProperty("CsvServlet.path", "/structr/csv/*");
			config.setProperty("CsvServlet.resourceprovider", UiResourceProvider.class.getName());
			config.setProperty("CsvServlet.authenticator", UiAuthenticator.class.getName());
			config.setProperty("CsvServlet.user.class", User.class.getName());
			config.setProperty("CsvServlet.user.autocreate", "false");
			config.setProperty("CsvServlet.defaultview", PropertyView.Public);
			config.setProperty("CsvServlet.outputdepth", "3");

			config.setProperty("UploadServlet.class", UploadServlet.class.getName());
			config.setProperty("UploadServlet.path", "/structr/upload");
			config.setProperty("UploadServlet.resourceprovider", UiResourceProvider.class.getName());
			config.setProperty("UploadServlet.authenticator", UiAuthenticator.class.getName());
			config.setProperty("UploadServlet.user.class", User.class.getName());
			config.setProperty("UploadServlet.user.autocreate", "false");
			config.setProperty("UploadServlet.defaultview", PropertyView.Public);
			config.setProperty("UploadServlet.outputdepth", "3");

			config.setProperty("HtmlServlet.class", HtmlServlet.class.getName());
			config.setProperty("HtmlServlet.path", "/structr/html/*");
			config.setProperty("HtmlServlet.resourceprovider", UiResourceProvider.class.getName());
			config.setProperty("HtmlServlet.authenticator", UiAuthenticator.class.getName());
			config.setProperty("HtmlServlet.user.class", User.class.getName());
			config.setProperty("HtmlServlet.user.autocreate", "false");
			config.setProperty("HtmlServlet.defaultview", PropertyView.Public);
			config.setProperty("HtmlServlet.outputdepth", "3");

			// Configure resource handlers
			config.setProperty(HttpService.RESOURCE_HANDLERS, "StructrUiHandler");
			
			config.setProperty("StructrUiHandler.contextPath", "/structr");
			config.setProperty("StructrUiHandler.resourceBase", "src/main/resources/structr");
			config.setProperty("StructrUiHandler.directoriesListed", Boolean.toString(false));
			config.setProperty("StructrUiHandler.welcomeFiles", "index.html");

			// let structr.conf override defaults
			// read structr.conf
			final String configFileName = "structr.conf";
			
			try {
				final FileInputStream fis    = new FileInputStream(configFileName);
				final Properties structrConf = new Properties();
				structrConf.load(fis);
				fis.close();
				
				Logger.getLogger(Ui.class.getName()).log(Level.INFO, "Read {0} entries from {1}", new Object[] { structrConf.size(), configFileName });
				
				config.putAll(structrConf);

			} catch (IOException ignore) { }
			
			
			final Services services = Services.getInstance(config);

			// wait for service layer to be initialized
			do {
				try { Thread.sleep(100); } catch (Throwable t) {}

			} while (!services.isInitialized());

		} catch(Throwable t) {

			t.printStackTrace();
		}
	}
}
