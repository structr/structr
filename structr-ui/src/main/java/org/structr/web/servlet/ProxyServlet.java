/**
 * Copyright (C) 2010-2016 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.servlet;

import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.ThreadLocalMatcher;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.rest.service.HttpService;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.service.StructrHttpServiceConfig;
import org.structr.web.auth.UiAuthenticator;
import org.structr.rest.common.HttpHelper;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.Page;

//~--- classes ----------------------------------------------------------------
/**
 * Servlet for proxy requests.
 *
 *
 *
 */
public class ProxyServlet extends HttpServlet implements HttpServiceServlet {

	private static final Logger logger = LoggerFactory.getLogger(ProxyServlet.class.getName());

	public static final String CONFIRM_REGISTRATION_PAGE = "/confirm_registration";
	public static final String RESET_PASSWORD_PAGE       = "/reset-password";
	public static final String POSSIBLE_ENTRY_POINTS_KEY = "possibleEntryPoints";
	public static final String DOWNLOAD_AS_FILENAME_KEY  = "filename";
	public static final String RANGE_KEY                 = "range";
	public static final String DOWNLOAD_AS_DATA_URL_KEY  = "as-data-url";
	public static final String CONFIRM_KEY_KEY           = "key";
	public static final String TARGET_PAGE_KEY           = "target";
	public static final String ERROR_PAGE_KEY            = "onerror";

	public static final String CUSTOM_RESPONSE_HEADERS      = "HtmlServlet.customResponseHeaders";
	public static final String OBJECT_RESOLUTION_PROPERTIES = "HtmlServlet.resolveProperties";

	private static final String defaultCustomResponseHeaders = "Strict-Transport-Security:max-age=60,"
				+ "X-Content-Type-Options:nosniff,"
				+ "X-Frame-Options:SAMEORIGIN,"
				+ "X-XSS-Protection:1;mode=block";
	private static List<String> customResponseHeaders = Collections.EMPTY_LIST;

	private static final ThreadLocalMatcher threadLocalUUIDMatcher = new ThreadLocalMatcher("[a-fA-F0-9]{32}");
	private static final ExecutorService threadPool = Executors.newCachedThreadPool();

	private final StructrHttpServiceConfig config = new StructrHttpServiceConfig();
	private final Set<String> possiblePropertyNamesForEntityResolving   = new LinkedHashSet<>();

	private boolean isAsync = false;


	@Override
	public StructrHttpServiceConfig getConfig() {
		return config;
	}

	public ProxyServlet() {

		String customResponseHeadersString = Services.getBaseConfiguration().getProperty(CUSTOM_RESPONSE_HEADERS);

		if (StringUtils.isBlank(customResponseHeadersString)) {

			customResponseHeadersString = defaultCustomResponseHeaders;
		}

		if (StringUtils.isNotBlank(customResponseHeadersString)) {
			customResponseHeaders = Arrays.asList(customResponseHeadersString.split("[ ,]+"));
		}

		// resolving properties
		final String resolvePropertiesSource = StructrApp.getConfigurationValue(OBJECT_RESOLUTION_PROPERTIES, "AbstractNode.name");
		for (final String src : resolvePropertiesSource.split("[, ]+")) {

			final String name = src.trim();
			if (StringUtils.isNotBlank(name)) {

				possiblePropertyNamesForEntityResolving.add(name);
			}
		}

		this.isAsync = Services.parseBoolean(Services.getBaseConfiguration().getProperty(HttpService.ASYNC), true);
	}

	@Override
	public void destroy() {
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) {

		final Authenticator auth = getConfig().getAuthenticator();
		
		SecurityContext securityContext;
		String content;

		if (auth == null) {
			
			final String errorMessage = "No authenticator class found. Check log for 'Missing authenticator key " + this.getClass().getSimpleName() + ".authenticator'";
			logger.error(errorMessage);
			
			try {
				final ServletOutputStream out = response.getOutputStream();
				content = errorPage(new Throwable(errorMessage));
				IOUtils.write(content, out);

			} catch (IOException ex) {
				logger.error("Could not write to response", ex);
			}
			
			return;
		}		

		try {

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				securityContext = auth.initializeAndExamineRequest(request, response);
				tx.success();
			}

			// Ensure access mode is frontend
			securityContext.setAccessMode(AccessMode.Frontend);
			
			String address = request.getParameter("url");
			final URI url  = URI.create(address);

			String proxyUrl      = request.getParameter("proxyUrl");
			String proxyUsername = request.getParameter("proxyUsername");
			String proxyPassword = request.getParameter("proxyPassword");
			String authUsername  = request.getParameter("authUsername");
			String authPassword  = request.getParameter("authPassword");
			String cookie        = request.getParameter("cookie");

			final Principal user = securityContext.getCachedUser();

			if (user != null && StringUtils.isBlank(proxyUrl)) {
				proxyUrl      = user.getProperty(User.proxyUrl);
				proxyUsername = user.getProperty(User.proxyUsername);
				proxyPassword = user.getProperty(User.proxyPassword);
			}
			
			content = HttpHelper.get(address, authUsername, authPassword, proxyUrl, proxyUsername, proxyPassword, cookie, Collections.EMPTY_MAP).replace("<head>", "<head>\n  <base href=\"" + url + "\">");


		} catch (Throwable t) {

			logger.error("Exception while processing request", t);
			content = errorPage(t);
		}

