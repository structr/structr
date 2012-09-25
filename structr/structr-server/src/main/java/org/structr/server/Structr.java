/*
 *  Copyright (C) 2012 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.server;

import ch.qos.logback.access.jetty.RequestLogImpl;
import ch.qos.logback.access.servlet.TeeFilter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.DispatcherType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.structr.context.ApplicationContextListener;
import org.structr.core.Service;
import org.structr.core.agent.AgentService;
import org.structr.core.auth.Authenticator;
import org.structr.core.cron.CronService;
import org.structr.core.log.LogService;
import org.structr.core.module.ModuleService;
import org.structr.core.node.NodeService;
import org.structr.rest.ResourceProvider;
import org.structr.rest.servlet.JsonRestServlet;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

/**
 *
 * @author Christian Morgner
 */
public class Structr {
	
	private static final Logger logger          = Logger.getLogger(Structr.class.getName());
	
	private String applicationName              = "structr server";
	private String restUrl                      = "/structr/rest";
	
	private String host                         = "0.0.0.0";
	private String keyStorePath                 = null;
	private String keyStorePassword             = null;
	private String contextPath                  = System.getProperty("contextPath", "/");
	private String basePath                     = "";

	private int restPort                        = 8082;
	private int httpsPort                       = 8083;
	
	private int jsonDepth                       = 1;
	private boolean logRequests                 = false;
	private String logPrefix                    = "structr";
	
	private String smtpHost                     = "localhost";
	private int smtpPort                        = 25;
	
	private String logDbName		    = "logDb.dat";
	
	private int maxIdleTime                     = Integer.parseInt(System.getProperty("maxIdleTime", "30000"));
	private int requestHeaderSize               = Integer.parseInt(System.getProperty("requestHeaderSize", "8192"));

	private Map<String, ServletHolder> servlets = new LinkedHashMap<String, ServletHolder>();
	private Map<String, String> servletParams   = new HashMap<String, String>();
	private ResourceHandler resourceHandler     = null;

	private boolean enableRewriteFilter         = false;
	private boolean quiet                       = false;
	private boolean enableHttps                 = false;
	private boolean enableGzipCompression       = true;
	
	private Class<? extends StructrServer> app  = null;
	private Class resourceProvider              = null;
	private Class authenticator                 = null;
	private String defaultPropertyView          = null;
	
	private Set<Class<? extends Service>> configuredServices	= new HashSet<Class<? extends Service>>();
	private Map<String, String> cronServiceTasks			= new LinkedHashMap<String, String>();
	
	private List<String> customConfigLines      = new LinkedList<String>();
	
	private List<Callback> callbacks            = new LinkedList<Callback>();

	public static class Callback {
		public Callback() {};
		public void execute() {};
	}

	//~--- methods --------------------------------------------------------

	private Structr(Class<? extends StructrServer> applicationClass, String applicationName, int httpPort, int httpsPort) {
		this.app = applicationClass;
		this.applicationName = applicationName;
		this.httpsPort = httpsPort;
		this.restPort = httpPort;
	}
	
	private Structr(Class<? extends StructrServer> applicationClass, String applicationName, int httpPort) {
		this.app = applicationClass;
		this.applicationName = applicationName;
		this.restPort = httpPort;
	}
	
	private Structr(Class<? extends StructrServer> applicationClass, String applicationName) {
		this.app = applicationClass;
		this.applicationName = applicationName;
	}
	
	private Structr(Class<? extends StructrServer> applicationClass) {
		this.app = applicationClass;
	}
	
	public static Structr createServer(Class<? extends StructrServer> applicationClass, String applicationName, int httpPort, int httpsPort) {
		return new Structr(applicationClass, applicationName, httpPort, httpsPort);
	}
	
	public static Structr createServer(Class<? extends StructrServer> applicationClass, String applicationName, int httpPort) {
		return new Structr(applicationClass, applicationName, httpPort);
	}
	
	public static Structr createServer(Class<? extends StructrServer> applicationClass, String applicationName) {
		return new Structr(applicationClass, applicationName);
	}
	
	public static Structr createServer(Class<? extends StructrServer> applicationClass) {
		return new Structr(applicationClass);
	}

	// ----- builder methods -----
	public Structr host(String host) {
		this.host = host;
		return this;
	}
	
	public Structr restUrl(String restUrl) {
		this.restUrl = restUrl;
		return this;
	}
	
	public Structr keyStorePath(String keyStorePath) {
		this.keyStorePath = keyStorePath;
		return this;
	}
	
