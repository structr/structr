/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.ee10.servlets.DoSFilter;
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
import org.structr.api.config.Setting;
import org.structr.api.config.Settings;
import org.structr.api.config.StringMultiChoiceSetting;
import org.structr.api.service.*;
import org.structr.core.Services;
import org.structr.rest.auth.SessionHelper;
import org.structr.rest.common.MetricsFilter;
import org.structr.rest.common.Stats;
import org.structr.rest.common.StatsCallback;
import org.structr.rest.servlet.DocumentationServlet;
import org.structr.rest.servlet.UIExtensionsServlet;
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
import java.util.stream.Collectors;

@ServiceDependency(SchemaService.class)
@StopServiceForMaintenanceMode
@StartServiceInMaintenanceMode
public class HttpService implements RunnableService, StatsCallback {

	private static final Logger logger = LoggerFactory.getLogger(HttpService.class.getName());

	public static final StringMultiChoiceSetting UriComplianceAllowedViolations = (StringMultiChoiceSetting) new StringMultiChoiceSetting(Settings.serverGroup, "HTTP Settings", "httpservice.uricompliance.allowedviolations", "",
			new LinkedHashSet<>(Arrays.stream(UriCompliance.Violation.values())
										.map(Enum::toString)
										.toList()),
			"These are URI \"violations\", which may be allowed by the compliance mode.").setLongDescription("""
				These are actual violations of the RFC, as they represent additional requirements in excess of the strict compliance of <a href="https://datatracker.ietf.org/doc/html/rfc3986">RFC 3986</a>.
				Allowing these violations allows requests to violate the corresponding additional requirement.

				The main use cases for allowing some of these violations are the "URL Routing" feature for pages when URL variables are used.

				By default, no violations are allowed.

				The following violations can be useful when using the features mentioned above or when handling files with these special characters in their names:

				<ul>
					<li><code>AMBIGUOUS_EMPTY_SEGMENT</code> to allow empty URL segments "//"</li>
					<li><code>AMBIGUOUS_PATH_SEPARATOR</code> to allow using encoded slashes "%%2f"</li>
					<li><code>AMBIGUOUS_PATH_ENCODING</code> to allow using encoding percent signs "%%25"</li>
				</ul>

				These are all possible values:
				<dl>
					%s
				</dl>
				""".formatted(
						Arrays.stream(UriCompliance.Violation.values())
								.sorted(Comparator.comparing(UriCompliance.Violation::getName))
								.map(v -> "<dt><a href=\"%s\">%s</a></dt><dd>%s</dd>".formatted(v.getURL(), v.getName(), v.getDescription()))
								.collect(Collectors.joining("\n"))
				)
	);

	private enum LifecycleEvent {
		Started, Stopped
	}

	private final Map<String, Map<String, Stats>> stats = new ConcurrentHashMap<>();
	private ResourceHandler exportedResourceHandler     = null;
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

		if (server != null) {

			try {
				server.stop();

				while (server.isStopping() && !server.isStopped()) {
					// wait until server is stopped
					Thread.sleep(100);
				}

			} catch (Exception ex) {
				logger.warn("Exception while stopping Jetty: {}", ex.getMessage());
			}
		}