		try {
			final ServletOutputStream out = response.getOutputStream();
			IOUtils.write(content, out);
		} catch (IOException ex) {
			logger.error("Could not write to response", ex);
		}
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		doGet(request, response);

	}

	@Override
	protected void doHead(final HttpServletRequest request, final HttpServletResponse response) {

		try {
			String path = request.getPathInfo();


		} catch (Throwable t) {

			logger.error("Exception while processing request", t);
			UiAuthenticator.writeInternalServerError(response);
		}
	}

	@Override
	protected void doOptions(final HttpServletRequest request, final HttpServletResponse response) {

		final Authenticator auth = config.getAuthenticator();

		try {

			response.setContentLength(0);
			response.setHeader("Allow", "GET,HEAD,OPTIONS");

		} catch (Throwable t) {

			logger.error("Exception while processing request", t);
			UiAuthenticator.writeInternalServerError(response);
		}
	}


	//~--- set methods ----------------------------------------------------
	public static void setNoCacheHeaders(final HttpServletResponse response) {

		response.setHeader("Cache-Control", "private, max-age=0, s-maxage=0, no-cache, no-store, must-revalidate"); // HTTP 1.1.
		response.setHeader("Pragma", "no-cache, no-store"); // HTTP 1.0.
		response.setDateHeader("Expires", 0);

	}

	private static void setCustomResponseHeaders(final HttpServletResponse response) {

		for (final String header : customResponseHeaders) {

			final String[] keyValuePair = header.split("[ :]+");
			response.setHeader(keyValuePair[0], keyValuePair[1]);

			logger.debug("Set custom response header: {} {}", new Object[]{keyValuePair[0], keyValuePair[1]});

		}

	}

	private static boolean notModifiedSince(final HttpServletRequest request, HttpServletResponse response, final AbstractNode node, final boolean dontCache) {

		boolean notModified = false;
		final Date lastModified = node.getLastModifiedDate();

		// add some caching directives to header
		// see http://weblogs.java.net/blog/2007/08/08/expires-http-header-magic-number-yslow
		final DateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		response.setHeader("Date", httpDateFormat.format(new Date()));

		final Calendar cal = new GregorianCalendar();
		final Integer seconds = node.getProperty(Page.cacheForSeconds);

		if (!dontCache && seconds != null) {

			cal.add(Calendar.SECOND, seconds);
			response.setHeader("Cache-Control", "max-age=" + seconds + ", s-maxage=" + seconds + "");
			response.setHeader("Expires", httpDateFormat.format(cal.getTime()));

		} else {

			if (!dontCache) {
				response.setHeader("Cache-Control", "no-cache, must-revalidate, proxy-revalidate");
			} else {
				response.setHeader("Cache-Control", "private, no-cache, no-store, max-age=0, s-maxage=0, must-revalidate, proxy-revalidate");
			}

		}

		if (lastModified != null) {

			final Date roundedLastModified = DateUtils.round(lastModified, Calendar.SECOND);
			response.setHeader("Last-Modified", httpDateFormat.format(roundedLastModified));

			final String ifModifiedSince = request.getHeader("If-Modified-Since");

			if (StringUtils.isNotBlank(ifModifiedSince)) {

				try {

					Date ifModSince = httpDateFormat.parse(ifModifiedSince);

					// Note that ifModSince has not ms resolution, so the last digits are always 000
					// That requires the lastModified to be rounded to seconds
					if ((ifModSince != null) && (roundedLastModified.equals(ifModSince) || roundedLastModified.before(ifModSince))) {

						notModified = true;

						response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
						response.setHeader("Vary", "Accept-Encoding");

					}

				} catch (ParseException ex) {
					logger.warn("Could not parse If-Modified-Since header", ex);
				}

			}

		}

		return notModified;
	}


	private String errorPage(final Throwable t) {
		return "<html><head><title>Error in Structr Proxy</title></head><body><h1>Error in Proxy</h1><p>" + t.getMessage() + "</p>\n<!--" + ExceptionUtils.getStackTrace(t) + "--></body></html>";
	}
	
}
