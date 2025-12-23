/*
 * Copyright (C) 2010-2025 Structr GmbH
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


import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.http.*;
import org.eclipse.jetty.http2.WindowRateControl;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.rewrite.handler.RewriteRegexRule;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.session.DefaultSessionCache;
import org.eclipse.jetty.session.DefaultSessionIdManager;
import org.eclipse.jetty.session.SessionCache;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.service.*;
import org.structr.core.Services;
import org.structr.rest.auth.SessionHelper;
import org.structr.rest.common.MetricsFilter;
import org.structr.rest.common.Stats;
import org.structr.rest.common.StatsCallback;
import org.structr.rest.servlet.MetricsServlet;
import org.structr.schema.SchemaService;
import org.structr.websocket.servlet.WebSocketConfigurator;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@ServiceDependency(SchemaService.class)
@StopServiceForMaintenanceMode
@StartServiceInMaintenanceMode
public class HttpService implements RunnableService, StatsCallback {

	private static final Logger logger = LoggerFactory.getLogger(HttpService.class.getName());

	private enum LifecycleEvent {
		Started, Stopped
	}

	private final Map<String, Map<String, Stats>> stats = new ConcurrentHashMap<>();
	private SslContextFactory.Server sslContextFactory  = null;
	private DefaultSessionCache sessionCache            = null;
	private GzipHandler gzipHandler                     = null;
	private HttpConfiguration httpConfig                = null;
	private HttpConfiguration httpsConfig               = null;
	private Server server                               = null;
	private Server maintenanceServer                    = null;
	private int requestHeaderSize                       = 8192;
	private boolean httpsActive                         = false;

	static {

		Services.getInstance().registerInitializationCallback(() -> {

			if (Settings.ClearSessionsOnStartup.getValue()) {
				SessionHelper.clearAllSessions();
			}
		});
	}

	@Override
	public void startService() throws Exception {

		logger.info("Starting {} (host={}:{}, maxIdleTime={}, requestHeaderSize={})", Settings.ApplicationTitle.getValue(), Settings.ApplicationHost.getValue(), Settings.getSettingOrMaintenanceSetting(Settings.HttpPort).getValue(), Services.getGlobalSessionTimeout(), requestHeaderSize);
		logger.info("Base path {}", Settings.getBasePath());
		logger.info("{} started at http://{}:{}", Settings.ApplicationTitle.getValue(), Settings.ApplicationHost.getValue(), Settings.getSettingOrMaintenanceSetting(Settings.HttpPort).getValue());

		Exception exception = null;
		int maxAttempts = Services.isTesting() ? 12 : 3;

		while (maxAttempts-- > 0) {

			try {

				server.start();

				if (maintenanceServer != null) {
					maintenanceServer.start();
				}

				maxAttempts = 0;
				exception = null;

			} catch (Exception e) {
				logger.warn("Error, retrying {} more times after 10s - Caught: {} ", maxAttempts, e.getMessage());
				Thread.sleep(10000);
				exception = e;
			}
		}

		// if exception is set, don't continue and throw it
		if (exception != null) {
			throw exception;
		}

		try {

			while (!server.isStarted() || (maintenanceServer != null && !maintenanceServer.isStarted()) ) {
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

		stopJetty(server, "HTTP Service");
		stopJetty(maintenanceServer, "Maintenance HTTP Service");
	}

	private void stopJetty(final Server jettyServer, final String serverName) {

		if (jettyServer == null) {
			return;
		}

		try {
			jettyServer.setStopTimeout(30_000);
			jettyServer.stop();
			jettyServer.join();

		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			logger.warn("Interrupted while stopping {}", serverName, ie);

		} catch (Exception ex) {
			logger.warn("Exception while stopping {}", serverName, ex);
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
	public ServiceResult initialize(final StructrServices services, String serviceName) throws ReflectiveOperationException {

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
		final String host                   = Settings.ApplicationHost.getValue();
		final boolean maintenanceModeActive = Settings.MaintenanceModeEnabled.getValue();
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

		final ServletContextHandler servletContext = new ServletContextHandler(contextPath, true, true);
		final ErrorHandler errorHandler = new ErrorHandler();

		errorHandler.setShowStacks(false);
		servletContext.setErrorHandler(errorHandler);
		servletContext.getServletHandler().setDecodeAmbiguousURIs(true);

		// websockets (new)
		servletContext.insertHandler(WebSocketUpgradeHandler.from(server, servletContext, new WebSocketConfigurator("WebSocketServlet")));

		if (enableGzipCompression) {
			gzipHandler = new GzipHandler();
			gzipHandler.setIncludedMimeTypes("text/html", "text/xml", "text/plain", "text/css", "text/javascript", "application/javascript", "application/json", "image/svg+xml");
			gzipHandler.setInflateBufferSize(32768);
			gzipHandler.setMinGzipSize(256);
			gzipHandler.setIncludedMethods("GET", "POST", "PUT", "HEAD", "DELETE");
			gzipHandler.addIncludedPaths("/*");
			//gzipHandler.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ASYNC));
		}

		servletContext.insertHandler(gzipHandler);

		final List<Connector> connectors = new LinkedList<>();

		// Enable serving static resources for structr-ui (and redirect to config servlet if the system is not configured yet)
		{
			final ResourceHandler resourceHandler = new ResourceHandler(servletContext) {

				@Override
				public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {

					final String target = Request.getPathInContext(request);

					if (Settings.SetupWizardCompleted.getValue() == false && Settings.ConfigServletEnabled.getValue() == true && ("/".equals(target) || "/index.html".equals(target))) {

						final HttpFields.Mutable headers = response.getHeaders();

						// please don't cache this redirect
						headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
						headers.add("Expires", (String) null);
						headers.add("Location", Settings.ApplicationRootPath.getValue() + "/structr/config");

						response.setStatus(HttpServletResponse.SC_FOUND);

						callback.succeeded();

						return true;

					} else {

						return super.handle(request, response, callback);
					}
				}
			};

			// locate static resources (local vs. deb fallback)
			final String resourceBasePath = (Paths.get("src/main/resources/structr").toFile().exists() ? "src/main/resources/structr" : "structr");
			final ResourceFactory factory = ResourceFactory.of(resourceHandler);
			final Resource baseResource   = factory.newResource(URI.create(resourceBasePath).normalize());

			resourceHandler.setDirAllowed(false);
			resourceHandler.setWelcomeFiles("index.html");

			resourceHandler.setBaseResource(baseResource);
			resourceHandler.setCacheControl("max-age=0");
			//resourceHandler.setEtags(true);

			final ContextHandler context = new ContextHandler("/structr");
			context.setHandler(resourceHandler);
			context.clearAliasChecks();
			context.addAliasCheck((pathInContext, resource) -> resource.exists());

			contexts.addHandler(context);
		}

		if (Settings.ConfigServletEnabled.getValue()) {

			// configuration wizard entry point
			servletContext.addServlet("org.structr.rest.servlet.ConfigServlet", "/structr/config/*");
		}

		final SessionHandler sessionHandler = servletContext.getSessionHandler();

		sessionCache = new DefaultSessionCache(sessionHandler);

		if (licenseManager != null) {

			final String hardwareId = licenseManager.getHardwareFingerprint();

			DefaultSessionIdManager idManager = new DefaultSessionIdManager(server, new SecureRandom(hardwareId.getBytes()));
			idManager.setWorkerName(hardwareId);

			sessionCache.getSessionManager().setSessionIdManager(idManager);
		}

		// configure the HttpOnly flag for JSESSIONID cookie
		sessionHandler.setHttpOnly(Settings.HttpOnly.getValue());

		// configure the SameSite attribute for JSESSIONID cookie
		sessionHandler.setSameSite(HttpCookie.SameSite.valueOf(Settings.CookieSameSite.getValue().toUpperCase()));

		// configure the Secure flag for JSESSIONID cookie
		sessionHandler.getSessionCookieConfig().setSecure(Settings.CookieSecure.getValue());

		final StructrSessionDataStore sessionDataStore = new StructrSessionDataStore();

		sessionCache.setSessionDataStore(sessionDataStore);
		sessionCache.setSaveOnInactiveEviction(false);
		sessionCache.setRemoveUnloadableSessions(true);
		sessionCache.setEvictionPolicy(60);

		// make sessions "immortal" from the session handlers POV (we handle timeout)
		servletContext.getSessionHandler().setMaxInactiveInterval(-1);
		servletContext.getSessionHandler().setSessionCache(sessionCache);

		// enable request logging
		if (logRequests) {

			final String logPath = basePath + "/logs";
			final File logDir    = new File(logPath);

			// Create logs directory if not existing
			if (!logDir.exists()) {

				logDir.mkdir();
			}

			Slf4jRequestLogWriter requestLogWriter = new Slf4jRequestLogWriter();

			final String request_format = "%t \"%r\" %s %{ms}T";
			final RequestLog requestLog = new CustomRequestLog(requestLogWriter, request_format);
			server.setRequestLog(requestLog);
		}

		final Map<String, ServletHolder> servlets = collectServlets(licenseManager);

		// add servlet elements
		int position = 1;
		for (Map.Entry<String, ServletHolder> servlet : servlets.entrySet()) {

			final ServletHolder servletHolder = servlet.getValue();
			final String path = servlet.getKey();

			servletHolder.setInitOrder(position++);

			logger.info("Adding servlet {} for {}", servletHolder, path);

			servletContext.addServlet(servletHolder, path);
		}

		// only add metrics filter if metrics servlet is enabled
		if (Settings.Servlets.getValue("").contains(MetricsServlet.class.getSimpleName())) {
			servletContext.addFilter(MetricsFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
		}

		// Always add servletContext last because it's terminal in the resource chain
		contexts.addHandler(servletContext);

		if (enableRewriteFilter) {

			final RewriteHandler rewriteHandler = new RewriteHandler();

			//rewriteHandler.setRewriteRequestURI(true);

			rewriteHandler.addRule(new RewriteRegexRule("^(\\/(?!structr$|structr\\/.*).*)", "/structr/html$1"));
			rewriteHandler.setHandler(contexts);
			server.setHandler(rewriteHandler);

			// Enable https redirect handler
			if (forceHttps) {

				SecuredRedirectHandler securedHandler = new SecuredRedirectHandler();
				securedHandler.setHandler(rewriteHandler);
				server.setHandler(securedHandler);
			}
		}

		httpConfig = new HttpConfiguration();
		httpConfig.setSendServerVersion(false);
		httpConfig.setSecureScheme("https");
		httpConfig.setSecurePort(httpsPort);
		httpConfig.setOutputBufferSize(1024 * 1024); // intentionally low buffer size to allow even small bits of content to be sent to the client in case of slow rendering
		httpConfig.setRequestHeaderSize(requestHeaderSize);

		switch(Settings.UriCompliance.getValue()) {

			default:
			case "RFC3986":
				httpConfig.setUriCompliance(UriCompliance.RFC3986);
				break;

			case "JETTY_DEFAULT":
				httpConfig.setUriCompliance(UriCompliance.DEFAULT);
				break;

			case "LEGACY":
				httpConfig.setUriCompliance(UriCompliance.LEGACY);
				break;

			case "RFC3986_UNAMBIGUOUS":
				httpConfig.setUriCompliance(UriCompliance.UNAMBIGUOUS);
				break;

			case "UNSAFE":
				httpConfig.setUriCompliance(UriCompliance.UNSAFE);
				break;
		}

		httpConfig.setUriCompliance(UriCompliance.from("RFC3986,AMBIGUOUS_PATH_SEPARATOR"));

		if (StringUtils.isNotBlank(host) && httpPort > -1) {

			final ServerConnector httpConnector = new ServerConnector(server, new HttpConnectionFactory(httpConfig), new HTTP2CServerConnectionFactory(httpConfig));

			httpConnector.setHost(host);
			httpConnector.setPort(httpPort);

			connectors.add(httpConnector);

		} else {

			logger.warn("Unable to configure HTTP server port, please make sure that {} and {} are set correctly in structr.conf.", Settings.ApplicationHost.getKey(), Settings.getSettingOrMaintenanceSetting(Settings.HttpPort).getKey());
		}

		httpsActive = false;

		if (enableHttps) {

			if (httpsPort > -1 && keyStorePath != null && !keyStorePath.isEmpty() && keyStorePassword != null) {

				try {

					httpsActive = true;

					httpsConfig = new HttpConfiguration(httpConfig);

					final SecureRequestCustomizer secureRequestCustomizer = new SecureRequestCustomizer();
					secureRequestCustomizer.setSniRequired(Settings.SNIRequired.getValue());
					secureRequestCustomizer.setSniHostCheck(Settings.SNIHostCheck.getValue());

					if (!Settings.SNIRequired.getValue() && !Settings.SNIHostCheck.getValue()) {

						logger.info("HTTPS enabled with default settings of disabled SNI enforcement.");
					}
					logger.info("SNI settings: httpservice.sni.required = {}, httpservice.sni.hostcheck = {}", Settings.SNIRequired.getValue(), Settings.SNIHostCheck.getValue());

					httpsConfig.addCustomizer(secureRequestCustomizer);

					sslContextFactory = new SslContextFactory.Server();
					sslContextFactory.setKeyStorePath(keyStorePath);
					sslContextFactory.setKeyStorePassword(keyStorePassword);

					String excludedProtocols = Settings.excludedProtocols.getValue();
					String includedProtocols = Settings.includedProtocols.getValue();
					String disabledCiphers = Settings.disabledCipherSuites.getValue();

					if (disabledCiphers.length() > 0) {
						disabledCiphers = disabledCiphers.replaceAll("\\s+", "");
						sslContextFactory.setExcludeCipherSuites(disabledCiphers.split(","));
					}

					if (excludedProtocols.length() > 0) {
						excludedProtocols = excludedProtocols.replaceAll("\\s+", "");
						sslContextFactory.setExcludeProtocols(excludedProtocols.split(","));
					}

					if (includedProtocols.length() > 0) {
						includedProtocols = includedProtocols.replaceAll("\\s+", "");
						sslContextFactory.setIncludeProtocols(includedProtocols.split(","));
					}

					final HttpConnectionFactory http11 = new HttpConnectionFactory(httpsConfig);
					final HTTP2ServerConnectionFactory http2 = new HTTP2ServerConnectionFactory(httpsConfig);
					http2.setRateControlFactory(new WindowRateControl.Factory(Settings.HttpConnectionRateLimit.getValue()));

					if (forceHttps) {
						sessionHandler.setSecureRequestOnly(true);
					}

					ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
					alpn.setDefaultProtocol(http11.getProtocol());

					final SslConnectionFactory tls = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

					final ServerConnector httpsConnector = new ServerConnector(server, tls, alpn, http2, http11);

					httpsConnector.setIdleTimeout(500000);
					httpsConnector.setHost(host);
					httpsConnector.setPort(httpsPort);

					if (Settings.dumpJettyStartupConfig.getValue()) {
						logger.info(httpsConnector.dump());
					}

					connectors.add(httpsConnector);

				} catch (Throwable t) {
					logger.warn("Unable to start SSL connector: {}", t.getMessage());
				}

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

		setupMaintenanceServer(maintenanceModeActive);

		return new ServiceResult(true);
	}

	private void setupMaintenanceServer(final boolean maintenanceModeActive) {

		if (maintenanceModeActive) {

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

				maintenanceServer.setHandler(new Handler.Abstract() {
					@Override
					public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {

						if (response.isCommitted()) {
							callback.succeeded();
							return true;
						}

						final String method = request.getMethod();

						if (!HttpMethod.GET.is(method)) {

							response.setStatus(HttpServletResponse.SC_NOT_FOUND);
							callback.succeeded();

							return true;
						}

						final HttpFields.Mutable responseHeaders = response.getHeaders();

						response.setStatus(HttpServletResponse.SC_OK);

						responseHeaders.add(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_HTML_UTF_8.toString());

						final StringBuilder maintenanceHTML = new StringBuilder();
						maintenanceHTML.append("<!DOCTYPE html>\n");
						maintenanceHTML.append("<html lang=\"en\">\n<head>\n");
						maintenanceHTML.append("<title>Maintenance Mode Active</title>\n");
						maintenanceHTML.append("<meta charset=\"utf-8\">\n");
						maintenanceHTML.append("</head>\n<body>\n");
						maintenanceHTML.append("<h2>Maintenance Mode Active</h2>\n");
						maintenanceHTML.append(Settings.MaintenanceMessage.getValue());
						maintenanceHTML.append("\n</body>\n</html>\n");

						responseHeaders.add(HttpHeader.CONTENT_LENGTH, maintenanceHTML.length());

						try (OutputStream out = Response.asBufferedOutputStream(request, response)) {
							out.write(maintenanceHTML.toString().getBytes());
						}

						callback.succeeded();

						return true;
					}
				});

			} else {

				final ResourceHandler resourceHandler = new ResourceHandler() {

					@Override
					public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {

						final String target     = Request.getPathInContext(request);
						final Resource resolved = getBaseResource().resolve(target);

						if (!target.equals("/") && (resolved == null || !resolved.exists())) {

							// redirect and don't cache
							final HttpFields.Mutable headers = response.getHeaders();
							headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
							headers.add("Expires", (String) null);
							headers.add("Location", "/");

							response.setStatus(HttpServletResponse.SC_FOUND);

							callback.succeeded();

							return true;
						}

						return super.handle(request, response, callback);
					}
				};
				resourceHandler.setDirAllowed(false);

				final ResourceFactory factory = ResourceFactory.of(resourceHandler);
				final Resource baseResource   = factory.newResource(URI.create(resourceBase).normalize());
				resourceHandler.setWelcomeFiles("index.html");
				resourceHandler.setDirAllowed(false);

				resourceHandler.setBaseResource(baseResource);
				resourceHandler.setCacheControl("max-age=0");

				final ContextHandler contextHandler = new ContextHandler(contextPath);
				contextHandler.setHandler(resourceHandler);

				final RewriteHandler rewriteHandler = new RewriteHandler();
				rewriteHandler.setHandler(contextHandler);

				maintenanceServer.setHandler(rewriteHandler);
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
						new SslConnectionFactory(sslContextFactory, "http/1.1"),
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

	public int getAllocatedPort() {

		for (final Connector c : server.getConnectors()) {

			if (c instanceof ServerConnector s) {

				final int port = s.getLocalPort();

				return port;
			}
		}

		return 0;
	}

	public void reloadSSLCertificate() {

		if (sslContextFactory != null) {

			try {

				final String keyStorePath           = Settings.KeystorePath.getValue();
				final String keyStorePassword       = Settings.KeystorePassword.getValue();

				// in case path/password changed
				sslContextFactory.setKeyStorePath(keyStorePath);
				sslContextFactory.setKeyStorePassword(keyStorePassword);

				sslContextFactory.reload(new Consumer<SslContextFactory>() {
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

			map = new ConcurrentHashMap<>();
			stats.put(key, map);
		}

		return map;
	}

	// ----- interface StatsCallback -----
	public void recordStatsValue(final String key, final String source, final long value) {
		recordStatsValue(key, source, value, true);
	}

	public void recordStatsValue(final String key, final String source, final long value, final boolean aggregateOnly) {

		final Map<String, Stats> map = getRequestStats(key);
		Stats stats                  = map.get(source);

		if (stats == null) {

			stats = new Stats();
			map.put(source, stats);
		}

		stats.value(value, aggregateOnly);
	}


	// ----- interface Feature -----
	@Override
	public String getModuleName() {
		return "rest";
	}

	public SessionCache getSessionCache() {
		return sessionCache;
	}

	// ----- private methods -----
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

								final HttpServlet servlet = (HttpServlet)Class.forName(servletClassName).getDeclaredConstructor().newInstance();
								if (servlet instanceof HttpServiceServlet httpServiceServlet) {

									final StructrHttpServiceConfig cfg = httpServiceServlet.getConfig();
									if (cfg != null) {

										cfg.initializeFromSettings(servletName);
									}

									final ServletHolder servletHolder = new ServletHolder(servlet);
									((HttpServiceServlet) servlet).configureServletHolder(servletHolder);

									if (servletPath.endsWith("*")) {

										servlets.put(servletPath, servletHolder);

									} else {

										servlets.put(servletPath + "/*", servletHolder);
									}

									// callback for statistics
									httpServiceServlet.registerStatsCallback(this);
								}

							} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException nfex) {
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
						final HttpServiceLifecycleListener listener = (HttpServiceLifecycleListener) Class.forName(listenerClass).getDeclaredConstructor().newInstance();
						switch (event) {

							case Started:
								listener.serverStarted();
								break;

							case Stopped:
								listener.serverStopped();
								break;
						}

					} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException ex) {
						logger.error("Unable to send lifecycle event to listener " + listenerClass, ex);
					}
				}
			}
		}
	}
}

