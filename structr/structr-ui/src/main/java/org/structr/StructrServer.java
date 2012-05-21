/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;

import org.eclipse.jetty.server.Connector;

/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;

import org.structr.context.ApplicationContextListener;
import org.structr.rest.servlet.JsonRestServlet;
import org.structr.web.servlet.HtmlServlet;
import org.structr.websocket.servlet.WebSocketServlet;

import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;

import java.util.*;

import javax.servlet.DispatcherType;

//~--- classes ----------------------------------------------------------------

/**
 * structr UI server
 *
 * @author Axel Morgner
 */
public class StructrServer {

	public static final String REST_URL = "/structr/rest";

	//~--- methods --------------------------------------------------------

	public static void main(String[] args) throws Exception {

		String appName        = "structr UI 0.5";
		String host           = System.getProperty("host", "0.0.0.0");
		String keyStorePath   = System.getProperty("keyStorePath", "keystore.jks");
		int httpPort          = Integer.parseInt(System.getProperty("port", "8082"));
		int httpsPort         = Integer.parseInt(System.getProperty("httpsPort", "8083"));
		int maxIdleTime       = Integer.parseInt(System.getProperty("maxIdleTime", "30000"));
		int requestHeaderSize = Integer.parseInt(System.getProperty("requestHeaderSize", "8192"));
		String contextPath    = System.getProperty("contextPath", "/");

		System.out.println();
		System.out.println("Starting " + appName + " (host=" + host + ":" + httpPort + ", maxIdleTime=" + maxIdleTime + ", requestHeaderSize=" + requestHeaderSize + ")");
		System.out.println();

		Server server                     = new Server(httpPort);
		HandlerCollection handlers        = new HandlerCollection();
		ContextHandlerCollection contexts = new ContextHandlerCollection();

		// setup HTTP connector
		SslContextFactory factory = new SslContextFactory(keyStorePath);

		factory.setKeyStorePassword("structrKeystore");

		SslSelectChannelConnector httpsConnector = new SslSelectChannelConnector(factory);

		httpsConnector.setHost(host);
		httpsConnector.setPort(httpsPort);
		httpsConnector.setMaxIdleTime(maxIdleTime);
		httpsConnector.setRequestHeaderSize(requestHeaderSize);

		// ServletContextHandler context0    = new ServletContextHandler(ServletContextHandler.SESSIONS);
		// SelectChannelConnector connector0 = new SelectChannelConnector();
		SelectChannelConnector httpConnector = new SelectChannelConnector();

		httpConnector.setHost(host);
		httpConnector.setPort(httpPort);
		httpConnector.setMaxIdleTime(maxIdleTime);
		httpConnector.setRequestHeaderSize(requestHeaderSize);

		String basePath = System.getProperty("home", "");
		File baseDir    = new File(basePath);

		basePath = baseDir.getAbsolutePath();

		// baseDir  = new File(basePath);
		System.out.println("Starting in directory " + basePath);

		String modulesPath = basePath + "/modules";
		File modulesDir    = new File(modulesPath);

		if (!modulesDir.exists()) {

			modulesDir.mkdir();
		}

		modulesPath = modulesDir.getAbsolutePath();

		File modulesConfFile     = new File(modulesPath + "/modules.conf");
		String warPath           = StructrServer.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		List<String> modulesConf = new LinkedList<String>();

		if (!(warPath.endsWith(".war"))) {

			File warFile = new File(warPath + "..");

			warPath = warFile.getAbsolutePath() + "/structr-ui.war";

		}

		WebAppContext webapp = new WebAppContext();

		webapp.setDescriptor(webapp + "/WEB-INF/web.xml");
		webapp.setTempDirectory(baseDir);
		webapp.setWar(warPath);
		System.out.println("Using WAR file " + warPath);

		// FilterHolder rewriteFilter =
		webapp.addFilter(UrlRewriteFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		// rewriteFilter.setInitParameter("logLevel", "DEBUG");
		// Strange behaviour of jetty:
		// If there's a directory with the same name like the WAR file in the same directory,
		// jetty will extract the classes into this directory and not into 'webapp'.
		// To avoid this, we will check for this directory and exit if it exists.
		String directoryName            = warPath.substring(0, (warPath.length() - 4));
		File directoryWhichMustNotExist = new File(directoryName);

		if (directoryWhichMustNotExist.exists()) {

			System.err.println("Directory " + directoryWhichMustNotExist + " must not exist.");

			// System.err.println("Delete it or move WAR file to another directory and start from there.");
			// System.exit(1);
			directoryWhichMustNotExist.delete();
			System.err.println("Deleted " + directoryWhichMustNotExist);

		}

		// search structr modules in WAR file
		ZipFile war                          = new ZipFile(new File(warPath));
		Enumeration<ZipArchiveEntry> entries = war.getEntries();

		while (entries.hasMoreElements()) {

			ZipArchiveEntry entry = entries.nextElement();
			String name           = entry.getName().substring(entry.getName().lastIndexOf("/") + 1);

			if (name.startsWith("structr") && name.endsWith(".jar")) {

				System.out.println("Found module " + name);
				modulesConf.add(name + "=active");

			}

		}

		war.close();

		// Create modules.conf if not existing
		if (!modulesConfFile.exists()) {

			modulesConfFile.createNewFile();
			FileUtils.writeLines(modulesConfFile, "UTF-8", modulesConf);

		}

		String confPath = basePath + "/structr.conf";
		File confFile   = new File(confPath);

		// Create structr.conf if not existing
		if (!confFile.exists()) {

			// synthesize a config file
			List<String> config = new LinkedList<String>();

			config.add("##################################");
			config.add("# structr global config file     #");
			config.add("##################################");
			config.add("# local title of running structr application");
			config.add("application.title = structr UI (" + host + ")");
			config.add("# base directory");
			config.add("base.path = " + basePath);
			config.add("# temp files directory");
			config.add("tmp.path = /tmp");
			config.add("# database files directory");
			config.add("database.path = " + basePath + "/db");
			config.add("# binary files directory");
			config.add("files.path = " + basePath + "/files");
			config.add("# modules directory");
			config.add("modules.path = " + basePath + "/modules");
			config.add("smtp.host = localhost");
			config.add("smtp.port = 25");
			config.add("superuser.username = superadmin");
			config.add("superuser.password = " + RandomStringUtils.randomAlphanumeric(12));    // Intentionally, no default password here
			config.add("configured.services = ModuleService NodeService AgentService");

			// don't start cron service without config file
			// config.add("configured.services = ModuleService NodeService AgentService CronService");
			// config.add("CronService.tasks = " + FeedCrawlerTask.class.getName());
//                      config.add(FeedCrawlerTask.class.getName() + ".cronExpression = 0 0 * * * *");
			confFile.createNewFile();
			FileUtils.writeLines(confFile, "UTF-8", config);
		}

		webapp.setInitParameter("configfile.path", confFile.getAbsolutePath());
		webapp.setContextPath(contextPath);
		webapp.setParentLoaderPriority(true);

		// JSON REST Servlet
		JsonRestServlet structrRestServlet = new JsonRestServlet();
		ServletHolder holder               = new ServletHolder(structrRestServlet);
		Map<String, String> initParams     = new HashMap<String, String>();

		initParams.put("RequestLogging", "true");
		initParams.put("PropertyFormat", "FlatNameValue");
		initParams.put("ResourceProvider", "org.structr.web.common.UiResourceProvider");

//              initParams.put("ResourceProvider", "org.structr.rest.resource.StructrResourceProvider");
		initParams.put("Authenticator", "org.structr.web.auth.UiAuthenticator");
		initParams.put("IdProperty", "uuid");
		holder.setInitParameters(initParams);
		holder.setInitOrder(2);
		webapp.addServlet(holder, REST_URL + "/*");

		// HTML Servlet
		HtmlServlet htmlServlet            = new HtmlServlet();
		ServletHolder htmlServletHolder    = new ServletHolder(htmlServlet);
		Map<String, String> htmlInitParams = new HashMap<String, String>();

		htmlInitParams.put("Authenticator", "org.structr.web.auth.HttpAuthenticator");
		htmlInitParams.put("IdProperty", "uuid");
		htmlServletHolder.setInitParameters(htmlInitParams);
		htmlServletHolder.setInitOrder(3);
		webapp.addServlet(htmlServletHolder, "/structr/html/*");

		// WebSocket Servlet
		WebSocketServlet wsServlet       = new WebSocketServlet();
		ServletHolder wsServletHolder    = new ServletHolder(wsServlet);
		Map<String, String> wsInitParams = new HashMap<String, String>();

		wsInitParams.put("Authenticator", "org.structr.web.auth.UiAuthenticator");
		wsInitParams.put("IdProperty", "uuid");
		wsServletHolder.setInitParameters(wsInitParams);
		wsServletHolder.setInitOrder(4);
		webapp.addServlet(wsServletHolder, "/structr/ws/*");
		webapp.addEventListener(new ApplicationContextListener());

		// enable request logging
		RequestLogHandler requestLogHandler = new RequestLogHandler();
		String logPath                      = basePath + "/logs";
		File logDir                         = new File(logPath);

		// Create logs directory if not existing
		if (!logDir.exists()) {

			logDir.mkdir();
		}

		logPath = logDir.getAbsolutePath();

		NCSARequestLog requestLog = new NCSARequestLog(logPath + "/structr-yyyy_mm_dd.request.log");

		requestLog.setRetainDays(90);
		requestLog.setAppend(true);
		requestLog.setExtended(false);
		requestLog.setLogTimeZone("GMT");
		requestLogHandler.setRequestLog(requestLog);
		contexts.setHandlers(new Handler[] { webapp, requestLogHandler });
		handlers.setHandlers(new Handler[] { contexts, new DefaultHandler(), requestLogHandler });
		server.setHandler(handlers);
		server.setConnectors(new Connector[] { httpConnector, httpsConnector });
		server.setGracefulShutdown(1000);
		server.setStopAtShutdown(true);
		System.out.println();
		System.out.println("structr UI:        http://" + host + ":" + httpPort + contextPath + "structr/");
		System.out.println();
		server.start();
		server.join();
		System.out.println(appName + " stopped.");

	}

}