		if (maintenanceServer != null) {

			try {
				maintenanceServer.stop();

				while (maintenanceServer.isStopping() && !maintenanceServer.isStopped()) {
					// wait until server is stopped
					Thread.sleep(100);
				}

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

	public Server getServer() {
		return server;
	}

	public ResourceHandler getExportedResourceHandler() {
		return exportedResourceHandler;
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

		// Block TRACE verb globally instead of adding code to every servlet
		{
			servletContext.addFilter(new FilterHolder((request, response, chain) -> {

				if ("TRACE".equalsIgnoreCase(((HttpServletRequest) request).getMethod())) {

					((HttpServletResponse) response).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
					return;
				}

				chain.doFilter(request, response);

			}), "/*", EnumSet.of(DispatcherType.REQUEST));
		}

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

			// Locate the static UI resources. Precedence:
			//   1. src/main/resources/structr - running from a source checkout (Maven build output)
			//   2. ./structr (when it contains index.html) - optional filesystem override for local
			//      development without a rebuild, or ops/enterprise customization of the served files.
			//      The index.html sentinel is required so that an unrelated ./structr/docs directory
			//      (used by the documentation tooling) does not hijack UI serving.
			//   3. the 'structr' resources bundled inside the application jar, served from the class path
			//      (the default for a packaged distribution)
			final ResourceFactory factory = ResourceFactory.of(resourceHandler);
			final Path devResources       = Paths.get("src/main/resources/structr");
			final Path localResources     = Paths.get("structr");
			final Resource baseResource;

			if (Files.isDirectory(devResources)) {

				baseResource = factory.newResource(devResources);
				logger.info("Serving static resources from development source directory {}", devResources.toAbsolutePath());

			} else if (Files.isRegularFile(localResources.resolve("index.html"))) {

				baseResource = factory.newResource(localResources);
				logger.info("Serving static resources from local override directory {}", localResources.toAbsolutePath());

			} else {

				baseResource = factory.newClassLoaderResource("structr/");
				logger.info("Serving static resources from the application jar (class path)");
			}

			if (baseResource == null) {
				logger.error("Unable to locate static UI resources (no source dir, no ./structr override, and no 'structr' resources on the class path).");
			}

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

			exportedResourceHandler = resourceHandler;
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

			// configure per-servlet DoSFilter (no-op if disabled or main switch is off)
			configureDoSFilter(servletContext, servletHolder.getName(), path);
		}

		// only add metrics filter if metrics servlet is enabled
		if (Settings.Servlets.getValue("").contains(MetricsServlet.class.getSimpleName())) {
			servletContext.addFilter(MetricsFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
		}

		// docs
		servletContext.addServlet(DocumentationServlet.class, "/structr/docs/ontology/*");
		servletContext.addServlet(DocumentationServlet.class, "/structr/docs/ontology");

		// UI extension scripts contributed by modules
		servletContext.addServlet(UIExtensionsServlet.class, "/structr/js/module-extensions.js");

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

		final UriCompliance.Violation[] allowedViolations = Arrays.stream(UriComplianceAllowedViolations.getSelectedOptions()).filter(str -> {

			try {

				UriCompliance.Violation.valueOf(str);
				return true;

			} catch (IllegalArgumentException iae) {

				logger.error("Unable to start HTTP Service because of unsupported URI compliance violation: '{}'.\nPossible values are: {}", str, String.join(" ", UriComplianceAllowedViolations.getAvailableOptions()));

				System.exit(1);
				return false;
			}

		}).map(UriCompliance.Violation::valueOf).toArray(UriCompliance.Violation[]::new);

		final UriCompliance customUriComplianceMode = UriCompliance.RFC3986.with("Custom UriCompliance based on RFC3986 with allowed violations", allowedViolations);

		httpConfig.setUriCompliance(customUriComplianceMode);

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
	/**
	 * Dispatches to attachDoSFilter with the correct per-servlet Setting objects based on
	 * the servlet's short name. Only known servlets are covered; any servlet without a
	 * matching case is left without a DoSFilter.
	 */
	private void configureDoSFilter(final ServletContextHandler servletContext, final String servletName, final String path) {

		if (!Settings.RateLimiting.getValue()) {
			return;
		}

		switch (servletName) {

			case "JsonRestServlet":
				attachDoSFilter(servletContext, path, servletName,
					Settings.JsonRestDosEnabled, Settings.JsonRestDosMaxRequestsPerSec, Settings.JsonRestDosDelayMs,
					Settings.JsonRestDosMaxWaitMs, Settings.JsonRestDosThrottledRequests, Settings.JsonRestDosThrottleMs,
					Settings.JsonRestDosMaxRequestMs, Settings.JsonRestDosMaxIdleTrackerMs, Settings.JsonRestDosInsertHeaders,
					Settings.JsonRestDosRemotePort, Settings.JsonRestDosIpWhitelist, Settings.JsonRestDosManagedAttr,
					Settings.JsonRestDosTooManyCode);
				break;

			case "HtmlServlet":
				attachDoSFilter(servletContext, path, servletName,
					Settings.HtmlDosEnabled, Settings.HtmlDosMaxRequestsPerSec, Settings.HtmlDosDelayMs,
					Settings.HtmlDosMaxWaitMs, Settings.HtmlDosThrottledRequests, Settings.HtmlDosThrottleMs,
					Settings.HtmlDosMaxRequestMs, Settings.HtmlDosMaxIdleTrackerMs, Settings.HtmlDosInsertHeaders,
					Settings.HtmlDosRemotePort, Settings.HtmlDosIpWhitelist, Settings.HtmlDosManagedAttr,
					Settings.HtmlDosTooManyCode);
				break;

			case "CsvServlet":
				attachDoSFilter(servletContext, path, servletName,
					Settings.CsvDosEnabled, Settings.CsvDosMaxRequestsPerSec, Settings.CsvDosDelayMs,
					Settings.CsvDosMaxWaitMs, Settings.CsvDosThrottledRequests, Settings.CsvDosThrottleMs,
					Settings.CsvDosMaxRequestMs, Settings.CsvDosMaxIdleTrackerMs, Settings.CsvDosInsertHeaders,
					Settings.CsvDosRemotePort, Settings.CsvDosIpWhitelist, Settings.CsvDosManagedAttr,
					Settings.CsvDosTooManyCode);
				break;

			case "UploadServlet":
				attachDoSFilter(servletContext, path, servletName,
					Settings.UploadDosEnabled, Settings.UploadDosMaxRequestsPerSec, Settings.UploadDosDelayMs,
					Settings.UploadDosMaxWaitMs, Settings.UploadDosThrottledRequests, Settings.UploadDosThrottleMs,
					Settings.UploadDosMaxRequestMs, Settings.UploadDosMaxIdleTrackerMs, Settings.UploadDosInsertHeaders,
					Settings.UploadDosRemotePort, Settings.UploadDosIpWhitelist, Settings.UploadDosManagedAttr,
					Settings.UploadDosTooManyCode);
				break;

			case "ProxyServlet":
				attachDoSFilter(servletContext, path, servletName,
					Settings.ProxyDosEnabled, Settings.ProxyDosMaxRequestsPerSec, Settings.ProxyDosDelayMs,
					Settings.ProxyDosMaxWaitMs, Settings.ProxyDosThrottledRequests, Settings.ProxyDosThrottleMs,
					Settings.ProxyDosMaxRequestMs, Settings.ProxyDosMaxIdleTrackerMs, Settings.ProxyDosInsertHeaders,
					Settings.ProxyDosRemotePort, Settings.ProxyDosIpWhitelist, Settings.ProxyDosManagedAttr,
					Settings.ProxyDosTooManyCode);
				break;

			case "DeploymentServlet":
				attachDoSFilter(servletContext, path, servletName,
					Settings.DeploymentDosEnabled, Settings.DeploymentDosMaxRequestsPerSec, Settings.DeploymentDosDelayMs,
					Settings.DeploymentDosMaxWaitMs, Settings.DeploymentDosThrottledRequests, Settings.DeploymentDosThrottleMs,
					Settings.DeploymentDosMaxRequestMs, Settings.DeploymentDosMaxIdleTrackerMs, Settings.DeploymentDosInsertHeaders,
					Settings.DeploymentDosRemotePort, Settings.DeploymentDosIpWhitelist, Settings.DeploymentDosManagedAttr,
					Settings.DeploymentDosTooManyCode);
				break;

			case "FlowServlet":
				attachDoSFilter(servletContext, path, servletName,
					Settings.FlowDosEnabled, Settings.FlowDosMaxRequestsPerSec, Settings.FlowDosDelayMs,
					Settings.FlowDosMaxWaitMs, Settings.FlowDosThrottledRequests, Settings.FlowDosThrottleMs,
					Settings.FlowDosMaxRequestMs, Settings.FlowDosMaxIdleTrackerMs, Settings.FlowDosInsertHeaders,
					Settings.FlowDosRemotePort, Settings.FlowDosIpWhitelist, Settings.FlowDosManagedAttr,
					Settings.FlowDosTooManyCode);
				break;

			case "LoginServlet":
				attachDoSFilter(servletContext, path, servletName,
					Settings.LoginDosEnabled, Settings.LoginDosMaxRequestsPerSec, Settings.LoginDosDelayMs,
					Settings.LoginDosMaxWaitMs, Settings.LoginDosThrottledRequests, Settings.LoginDosThrottleMs,
					Settings.LoginDosMaxRequestMs, Settings.LoginDosMaxIdleTrackerMs, Settings.LoginDosInsertHeaders,
					Settings.LoginDosRemotePort, Settings.LoginDosIpWhitelist, Settings.LoginDosManagedAttr,
					Settings.LoginDosTooManyCode);
				break;

			case "LogoutServlet":
				attachDoSFilter(servletContext, path, servletName,
					Settings.LogoutDosEnabled, Settings.LogoutDosMaxRequestsPerSec, Settings.LogoutDosDelayMs,
					Settings.LogoutDosMaxWaitMs, Settings.LogoutDosThrottledRequests, Settings.LogoutDosThrottleMs,
					Settings.LogoutDosMaxRequestMs, Settings.LogoutDosMaxIdleTrackerMs, Settings.LogoutDosInsertHeaders,
					Settings.LogoutDosRemotePort, Settings.LogoutDosIpWhitelist, Settings.LogoutDosManagedAttr,
					Settings.LogoutDosTooManyCode);
				break;

			case "TokenServlet":
				attachDoSFilter(servletContext, path, servletName,
					Settings.TokenDosEnabled, Settings.TokenDosMaxRequestsPerSec, Settings.TokenDosDelayMs,
					Settings.TokenDosMaxWaitMs, Settings.TokenDosThrottledRequests, Settings.TokenDosThrottleMs,
					Settings.TokenDosMaxRequestMs, Settings.TokenDosMaxIdleTrackerMs, Settings.TokenDosInsertHeaders,
					Settings.TokenDosRemotePort, Settings.TokenDosIpWhitelist, Settings.TokenDosManagedAttr,
					Settings.TokenDosTooManyCode);
				break;

			case "EventSourceServlet":
				attachDoSFilter(servletContext, path, servletName,
					Settings.EventSourceDosEnabled, Settings.EventSourceDosMaxRequestsPerSec, Settings.EventSourceDosDelayMs,
					Settings.EventSourceDosMaxWaitMs, Settings.EventSourceDosThrottledRequests, Settings.EventSourceDosThrottleMs,
					Settings.EventSourceDosMaxRequestMs, Settings.EventSourceDosMaxIdleTrackerMs, Settings.EventSourceDosInsertHeaders,
					Settings.EventSourceDosRemotePort, Settings.EventSourceDosIpWhitelist, Settings.EventSourceDosManagedAttr,
					Settings.EventSourceDosTooManyCode);
				break;

			case "HealthCheckServlet":
				attachDoSFilter(servletContext, path, servletName,
					Settings.HealthCheckDosEnabled, Settings.HealthCheckDosMaxRequestsPerSec, Settings.HealthCheckDosDelayMs,
					Settings.HealthCheckDosMaxWaitMs, Settings.HealthCheckDosThrottledRequests, Settings.HealthCheckDosThrottleMs,
					Settings.HealthCheckDosMaxRequestMs, Settings.HealthCheckDosMaxIdleTrackerMs, Settings.HealthCheckDosInsertHeaders,
					Settings.HealthCheckDosRemotePort, Settings.HealthCheckDosIpWhitelist, Settings.HealthCheckDosManagedAttr,
					Settings.HealthCheckDosTooManyCode);
				break;

			case "HistogramServlet":
				attachDoSFilter(servletContext, path, servletName,
					Settings.HistogramDosEnabled, Settings.HistogramDosMaxRequestsPerSec, Settings.HistogramDosDelayMs,
					Settings.HistogramDosMaxWaitMs, Settings.HistogramDosThrottledRequests, Settings.HistogramDosThrottleMs,
					Settings.HistogramDosMaxRequestMs, Settings.HistogramDosMaxIdleTrackerMs, Settings.HistogramDosInsertHeaders,
					Settings.HistogramDosRemotePort, Settings.HistogramDosIpWhitelist, Settings.HistogramDosManagedAttr,
					Settings.HistogramDosTooManyCode);
				break;

			case "OpenAPIServlet":
				attachDoSFilter(servletContext, path, servletName,
					Settings.OpenAPIDosEnabled, Settings.OpenAPIDosMaxRequestsPerSec, Settings.OpenAPIDosDelayMs,
					Settings.OpenAPIDosMaxWaitMs, Settings.OpenAPIDosThrottledRequests, Settings.OpenAPIDosThrottleMs,
					Settings.OpenAPIDosMaxRequestMs, Settings.OpenAPIDosMaxIdleTrackerMs, Settings.OpenAPIDosInsertHeaders,
					Settings.OpenAPIDosRemotePort, Settings.OpenAPIDosIpWhitelist, Settings.OpenAPIDosManagedAttr,
					Settings.OpenAPIDosTooManyCode);
				break;

			case "MetricsServlet":
				attachDoSFilter(servletContext, path, servletName,
					Settings.MetricsDosEnabled, Settings.MetricsDosMaxRequestsPerSec, Settings.MetricsDosDelayMs,
					Settings.MetricsDosMaxWaitMs, Settings.MetricsDosThrottledRequests, Settings.MetricsDosThrottleMs,
					Settings.MetricsDosMaxRequestMs, Settings.MetricsDosMaxIdleTrackerMs, Settings.MetricsDosInsertHeaders,
					Settings.MetricsDosRemotePort, Settings.MetricsDosIpWhitelist, Settings.MetricsDosManagedAttr,
					Settings.MetricsDosTooManyCode);
				break;

			default:
				logger.debug("No DoSFilter settings defined for servlet '{}', skipping filter attachment", servletName);
				break;
		}
	}

	/**
	 * Attaches a Jetty DoSFilter to the given servlet path, configured from the supplied
	 * per-servlet settings. No filter is attached if the enabled setting is false.
	 */
	private void attachDoSFilter(final ServletContextHandler servletContext, final String path, final String servletName,
		final Setting<Boolean> enabled,
		final Setting<Integer> maxRequestsPerSec, final Setting<Integer> delayMs, final Setting<Integer> maxWaitMs,
		final Setting<Integer> throttledRequests, final Setting<Integer> throttleMs, final Setting<Integer> maxRequestMs,
		final Setting<Integer> maxIdleTrackerMs, final Setting<Boolean> insertHeaders,
		final Setting<Boolean> remotePort, final Setting<String> ipWhitelist, final Setting<Boolean> managedAttr,
		final Setting<Integer> tooManyCode) {

		if (!enabled.getValue()) {
			return;
		}

		final FilterHolder holder = new FilterHolder(DoSFilter.class);

		holder.setInitParameter("maxRequestsPerSec", maxRequestsPerSec.getValue().toString());
		holder.setInitParameter("delayMs",           delayMs.getValue().toString());
		holder.setInitParameter("maxWaitMs",         maxWaitMs.getValue().toString());
		holder.setInitParameter("throttledRequests", throttledRequests.getValue().toString());
		holder.setInitParameter("throttleMs",        throttleMs.getValue().toString());
		holder.setInitParameter("maxRequestMs",      maxRequestMs.getValue().toString());
		holder.setInitParameter("maxIdleTrackerMs",  maxIdleTrackerMs.getValue().toString());
		holder.setInitParameter("insertHeaders",     insertHeaders.getValue().toString());
		holder.setInitParameter("remotePort",        remotePort.getValue().toString());
		holder.setInitParameter("ipWhitelist",       ipWhitelist.getValue());
		holder.setInitParameter("managedAttr",       managedAttr.getValue().toString());
		holder.setInitParameter("tooManyCode",       tooManyCode.getValue().toString());

		servletContext.addFilter(holder, path, EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC));

		logger.info("Attached DoSFilter to servlet {} at path {}", servletName, path);
	}

	private Map<String, ServletHolder> collectServlets(final LicenseManager licenseManager) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		final Map<String, ServletHolder> servlets = new LinkedHashMap<>();
		final String[] selectedServlets = Settings.Servlets.getSelectedOptions();

		if (selectedServlets.length > 0) {

			for (String servletName : selectedServlets) {

				if (StringUtils.isNotBlank(servletName)) {

					final String servletClassName = Settings.getOrCreateStringSetting(servletName, "class").getValue();
					if (servletClassName != null) {

						final String servletPath = Settings.getOrCreateStringSetting(servletName, "path").getValue();
						if (servletPath != null) {

							try {

								final Class<?> servletClass = Class.forName(servletClassName);

								// Servlets may live in feature modules (e.g. structr.flow.module,
								// structr.pdf.module) that base does not 'requires'. Add a runtime read
								// edge so base can reflectively instantiate the servlet; the feature
								// module exports its servlet package.
								HttpService.class.getModule().addReads(servletClass.getModule());

								final HttpServlet servlet = (HttpServlet)servletClass.getDeclaredConstructor().newInstance();
								if (servlet instanceof HttpServiceServlet httpServiceServlet) {

									final StructrHttpServiceConfig cfg = httpServiceServlet.getConfig();
									if (cfg != null) {

										cfg.initializeFromSettings(servletName);
									}

									final ServletHolder servletHolder = new ServletHolder(servlet);
									servletHolder.setName(servletName);
									httpServiceServlet.configureServletHolder(servletHolder);

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

