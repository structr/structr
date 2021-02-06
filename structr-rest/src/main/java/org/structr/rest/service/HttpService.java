/*
 * Copyright (C) 2010-2021 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.service;

import ch.qos.logback.access.jetty.RequestLogImpl;
import ch.qos.logback.access.servlet.TeeFilter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.chemistry.opencmis.server.impl.CmisRepositoryContextListener;
import org.apache.chemistry.opencmis.server.impl.atompub.CmisAtomPubServlet;
import org.apache.chemistry.opencmis.server.impl.browser.CmisBrowserBindingServlet;
import org.apache.chemistry.opencmis.server.shared.BasicAuthCallContextHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.service.Command;
import org.structr.api.service.LicenseManager;
import org.structr.api.service.RunnableService;
import org.structr.api.service.ServiceDependency;
import org.structr.api.service.ServiceResult;
import org.structr.api.service.StartServiceInMaintenanceMode;
import org.structr.api.service.StopServiceForMaintenanceMode;
import org.structr.api.service.StructrServices;
import org.structr.core.Services;
import org.structr.rest.ResourceProvider;
import org.structr.rest.auth.SessionHelper;
import org.structr.rest.common.Stats;
import org.structr.rest.common.StatsCallback;
import org.structr.schema.SchemaService;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

@ServiceDependency(SchemaService.class)
@StopServiceForMaintenanceMode
@StartServiceInMaintenanceMode
public class HttpService implements RunnableService, StatsCallback {

	private static final Logger logger = LoggerFactory.getLogger(HttpService.class.getName());

	// set of resource providers for this service
	private final Set<ResourceProvider> resourceProviders = new LinkedHashSet<>();

	private enum LifecycleEvent {
		Started, Stopped
	}

	private Map<String, Map<String, Stats>> stats = new LinkedHashMap<>();
	private DefaultSessionCache sessionCache      = null;
	private GzipHandler gzipHandler               = null;
	private HttpConfiguration httpConfig          = null;
	private HttpConfiguration httpsConfig         = null;
	private SslContextFactory.Server sslServer    = null;
	private Server server                         = null;
	private Server maintenanceServer              = null;
	private int maxIdleTime                       = 30000;
	private int requestHeaderSize                 = 8192;
	private boolean httpsActive                   = false;

	@Override
	public void startService() throws Exception {

		logger.info("Starting {} (host={}:{}, maxIdleTime={}, requestHeaderSize={})", Settings.ApplicationTitle.getValue(), Settings.ApplicationHost.getValue(), Settings.getSettingOrMaintenanceSetting(Settings.HttpPort).getValue(), maxIdleTime, requestHeaderSize);
		logger.info("Base path {}", Settings.getBasePath());
		logger.info("{} started at http://{}:{}", Settings.ApplicationTitle.getValue(), Settings.ApplicationHost.getValue(), Settings.getSettingOrMaintenanceSetting(Settings.HttpPort).getValue());

		server.start();

		if (maintenanceServer != null) {
			maintenanceServer.start();
		}

		try {

			while (!server.isStarted()) {
				Thread.sleep(100);
			}

		} catch (Throwable t) {
			logger.error(ExceptionUtils.getStackTrace(t));
		}

		// The jsp directory is created by the container, but we don't need it
		removeDir(Settings.getBasePath(), "jsp");

		// send lifecycle event that the server has been started
		sendLifecycleEvent(LifecycleEvent.Started);
	}

	@Override
	public void stopService() {

		if (server != null) {

			try {
				server.stop();

			} catch (Exception ex) {
				logger.warn("Exception while stopping Jetty: {}", ex.getMessage());
			}
		}

		if (maintenanceServer != null) {

			try {
				maintenanceServer.stop();

			} catch (Exception ex) {
				logger.warn("Exception while stopping temporary maintenance server: {}", ex.getMessage());
			}
		}
	}

	@Override
	public boolean runOnStartup() {
		return true;
	}

	@Override
	public boolean isRunning() {
		return server != null && server.isRunning();
	}

	@Override
	public void injectArguments(Command command) {
	}

	@Override
	public ServiceResult initialize(final StructrServices services, String serviceName) throws ClassNotFoundException, IllegalAccessException, InstantiationException {

		final LicenseManager licenseManager = services.getLicenseManager();
		final boolean isTest                = Services.isTesting();
		String sourceJarName                = getClass().getProtectionDomain().getCodeSource().getLocation().toString();

		if (!isTest && StringUtils.stripEnd(sourceJarName, System.getProperty("file.separator")).endsWith("classes")) {

			String jarFile = System.getProperty("jarFile");
			if (StringUtils.isEmpty(jarFile)) {
				throw new IllegalArgumentException(getClass().getName() + " was started in an environment where the classloader cannot determine the JAR file containing the main class.\n"
					+ "Please specify the path to the JAR file in the parameter -DjarFile.\n"
					+ "Example: -DjarFile=${project.build.directory}/${project.artifactId}-${project.version}.jar");
			}
			sourceJarName = jarFile;
		}

		// load configuration from properties file
		maxIdleTime       = Services.parseInt(System.getProperty("maxIdleTime"), 30000);
		requestHeaderSize = Services.parseInt(System.getProperty("requestHeaderSize"), 8192);

		if (Settings.Async.getValue()) {
			logger.info("Running in asynchronous mode");
		}

		// other properties
		final String keyStorePath           = Settings.KeystorePath.getValue();
		final String keyStorePassword       = Settings.KeystorePassword.getValue();
		final String contextPath            = System.getProperty("contextPath", "/");
		final boolean enableHttps           = Settings.HttpsEnabled.getValue();
		final boolean enableGzipCompression = Settings.GzipCompression.getValue();
		final boolean logRequests           = Settings.RequestLogging.getValue();
		final String logPrefix              = Settings.LogPrefix.getValue();
		final String host                   = Settings.ApplicationHost.getValue();
		final boolean mainteanceModeActive  = Settings.MaintenanceModeEnabled.getValue();
		final int httpPort                  = Settings.getSettingOrMaintenanceSetting(Settings.HttpPort).getValue();
		final int httpsPort                 = Settings.getSettingOrMaintenanceSetting(Settings.HttpsPort).getValue();
		boolean forceHttps                  = Settings.getSettingOrMaintenanceSetting(Settings.ForceHttps).getValue();
		boolean enableRewriteFilter         = true;

		// get current base path
		String basePath = System.getProperty("home", Settings.getBasePath());
		if (StringUtils.isEmpty(basePath)) {

			// use cwd and, if that fails, /tmp as a fallback
			basePath = System.getProperty("user.dir", "/tmp");
		}

		// create base directory if it does not exist
		final File baseDir = new File(basePath);
		if (!baseDir.exists()) {
			baseDir.mkdirs();
		}

		server = new Server(httpPort);
		final ContextHandlerCollection contexts = new ContextHandlerCollection();

		contexts.addHandler(new DefaultHandler());

		final ServletContextHandler servletContext = new ServletContextHandler(server, contextPath, true, true);
		final ErrorHandler errorHandler = new ErrorHandler();

		errorHandler.setShowStacks(false);
		servletContext.setErrorHandler(errorHandler);

		if (enableGzipCompression) {
			gzipHandler = new GzipHandler();
			gzipHandler.setIncludedMimeTypes("text/html", "text/xml", "text/plain", "text/css", "text/javascript", "application/javascript", "application/json", "image/svg+xml");
			gzipHandler.setInflateBufferSize(32768);
			gzipHandler.setMinGzipSize(256);
			gzipHandler.setCompressionLevel(9);
			gzipHandler.setIncludedMethods("GET", "POST", "PUT", "HEAD", "DELETE");
			gzipHandler.addIncludedPaths("/*");
			gzipHandler.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ASYNC));
		}

		servletContext.setGzipHandler(gzipHandler);

		final List<Connector> connectors = new LinkedList<>();

		// create resource collection from base path & source JAR
		try {
			servletContext.setBaseResource(new ResourceCollection(Resource.newResource(basePath), JarResource.newJarResource(Resource.newResource(sourceJarName))));

		} catch (Throwable t) {

			logger.warn("Base resource {} not usable: {}", new Object[]{basePath, t.getMessage()});
		}

		// this is needed for the filters to work on the root context "/"
		servletContext.addServlet("org.eclipse.jetty.servlet.DefaultServlet", "/");
		servletContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");

		if (Settings.ConfigServletEnabled.getValue()) {

			// configuration wizard entry point
			servletContext.addServlet("org.structr.rest.servlet.ConfigServlet", "/structr/config/*");
		}

		// CMIS setup
		if (Settings.CmisEnabled.getValue() && (licenseManager == null || licenseManager.isModuleLicensed("cmis"))) {

			try {

				servletContext.addEventListener(new CmisRepositoryContextListener());

				final ServletHolder cmisAtomHolder = servletContext.addServlet(CmisAtomPubServlet.class.getName(), "/structr/cmis/atom/*");
				cmisAtomHolder.setInitParameter("callContextHandler", BasicAuthCallContextHandler.class.getName());
				cmisAtomHolder.setInitParameter("cmisVersion", "1.1");

				final ServletHolder cmisBrowserHolder = servletContext.addServlet(CmisBrowserBindingServlet.class.getName(), "/structr/cmis/browser/*");
				cmisBrowserHolder.setInitParameter("callContextHandler", BasicAuthCallContextHandler.class.getName());
				cmisBrowserHolder.setInitParameter("cmisVersion", "1.1");


			} catch (Throwable t) {
				logger.warn("Cannot initialize CMIS servlet", t);
			}
		}

		sessionCache = new DefaultSessionCache(servletContext.getSessionHandler());

		if (licenseManager != null) {

			final String hardwareId = licenseManager.getHardwareFingerprint();

			DefaultSessionIdManager idManager = new DefaultSessionIdManager(server, new SecureRandom(hardwareId.getBytes()));
			idManager.setWorkerName(hardwareId);

			sessionCache.getSessionHandler().setSessionIdManager(idManager);

			if (Settings.HttpOnly.getValue()) {
				sessionCache.getSessionHandler().setHttpOnly(isTest);
			}

		}

		if (Settings.ClearSessionsOnStartup.getValue()) {
			SessionHelper.clearAllSessions();
		}

		final StructrSessionDataStore sessionDataStore = new StructrSessionDataStore();

		sessionCache.setSessionDataStore(sessionDataStore);
		sessionCache.setSaveOnInactiveEviction(false);
		sessionCache.setRemoveUnloadableSessions(true);
		sessionCache.setEvictionPolicy(60);

		servletContext.getSessionHandler().setMaxInactiveInterval(maxIdleTime);
		servletContext.getSessionHandler().setSessionCache(sessionCache);

		if (enableRewriteFilter) {

			final FilterHolder rewriteFilter = new FilterHolder(UrlRewriteFilter.class);
			rewriteFilter.setInitParameter("confPath", "urlrewrite.xml");
			servletContext.addFilter(rewriteFilter, "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ASYNC));
		}

		contexts.addHandler(servletContext);

		// enable request logging
		if (logRequests) {

			final String etcPath = basePath + "/etc";
			final File etcDir    = new File(etcPath);

			if (!etcDir.exists()) {

				etcDir.mkdir();
			}

			final String logbackConfFilePath = basePath + "/etc/logback-access.xml";
			final File logbackConfFile       = new File(logbackConfFilePath);

			if (!logbackConfFile.exists()) {

				// synthesize a logback accees log config file
				List<String> config = new LinkedList<>();

				config.add("<configuration>");
				config.add("  <appender name=\"FILE\" class=\"ch.qos.logback.core.rolling.RollingFileAppender\">");
				config.add("    <rollingPolicy class=\"ch.qos.logback.core.rolling.TimeBasedRollingPolicy\">");
				config.add("      <fileNamePattern>logs/" + logPrefix + "-%d{yyyy_MM_dd}.request.log.zip</fileNamePattern>");
				config.add("    </rollingPolicy>");
				config.add("    <encoder>");
				config.add("      <charset>UTF-8</charset>");
				config.add("      <pattern>%h %l %u %t \"%r\" %s %b %n%fullRequest%n%n%fullResponse</pattern>");
				config.add("    </encoder>");
				config.add("  </appender>");
				config.add("  <appender-ref ref=\"FILE\" />");
				config.add("</configuration>");

				try {
					logbackConfFile.createNewFile();
					FileUtils.writeLines(logbackConfFile, "UTF-8", config);

				} catch (IOException ioex) {

					logger.warn("Unable to write logback configuration.", ioex);
				}
			}

			final FilterHolder loggingFilter = new FilterHolder(TeeFilter.class);
			servletContext.addFilter(loggingFilter, "/*", EnumSet.of(DispatcherType.REQUEST, Settings.Async.getValue() ? DispatcherType.ASYNC : DispatcherType.FORWARD));
			loggingFilter.setInitParameter("includes", "");

			final RequestLogHandler requestLogHandler = new RequestLogHandler();
			final String logPath                      = basePath + "/logs";
			final File logDir                         = new File(logPath);

			// Create logs directory if not existing
			if (!logDir.exists()) {

				logDir.mkdir();

			}

			final RequestLogImpl requestLog = new RequestLogImpl();
			requestLog.setName("REQUESTLOG");
			requestLog.start();

			requestLogHandler.setRequestLog(requestLog);

			final HandlerCollection handlers = new HandlerCollection();

			handlers.setHandlers(new Handler[]{contexts, requestLogHandler});

			server.setHandler(handlers);

		} else {

			server.setHandler(contexts);
		}

		final List<ContextHandler> resourceHandler = collectResourceHandlers();
		for (ContextHandler contextHandler : resourceHandler) {
			contexts.addHandler(contextHandler);
		}

		final Map<String, ServletHolder> servlets = collectServlets(licenseManager);

		// add servlet elements
		int position = 1;
		for (Map.Entry<String, ServletHolder> servlet : servlets.entrySet()) {

			final ServletHolder servletHolder = servlet.getValue();
			final String path = servlet.getKey();

			servletHolder.setInitOrder(position++);

			logger.info("Adding servlet {} for {}", new Object[]{servletHolder, path});

			servletContext.addServlet(servletHolder, path);
		}

		contexts.addHandler(servletContext);

		httpConfig = new HttpConfiguration();
		httpConfig.setSendServerVersion(false);
		httpConfig.setSecureScheme("https");
		httpConfig.setSecurePort(httpsPort);
		httpConfig.setOutputBufferSize(1024); // intentionally low buffer size to allow even small bits of content to be sent to the client in case of slow rendering
		httpConfig.setRequestHeaderSize(requestHeaderSize);

		if (StringUtils.isNotBlank(host) && httpPort > -1) {

			final ServerConnector httpConnector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));

			httpConnector.setHost(host);
			httpConnector.setPort(httpPort);

			connectors.add(httpConnector);

		} else {

			logger.warn("Unable to configure HTTP server port, please make sure that {} and {} are set correctly in structr.conf.", Settings.ApplicationHost.getKey(), Settings.getSettingOrMaintenanceSetting(Settings.HttpPort).getKey());
		}

		httpsActive = false;

		if (enableHttps) {

			if (httpsPort > -1 && keyStorePath != null && !keyStorePath.isEmpty() && keyStorePassword != null) {

				httpsActive = true;

				httpsConfig = new HttpConfiguration(httpConfig);
				httpsConfig.addCustomizer(new SecureRequestCustomizer());

				sslServer = new SslContextFactory.Server();
				sslServer.setKeyStorePath(keyStorePath);
				sslServer.setKeyStorePassword(keyStorePassword);

				String excludedProtocols = Settings.excludedProtocols.getValue();
				String includedProtocols = Settings.includedProtocols.getValue();
				String disabledCiphers = Settings.disabledCipherSuites.getValue();

				if (disabledCiphers.length() > 0) {
					disabledCiphers = disabledCiphers.replaceAll("\\s+", "");
					sslServer.setExcludeCipherSuites(disabledCiphers.split(","));
				}

				if (excludedProtocols.length() > 0) {
					excludedProtocols = excludedProtocols.replaceAll("\\s+", "");
					sslServer.setExcludeProtocols(excludedProtocols.split(","));
				}

				if (includedProtocols.length() > 0) {
					includedProtocols = includedProtocols.replaceAll("\\s+", "");
					sslServer.setIncludeProtocols(includedProtocols.split(","));
				}

				final ServerConnector httpsConnector = new ServerConnector(server,
					new SslConnectionFactory(sslServer, "http/1.1"),
					new HttpConnectionFactory(httpsConfig));

				if (forceHttps) {
					sessionCache.getSessionHandler().setSecureRequestOnly(true);
				}

				httpsConnector.setPort(httpsPort);
				httpsConnector.setIdleTimeout(500000);

				httpsConnector.setHost(host);
				httpsConnector.setPort(httpsPort);

				if (Settings.dumpJettyStartupConfig.getValue()) {
					logger.info(httpsConnector.dump());
				}

				connectors.add(httpsConnector);

			} else {

				httpsActive = false;

				logger.warn("Unable to configure SSL, please make sure that {}, {} and {} are set correctly in structr.conf.", new Object[]{
					Settings.getSettingOrMaintenanceSetting(Settings.HttpsPort).getKey(),
					Settings.KeystorePath.getKey(),
					Settings.KeystorePassword.getKey()
				});
			}
		}

		if (!connectors.isEmpty()) {

			server.setConnectors(connectors.toArray(new Connector[0]));

		} else {

			logger.error("No connectors configured, aborting.");
			System.exit(0);
		}

		server.setStopTimeout(1000);
		server.setStopAtShutdown(true);

		setupMaintenanceServer(mainteanceModeActive);

		return new ServiceResult(true);
	}

	private void setupMaintenanceServer(final boolean mainteanceModeActive) {

		if (mainteanceModeActive) {

			final String keyStorePath           = Settings.KeystorePath.getValue();
			final String keyStorePassword       = Settings.KeystorePassword.getValue();
			final String contextPath            = System.getProperty("contextPath", "/");
			final boolean enableHttps           = Settings.HttpsEnabled.getValue();
			final String host                   = Settings.ApplicationHost.getValue();
			final int httpPort                  = Settings.HttpPort.getValue();
			final int httpsPort                 = Settings.HttpsPort.getValue();

			maintenanceServer = new Server(Settings.HttpPort.getValue());

			final String resourceBase = Settings.MaintenanceResourcePath.getValue();

			boolean useDefaultHandler = true;

			if (!StringUtils.isEmpty(resourceBase)) {

				final Path maintenanceResourceBase = Paths.get(resourceBase);
				if (Files.exists(maintenanceResourceBase) && Files.isDirectory(maintenanceResourceBase)) {
					useDefaultHandler = false;
				} else {
					logger.warn("Falling back to default maintenance handler. Given path does not exist or is not a directory. {}: {}", Settings.MaintenanceResourcePath.getKey(), resourceBase);
				}
			}

			if (useDefaultHandler) {

				maintenanceServer.setHandler(new AbstractHandler() {
					@Override
					public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

						if (response.isCommitted() || baseRequest.isHandled())
							return;

						baseRequest.setHandled(true);

						final String method = request.getMethod();

						if (!HttpMethod.GET.is(method)) {
							response.sendError(HttpServletResponse.SC_NOT_FOUND);
							return;
						}

						response.setStatus(HttpServletResponse.SC_OK);
						response.setContentType(MimeTypes.Type.TEXT_HTML_UTF_8.toString());

						final StringBuilder maintenanceHTML = new StringBuilder();
						maintenanceHTML.append("<!DOCTYPE html>\n");
						maintenanceHTML.append("<html lang=\"en\">\n<head>\n");
						maintenanceHTML.append("<title>Maintenance Mode Active</title>\n");
						maintenanceHTML.append("<meta charset=\"utf-8\">\n");
						maintenanceHTML.append("</head>\n<body>\n");
						maintenanceHTML.append("<h2>Maintenance Mode Active</h2>\n");
						maintenanceHTML.append("<p>");
						maintenanceHTML.append(Settings.MaintenanceMessage.getValue());
						maintenanceHTML.append("</p>\n");
						maintenanceHTML.append("</body>\n</html>\n");

						response.setContentLength(maintenanceHTML.length());

						try (OutputStream out = response.getOutputStream()) {
							out.write(maintenanceHTML.toString().getBytes());
						}
					}
				});

			} else {

				final ResourceHandler resourceHandler = new RedirectingResourceHandler();
				resourceHandler.setDirectoriesListed(false);

				resourceHandler.setResourceBase(resourceBase);
				resourceHandler.setCacheControl("max-age=0");

				final ContextHandler staticResourceHandler = new ContextHandler();
				staticResourceHandler.setContextPath(contextPath);
				staticResourceHandler.setHandler(resourceHandler);

				maintenanceServer.setHandler(staticResourceHandler);
			}

			final List<Connector> connectors = new LinkedList<>();

			httpConfig = new HttpConfiguration();
			httpConfig.setSendServerVersion(false);
			httpConfig.setSecureScheme("https");
			httpConfig.setSecurePort(httpsPort);

			if (StringUtils.isNotBlank(host) && httpPort > -1) {

				final ServerConnector httpConnector = new ServerConnector(maintenanceServer, new HttpConnectionFactory(httpConfig));

				httpConnector.setHost(host);
				httpConnector.setPort(httpPort);

				connectors.add(httpConnector);
			}

			if (enableHttps) {

				if (httpsPort > -1 && keyStorePath != null && !keyStorePath.isEmpty() && keyStorePassword != null) {

					final ServerConnector httpsConnector = new ServerConnector(maintenanceServer,
						new SslConnectionFactory(sslServer, "http/1.1"),
						new HttpConnectionFactory(httpsConfig));

					httpsConnector.setPort(httpsPort);
					httpsConnector.setIdleTimeout(500000);

					httpsConnector.setHost(host);
					httpsConnector.setPort(httpsPort);

					connectors.add(httpsConnector);
				}
			}

			if (!connectors.isEmpty()) {

				maintenanceServer.setConnectors(connectors.toArray(new Connector[0]));
			}

			maintenanceServer.setStopTimeout(1000);
			maintenanceServer.setStopAtShutdown(true);
		}
	}

	public void reloadSSLCertificate() {

		if (sslServer != null) {

			try {

				final String keyStorePath           = Settings.KeystorePath.getValue();
				final String keyStorePassword       = Settings.KeystorePassword.getValue();

				// in case path/password changed
				sslServer.setKeyStorePath(keyStorePath);
				sslServer.setKeyStorePassword(keyStorePassword);

				sslServer.reload(new Consumer<SslContextFactory>() {
					@Override
					public void accept(SslContextFactory t) {
					}
				});

			} catch (Exception e) {

				logger.error("Unable to reload SSL certificate.", e);
			}
		} else {

			logger.warn("Server started without SSL. Need to restart service.");
		}
	}

	@Override
	public void initialized() {}

	@Override
	public void shutdown() {


		if (server != null) {

			try {
				server.stop();

				if (Settings.ClearSessionsOnShutdown.getValue()) {
					SessionHelper.clearAllSessions();
				}

			} catch (Exception ex) {

				logger.warn("Error while stopping Jetty server: {}", ex.getMessage());
			}
		}

		// send lifecycle event that the server has been stopped
		sendLifecycleEvent(LifecycleEvent.Stopped);
	}

	@Override
	public String getName() {
		return HttpService.class.getName();
	}

	@Override
	public boolean isVital() {
		return true;
	}

	@Override
	public boolean waitAndRetry() {
		return false;
	}

	public boolean isHttpsActive() {
		return httpsActive;
	}

	public Map<String, Stats> getRequestStats(final String key) {

		Map<String, Stats> map = stats.get(key);
		if (map == null) {

			map = new LinkedHashMap<>();
			stats.put(key, map);
		}

		return map;
	}

	// ----- interface StatsCallback -----
	@Override
	public void recordStatsValue(final String key, final String source, final long value) {

		final Map<String, Stats> map = getRequestStats(key);
		Stats stats                  = map.get(source);

		if (stats == null) {

			stats = new Stats();
			map.put(source, stats);
		}

		stats.value(value);
	}


	// ----- interface Feature -----
	@Override
	public String getModuleName() {
		return "rest";
	}

	public Set<ResourceProvider> getResourceProviders() {
		return resourceProviders;
	}

	public SessionCache getSessionCache() {
		return sessionCache;
	}

	// ----- private methods -----
	private List<ContextHandler> collectResourceHandlers() throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		final List<ContextHandler> resourceHandlers = new LinkedList<>();
		final String resourceHandlerList            = Settings.ResourceHandlers.getValue();

		if (resourceHandlerList != null) {

			for (String resourceHandlerName : resourceHandlerList.split("[ \\t]+")) {

				if (StringUtils.isNotBlank(resourceHandlerName)) {

					final String contextPath = Settings.getOrCreateStringSetting(resourceHandlerName, "contextPath").getValue();
					if (contextPath != null) {

						final String resourceBase = Settings.getOrCreateStringSetting(resourceHandlerName, "resourceBase").getValue();
						if (resourceBase != null) {

							final ResourceHandler resourceHandler = new RedirectingResourceHandler();
							resourceHandler.setDirectoriesListed(Settings.getBooleanSetting(resourceHandlerName, "directoriesListed").getValue());

							final String welcomeFiles = Settings.getOrCreateStringSetting(resourceHandlerName, "welcomeFiles").getValue();
							if (welcomeFiles != null) {

								resourceHandler.setWelcomeFiles(StringUtils.split(welcomeFiles));
							}

							resourceHandler.setResourceBase(resourceBase);
							resourceHandler.setCacheControl("max-age=0");
							//resourceHandler.setEtags(true);

							final ContextHandler staticResourceHandler = new ContextHandler();
							staticResourceHandler.setContextPath(contextPath);
							staticResourceHandler.setHandler(resourceHandler);

							resourceHandlers.add(staticResourceHandler);

						} else {

							logger.warn("Unable to register resource handler {}, missing {}.resourceBase", resourceHandlerName, resourceHandlerName);
						}

					} else {

						logger.warn("Unable to register resource handler {}, missing {}.contextPath", resourceHandlerName, resourceHandlerName);
					}
				}
			}

		} else {

			logger.warn("No resource handlers configured for HttpService.");
		}

		return resourceHandlers;
	}

	private Map<String, ServletHolder> collectServlets(final LicenseManager licenseManager) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		final Map<String, ServletHolder> servlets = new LinkedHashMap<>();
		String servletNameList                    = Settings.Servlets.getValue();

		if (servletNameList != null) {

			for (String servletName : servletNameList.split("[ \\t]+")) {

				if (StringUtils.isNotBlank(servletName)) {

					final String servletClassName = Settings.getOrCreateStringSetting(servletName, "class").getValue();
					if (servletClassName != null) {

						final String servletPath = Settings.getOrCreateStringSetting(servletName, "path").getValue();
						if (servletPath != null) {

							try {

								final HttpServlet servlet = (HttpServlet)Class.forName(servletClassName).newInstance();
								if (servlet instanceof HttpServiceServlet) {

									final HttpServiceServlet httpServiceServlet = (HttpServiceServlet)servlet;

									// check license for servlet
									if (licenseManager == null || licenseManager.isValid(httpServiceServlet)) {

										final StructrHttpServiceConfig cfg = httpServiceServlet.getConfig();
										if (cfg != null) {

											cfg.initializeFromSettings(servletName, resourceProviders);
										}

										if (servletPath.endsWith("*")) {

											servlets.put(servletPath, new ServletHolder(servlet));

										} else {

											servlets.put(servletPath + "/*", new ServletHolder(servlet));
										}

										// callback for statistics
										httpServiceServlet.registerStatsCallback(this);
									}
								}

							} catch (ClassNotFoundException nfex) {

								logger.warn("Unable to instantiate servlet class {} for servlet {}", servletClassName, servletName);
							}

						} else {

							logger.warn("Unable to register servlet {}, missing {}.path", servletName, servletName);
						}

					} else {

						logger.warn("Unable to register servlet {}, missing {}.class", servletName, servletName);
					}
				}
			}

		} else {

			logger.warn("No servlets configured for HttpService.");
		}

		return servlets;
	}

	private void removeDir(final String basePath, final String directoryName) {

		final String strippedBasePath = StringUtils.stripEnd(basePath, "/");
		final File file               = new File(strippedBasePath + "/" + directoryName);

		if (file.isDirectory()) {

			try {

				FileUtils.deleteDirectory(file);

			} catch (IOException ex) {

				logger.error("Unable to delete directory {}: {}", new Object[]{directoryName, ex.getMessage()});
			}

		} else {

			file.delete();
		}
	}

	// ----- private methods -----
	private void sendLifecycleEvent(final LifecycleEvent event) {

		// instantiate and call lifecycle callbacks from configuration file
		final String listeners = Settings.LifecycleListeners.getValue();
		if (listeners != null) {

			final String[] listenerClasses = listeners.split("[\\s ,;]+");
			for (String listenerClass : listenerClasses) {

				if (StringUtils.isNotBlank(listenerClass)) {

					try {
						final HttpServiceLifecycleListener listener = (HttpServiceLifecycleListener) Class.forName(listenerClass).newInstance();
						switch (event) {

							case Started:
								listener.serverStarted();
								break;

							case Stopped:
								listener.serverStopped();
								break;
						}

					} catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {

						logger.error("Unable to send lifecycle event to listener " + listenerClass, ex);
					}
				}
			}
		}
	}

	/**
	 * A resource handler that redirects all requests to the config
	 * servlet if the system is not configured yet.
	 */
	private class RedirectingResourceHandler extends ResourceHandler {

		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

			if (Settings.SetupWizardCompleted.getValue() == false && ("/".equals(target) || "/index.html".equals(target))) {

				// please don't cache this redirect
				response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
				response.setHeader("Expires", null);

				// redirect to setup wizard
				response.sendRedirect("/structr/config");

			} else {

				super.handle(target, baseRequest, request, response);
			}
		}
	}
}