	public Structr keyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
		return this;
	}
	
	public Structr contextPath(String contextPath) {
		this.contextPath = contextPath;
		return this;
	}
	
	public Structr basePath(String basePath) {
		this.basePath = basePath;
		return this;
	}
	
	public Structr jsonDepth(int jsonDepth) {
		this.jsonDepth = jsonDepth;
		return this;
	}
	
	public Structr httpPort(int httpPort) {
		this.restPort = httpPort;
		return this;
	}
	
	public Structr httpsPort(int httpsPort) {
		this.httpsPort = httpsPort;
		return this;
	}
	
	public Structr smtpHost(String smtpHost) {
		this.smtpHost = smtpHost;
		return this;
	}
	
	public Structr smtpPort(int smtpPort) {
		this.smtpPort = smtpPort;
		return this;
	}
	
	public Structr logRequests(boolean logRequests) {
		this.logRequests = logRequests;
		return this;
	}
		
	public Structr logName(String logName) {
		this.logPrefix = logName;
		return this;
	}
	
	public Structr logDbName(String logDbName) {
		this.logDbName = logDbName;
		return this;
	}
	
	public Structr resourceProvider(Class<? extends ResourceProvider> resourceProviderClass) {
		this.resourceProvider = resourceProviderClass;
		return this;
	}
	
	public Structr authenticator(Class<? extends Authenticator> authenticatorClass) {
		this.authenticator = authenticatorClass;
		return this;
	}

	public Structr addServlet(String servletMapping, ServletHolder servletHolder) {
		servlets.put(servletMapping, servletHolder);
		return this;
	}
		
	public Structr addResourceHandler(String resourceBase, boolean directoriesListed, String[] welcomeFiles) {
		resourceHandler = new ResourceHandler();
		resourceHandler.setDirectoriesListed(directoriesListed);
		resourceHandler.setWelcomeFiles(welcomeFiles);
		resourceHandler.setResourceBase(resourceBase);		
		return this;
	}
	
	public Structr defaultPropertyView(String defaultPropertyView) {
		this.defaultPropertyView = defaultPropertyView;
		return this;
	}

	public Structr addConfiguredServices(Class<? extends Service> configuredService) {
		this.configuredServices.add(configuredService);
		return this;
	}
	
	public Structr addCronServiceTask(String cronServiceTask, String cronExpression) {
		this.cronServiceTasks.put(cronServiceTask, cronExpression);
		return this;
	}
	
	public Structr addCustomConfig(String configLine) {
		this.customConfigLines.add(configLine);
		return this;
	}
	
	public Structr addCallback(Callback callback) {
		this.callbacks.add(callback);
		return this;
	}
	
	/**
	 * Enable url rewrite filter (disabled by default). Put urlrewrite.xml in the
	 * root of your JAR file.
	 * 
	 * @return 
	 */
	public Structr enableRewriteFilter() {
		this.enableRewriteFilter = true;
		return this;
	}
	
	/**
	 * Disable GZIP compression for this structr server (enabled by default).
	 * @return 
	 */
	public Structr disableGzipCompression() {
		this.enableGzipCompression = false;
		return this;
	}
	
	/**
	 * Start the structr server with the previously specified configuration.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws Exception 
	 */
	public Server start(boolean waitForExit) throws IOException, InterruptedException, Exception {
		
		String sourceJarName                 = app.getProtectionDomain().getCodeSource().getLocation().toString();
		if (StringUtils.stripEnd(sourceJarName, "/").endsWith("classes")) {
			
			String jarFile = System.getProperty("jarFile");
			if (StringUtils.isEmpty(jarFile)) {
				throw new IllegalArgumentException(app.getName() + " was started in an environment where the classloader cannot determine the JAR file containing the main class.\n"
					+ "Please specify the path to the JAR file in the parameter -DjarFile.\n"
					+ "Example: -DjarFile=${project.build.directory}/${project.artifactId}-${project.version}.jar");
			}
			sourceJarName = jarFile;
		}

		File baseDir                         = new File(System.getProperty("home", basePath));
		String basePath                      = baseDir.getAbsolutePath();
		
		// create base directory if it does not exist
		if (!baseDir.exists()) {
			baseDir.mkdirs();
		}
		
		configuredServices.add(ModuleService.class);
		configuredServices.add(NodeService.class);
		configuredServices.add(AgentService.class);
		configuredServices.add(CronService.class);
		configuredServices.add(LogService.class);
		
		
		File confFile                        = checkStructrConf(basePath, sourceJarName);
		Properties configuration             = getConfiguration(confFile);

		checkPrerequisites(configuration);
		
		Server server                        = new Server(restPort);
		List<Connector> connectors           = new LinkedList<Connector>();
		HandlerCollection handlerCollection  = new HandlerCollection();


		// add possible resource handler
		if (resourceHandler != null) {

			handlerCollection.addHandler(resourceHandler);
		
		}
		
		ServletContextHandler context        = new WebAppContext(server, basePath, contextPath);

		// create resource collection from base path & source JAR
		context.setBaseResource(new ResourceCollection(Resource.newResource(basePath), JarResource.newJarResource(Resource.newResource(sourceJarName))));
		context.setInitParameter("configfile.path", confFile.getAbsolutePath());

		if (enableGzipCompression) {

			FilterHolder gzipFilter = context.addFilter(GzipFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
			gzipFilter.setInitParameter("mimeTypes", "text/html,text/plain,text/css,text/javascript");

		}
		
		if (enableRewriteFilter) {
			
			FilterHolder rewriteFilter = context.addFilter(UrlRewriteFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
			rewriteFilter.setInitParameter("confPath", "/urlrewrite.xml");
		}
		
		// enable request logging
		//if ("true".equals(configuration.getProperty("log.requests", "false"))) {
		if (logRequests) {

			String etcPath = basePath + "/etc";
			File etcDir    = new File(etcPath);

			if (!etcDir.exists()) {

				etcDir.mkdir();
			}
		
			String logbackConfFilePath = basePath + "/etc/logback-access.xml";
			File logbackConfFile       = new File(logbackConfFilePath);

			if (!logbackConfFile.exists()) {

				// synthesize a logback accees log config file
				List<String> config = new LinkedList<String>();

				config.add("<configuration>");
				config.add("  <appender name=\"FILE\" class=\"ch.qos.logback.core.rolling.RollingFileAppender\">");
				config.add("    <rollingPolicy class=\"ch.qos.logback.core.rolling.TimeBasedRollingPolicy\">");
				config.add("      <fileNamePattern>logs/" + logPrefix + "-%d{yyyy_MM_dd}.request.log.zip</fileNamePattern>");
				config.add("    </rollingPolicy>");
				config.add("    <encoder>");
				config.add("      <pattern>%h %l %u %t \"%r\" %s %b %n%fullRequest%n%n%fullResponse</pattern>");
				config.add("    </encoder>");
				config.add("  </appender>");
				config.add("<appender-ref ref=\"FILE\" />");
				config.add("</configuration>");
				logbackConfFile.createNewFile();
				FileUtils.writeLines(logbackConfFile, "UTF-8", config);
			}

			FilterHolder loggingFilter = context.addFilter(TeeFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
			loggingFilter.setInitParameter("includes", "");
			
			RequestLogHandler requestLogHandler = new RequestLogHandler();
			String logPath                      = basePath + "/logs";
			File logDir                         = new File(logPath);

			// Create logs directory if not existing
			if (!logDir.exists()) {

				logDir.mkdir();

			}

			RequestLogImpl requestLog = new RequestLogImpl();
			requestLogHandler.setRequestLog(requestLog);
			
			ContextHandlerCollection contexts = new ContextHandlerCollection();
			contexts.setHandlers(new Handler[] { contexts, new DefaultHandler(), requestLogHandler });
			
			handlerCollection.addHandler(contexts);


		}

		// configure JSON REST servlet
		JsonRestServlet structrRestServlet     = new JsonRestServlet();
		ServletHolder structrRestServletHolder = new ServletHolder(structrRestServlet);

		servletParams.put("PropertyFormat", "FlatNameValue");
		servletParams.put("ResourceProvider", resourceProvider.getName());
		servletParams.put("Authenticator", authenticator.getName());
		servletParams.put("DefaultPropertyView", defaultPropertyView);
		servletParams.put("IdProperty", "uuid");

		structrRestServletHolder.setInitParameters(servletParams);
		structrRestServletHolder.setInitOrder(0);

		// add to servlets
		servlets.put(restUrl + "/*", structrRestServletHolder);

		// add servlet elements
		int position = 1;
		for (Entry<String, ServletHolder> servlet : servlets.entrySet()) {
			
			String path                 = servlet.getKey();
			ServletHolder servletHolder = servlet.getValue();
			
			servletHolder.setInitOrder(position++);
			
			logger.log(Level.INFO, "Adding servlet {0} for {1}", new Object[] { servletHolder, path } );
			
			context.addServlet(servletHolder, path);
		}
		
		// register structr application context listener
		context.addEventListener(new ApplicationContextListener());

		handlerCollection.addHandler(context);
		server.setHandler(handlerCollection);

		
		// HTTPs can be disabled
		if (enableHttps) {
			
			if (httpsPort > -1 && keyStorePath != null && !keyStorePath.isEmpty() && keyStorePassword != null) {

				// setup HTTP connector
				SslSelectChannelConnector httpsConnector = null;
				SslContextFactory factory = new SslContextFactory(keyStorePath);

				factory.setKeyStorePassword(keyStorePassword);

				httpsConnector = new SslSelectChannelConnector(factory);

				httpsConnector.setHost(host);

				httpsConnector.setPort(httpsPort);
				httpsConnector.setMaxIdleTime(maxIdleTime);
				httpsConnector.setRequestHeaderSize(requestHeaderSize);

				connectors.add(httpsConnector);

			} else {

				logger.log(Level.WARNING, "Unable to configure SSL, please make sure that application.https.port, application.keystore.path and application.keystore.password are set correctly in structr.conf.");
			}
		}
		
		if (host != null && !host.isEmpty() && restPort > -1) {

			SelectChannelConnector httpConnector = new SelectChannelConnector();

			httpConnector.setHost(host);
			httpConnector.setPort(restPort);
			httpConnector.setMaxIdleTime(maxIdleTime);
			httpConnector.setRequestHeaderSize(requestHeaderSize);

			connectors.add(httpConnector);

		} else {

			logger.log(Level.WARNING, "Unable to configure REST port, please make sure that application.host, application.rest.port and application.rest.path are set correctly in structr.conf.");
		}
		
		if (!connectors.isEmpty()) {

			server.setConnectors(connectors.toArray(new Connector[0]));
			
		} else {
			
			logger.log(Level.SEVERE, "No connectors configured, aborting.");
			System.exit(0);
		}
		
		server.setGracefulShutdown(1000);
		server.setStopAtShutdown(true);

		if (!quiet) {
			
			System.out.println();
			System.out.println("Starting " + applicationName + " (host=" + host + ":" + restPort + ", maxIdleTime=" + maxIdleTime + ", requestHeaderSize=" + requestHeaderSize + ")");
			System.out.println("Base path " + basePath);
			System.out.println();
			System.out.println(applicationName + " started:        http://" + host + ":" + restPort + restUrl);
			System.out.println();
		}
		
		server.start();

		// The jsp directory is created by the container, but we don't need it
		removeDir(basePath, "jsp");
		
		if (!callbacks.isEmpty()) {
			
			for (Callback callback : callbacks) {
				
				callback.execute();
			}
			
		}
		
		if (waitForExit) {
			
			server.join();
		
			if (!quiet) {

				System.out.println();
				System.out.println(applicationName + " stopped.");
				System.out.println();
			}
		}
		
		return server;
	}

	private File checkStructrConf(String basePath, String sourceJarName) throws IOException {

		// create and register config file
		String confPath = basePath + "/structr.conf";
		File confFile   = new File(confPath);

		// Create structr.conf if not existing
		if (!confFile.exists()) {

			// synthesize a config file
			List<String> config = new LinkedList<String>();

			config.add("##################################");
			config.add("# structr global config file     #");
			config.add("##################################");
			config.add("");
			
			if (sourceJarName.endsWith(".jar") || sourceJarName.endsWith(".war")) {
				
				config.add("# resources");
				config.add("resources = " + sourceJarName);
				config.add("");
			}
			
			config.add("# JSON output nesting depth");
			config.add("json.depth = " + jsonDepth);
			config.add("");
			config.add("# base directory");
			config.add("base.path = " + basePath);
			config.add("");
			config.add("# temp files directory");
			config.add("tmp.path = /tmp");
			config.add("");
			config.add("# database files directory");
			config.add("database.path = " + basePath + "/db");
			config.add("");
			config.add("# binary files directory");
			config.add("files.path = " + basePath + "/files");
			config.add("");
			config.add("# log database file");
			config.add("log.database.path = " + basePath + "/" + logDbName);
			config.add("");
			config.add("# REST server settings");
			config.add("application.host = " + host);
			config.add("application.rest.port = " + restPort);
			config.add("application.rest.path = " + restUrl);
			config.add("");
			config.add("application.https.enabled = " + enableHttps);
			config.add("application.https.port = " + httpsPort);
			config.add("application.keystore.path = " + keyStorePath);
			config.add("application.keystore.password = " + keyStorePassword);
			config.add("");
			config.add("# SMTP settings");
			config.add("smtp.host = " + smtpHost);
			config.add("smtp.port = " + smtpPort);
			config.add("");
			config.add("superuser.username = superadmin");
			config.add("superuser.password = " + RandomStringUtils.randomAlphanumeric(12));    // Intentionally, no default password here
			
			if (!configuredServices.isEmpty()) {
			
				config.add("");
				config.add("# services");

				StringBuilder configuredServicesLine = new StringBuilder("configured.services =");

				for (Class<? extends Service> serviceClass : configuredServices) {
					configuredServicesLine.append(" ").append(serviceClass.getSimpleName());
				}

				config.add(configuredServicesLine.toString());
			
			}
			
			config.add("");
			config.add("# logging");
			config.add("log.requests = " + logRequests);
			config.add("log.name = structr-yyyy_mm_dd.request.log");

			if (!cronServiceTasks.isEmpty()) {
			
				config.add("");
				config.add("# cron tasks");
				config.add("CronService.tasks = \\");

				StringBuilder cronServiceTasksLines = new StringBuilder();
				StringBuilder cronExpressions = new StringBuilder();

				for (Entry<String, String> task : cronServiceTasks.entrySet()) {

					String taskClassName = task.getKey();
					String cronExpression = task.getValue();
					cronServiceTasksLines.append(taskClassName).append(" \\\n");
					cronExpressions.append(taskClassName).append(".cronExpression = ").append(cronExpression).append("\n");

				}

				if (cronServiceTasksLines.length() > 0) {

					config.add(cronServiceTasksLines.substring(0, cronServiceTasksLines.length()-3));
					config.add(cronExpressions.toString());
				}
			
			}
			
			if (!customConfigLines.isEmpty()) {
			
				config.add("# custom configuration");

				for (String configLine : customConfigLines) {
					config.add(configLine);
				}

			}
			
			confFile.createNewFile();
			
			FileUtils.writeLines(confFile, "UTF-8", config);
		}
		
		return confFile;
	}

	private Properties getConfiguration(File confFile)  {
		
		Properties props    = new Properties();
		FileInputStream fis = null;

		try {
			fis = new FileInputStream(confFile);
			props.load(fis);
			
		} catch(IOException ioex) {
			
			logger.log(Level.WARNING, "Unable to load settings from structr.conf");
			
		} finally {
			
			if (fis != null) {
				
				try { fis.close(); } catch(Throwable t) {}
			}
		}

		
		return props;
	}

	private void checkPrerequisites(Properties configuration) {
		
		host             = configuration.getProperty("application.host", "0.0.0.0");
		restUrl          = configuration.getProperty("application.rest.path", "/structr/rest");
		restPort         = parseInt(configuration.getProperty("application.rest.port", "8082"), -1);
		httpsPort        = parseInt(configuration.getProperty("application.https.port", "-1"), -1);
		enableHttps      = parseBoolean(configuration.getProperty("application.https.enabled", "false"), false);
		
		keyStorePath     = configuration.getProperty("application.keystore.path", "");
		keyStorePassword = configuration.getProperty("application.keystore.password", "");
		
		if (authenticator == null) {
			
			logger.log(Level.WARNING, "Using default authenticator.");
			
			authenticator = DefaultAuthenticator.class;
		}
		
		if (resourceProvider == null) {
			
			logger.log(Level.WARNING, "Using default resource provider.");
			
			resourceProvider = DefaultResourceProvider.class;
		}
		
		if (!restUrl.startsWith("/")) {
			
			logger.log(Level.WARNING, "Prepending missing '/' to rest URL");
			
			restUrl = "/".concat(restUrl);
		}
	}
	
	private void removeDir(final String basePath, final String directoryName) throws IOException {

		String strippedBasePath = StringUtils.stripEnd(basePath, "/");
		
		File file = new File(strippedBasePath + "/" + directoryName);

		if (file.isDirectory()) {

			FileUtils.deleteDirectory(file);
			
		} else {

			file.delete();
		}
	}
	
	private int parseInt(Object source, int defaultValue) {
		
		try { return Integer.parseInt(source.toString()); } catch(Throwable ignore) {}
		
		return defaultValue;
	}
	
	private boolean parseBoolean(Object source, boolean defaultValue) {
		
		try { return Boolean.parseBoolean(source.toString()); } catch(Throwable ignore) {}
		
		return defaultValue;
	}
}
