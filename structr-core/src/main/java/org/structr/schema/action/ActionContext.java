/**
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.schema.action;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.config.Settings;
import org.structr.api.util.Iterables;
import org.structr.common.AdvancedMailContainer;
import org.structr.common.ContextStore;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.schema.parser.DatePropertyParser;

/**
 *
 *
 */
public class ActionContext {

	private static final Logger logger = LoggerFactory.getLogger(ActionContext.class.getName());

	// cache is not static => library cache is per request
	private final Map<String, String> libraryCache = new HashMap<>();
	protected SecurityContext securityContext      = null;
	protected Predicate predicate                  = null;
	protected ErrorBuffer errorBuffer              = new ErrorBuffer();
	protected StringBuilder outputBuffer           = new StringBuilder();
	protected Locale locale                        = Locale.getDefault();
	private boolean javaScriptContext              = false;
	private ContextStore temporaryContextStore     = new ContextStore();
	private boolean disableVerboseExceptionLogging = false;

	public ActionContext(final SecurityContext securityContext) {
		this(securityContext, null);
	}

	public ActionContext(final SecurityContext securityContext, final Map<String, Object> parameters) {

		this.securityContext = securityContext;

		if (this.securityContext != null) {

			this.locale = this.securityContext.getEffectiveLocale();

			if (parameters != null) {
				this.securityContext.getContextStore().setParameters(parameters);
			}
		}
	}

	public ActionContext(final ActionContext other) {
		this.errorBuffer     = other.errorBuffer;
		this.securityContext = other.securityContext;
		this.locale          = other.locale;
	}

	public SecurityContext getSecurityContext() {
		return securityContext;
	}

	public void setSecurityContext(final SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	public boolean returnRawValue() {
		return false;
	}

	public void setDisableVerboseExceptionLogging(final boolean disable) {
		this.disableVerboseExceptionLogging = disable;
	}

	public boolean getDisableVerboseExceptionLogging() {
		return this.disableVerboseExceptionLogging;
	}

	public Object getConstant(final String name) {
		return this.temporaryContextStore.getConstant(name);
	}

	public void setConstant(final String name, final Object data) {
		this.temporaryContextStore.setConstant(name, data);
	}

	public Object getReferencedProperty(final GraphObject entity, final String refKey, final Object initialData, final int depth) throws FrameworkException {

		final String DEFAULT_VALUE_SEP = "!";
		final String[] parts           = refKey.split("[\\.]+");
		Object _data                   = initialData;

		// walk through template parts
		for (int i = 0; i < parts.length; i++) {

			String key          = parts[i];
			String defaultValue = null;


			if (StringUtils.contains(key, DEFAULT_VALUE_SEP)) {

				String[] ref = StringUtils.split(key, DEFAULT_VALUE_SEP);
				key          = ref[0];

				if (ref.length > 1) {
					defaultValue = ref[1];
				}
			}

			_data = evaluate(entity, key, _data, defaultValue, i+depth);
		}

		return _data;
	}

	public void raiseError(final int code, final ErrorToken errorToken) {
		errorBuffer.add(errorToken);
		errorBuffer.setStatus(code);
	}

	public ErrorBuffer getErrorBuffer() {
		return errorBuffer;
	}

	public boolean hasError() {
		return errorBuffer.hasError();
	}

	public void incrementCounter(final int level) {
		getContextStore().incrementCounter(level);
	}

	public int getCounter(final int level) {
		return getContextStore().getCounter(level);
	}

	public void resetCounter(final int level) {
		getContextStore().resetCounter(level);
	}

	public void store(final String key, final Object value) {
		getContextStore().store(key, value);
	}

	public Object retrieve(final String key) {
		return getContextStore().retrieve(key);
	}

	public Map<String, Object> getAllVariables () {
		return getContextStore().getAllVariables();
	}

	public void addTimer(final String key) {
		getContextStore().addTimer(key);
	}

	public Date getTimer(final String key) {
		return getContextStore().getTimer(key);
	}

	public void addHeader(final String key, final String value) {
		getContextStore().addHeader(key, value);
	}

	public void removeHeader(final String key) {
		getContextStore().removeHeader(key);
	}

	public Map<String, String> getHeaders() {
		return getContextStore().getHeaders();
	}

	public AdvancedMailContainer getAdvancedMailContainer() {
		return getContextStore().getAdvancedMailContainer();
	}

	public Object evaluate(final GraphObject entity, final String key, final Object data, final String defaultValue, final int depth) throws FrameworkException {

		Object value = getContextStore().getConstant(key);
		if (this.temporaryContextStore.getConstant(key) != null) {
			value = this.temporaryContextStore.getConstant(key);
		}

		if (value == null) {

			// special HttpServletRequest handling
			if (data instanceof HttpServletRequest) {
				value = ((HttpServletRequest)data).getParameterValues(key);

				if (value != null) {
					if (((String[]) value).length == 1) {
						value = ((String[]) value)[0];
					} else {
						value = Arrays.asList((String[]) value);
					}
				}
			}

			// special handling of maps..
			if (data instanceof Map) {
				value = ((Map)data).get(key);
			}

			if (data != null) {

				if (data instanceof GraphObject) {

					value = ((GraphObject)data).evaluate(this, key, defaultValue);

				} else {

					switch (key) {

						case "size":
							if (data instanceof Collection) {
								return ((Collection)data).size();
							}
							if (data instanceof Iterable) {
								return Iterables.count((Iterable)data);
							}
							if (data.getClass().isArray()) {
								return ((Object[])data).length;
							}
							break;
					}

				}

			} else {

				// keywords that need an existing security context
				if (securityContext != null) {

					// "data-less" keywords to start the evaluation chain

					// 1. keywords without special handling
					switch (key) {

						case "request":
							return securityContext.getRequest();

						case "baseUrl":
						case "base_url":
							return getBaseUrl(securityContext.getRequest());

						case "me":
							return securityContext.getUser(false);

						case "depth":
							return securityContext.getSerializationDepth() - 1;

					}

					// 2. keywords which require a request
					final HttpServletRequest request = securityContext.getRequest();

					if (request != null) {

						switch (key) {

							case "host":
								return request.getServerName();

							case "port":
								return request.getServerPort();

							case "pathInfo":
							case "path_info":
								return request.getPathInfo();

							case "queryString":
							case "query_string":
								return request.getQueryString();

							case "parameterMap":
							case "parameter_map":
								return request.getParameterMap();

							case "remoteAddress":
							case "remote_address":
								return getRemoteAddr(request);
						}
					}

					// 3. keywords which require a response
					final HttpServletResponse response = securityContext.getResponse();

					if (response != null) {

						switch (key) {

							case "response": {

								try {
									// return output stream of HTTP response for streaming
									return response.getOutputStream();

								} catch (IOException ioex) {
									logger.warn("", ioex);
								}
								return null;
							}

							case "statusCode":
							case "status_code":
								return response.getStatus();
						}
					}
				}

				// keywords that do not need a security context
				switch (key) {

					case "now":
						return this.isJavaScriptContext() ? new Date() : DatePropertyParser.format(new Date(), Settings.DefaultDateFormat.getValue());

					case "this":
						return entity;

					case "locale":
						return locale != null ? locale.toString() : null;

					case "tenantIdentifier":
					case "tenant_identifier":
						return Settings.TenantIdentifier.getValue();
				}
			}
		}

		if (value == null && defaultValue != null) {
			return Function.numberOrString(defaultValue);
		}

		return value;
	}

	public String getRequestInfoForVerboseJavaScriptExceptionLog() {

		final HttpServletRequest request = securityContext.getRequest();

		if (request != null) {

			final StringBuilder sb = new StringBuilder("Path = ");
			sb.append(request.getPathInfo());

			final String qs = request.getQueryString();
			final Map pm    = getAllVariables();

			if (qs != null) {

				sb.append("?");
				sb.append(qs);
			}

			if (!pm.isEmpty()) {

				sb.append(" –– Parameter Map = ");
				sb.append(pm.toString());
			}

			return sb.toString();
		}

		return null;
	}

	public static String getBaseUrl () {
		return getBaseUrl(null);
	}

	public static String getBaseUrl (final HttpServletRequest request) {

		final String baseUrlOverride = Settings.BaseUrlOverride.getValue();

		if (StringUtils.isNotEmpty(baseUrlOverride)) {
			return baseUrlOverride;
		}

		final StringBuilder sb = new StringBuilder("http");

		final Boolean httpsEnabled       = Settings.HttpsEnabled.getValue();
		final String name                = (request != null) ? request.getServerName() : Settings.ApplicationHost.getValue();
		final Integer port               = (request != null) ? request.getServerPort() : ((httpsEnabled) ? Settings.HttpsPort.getValue() : Settings.HttpPort.getValue());

		if (httpsEnabled) {
			sb.append("s");
		}

		sb.append("://");
		sb.append(name);

		// we need to specify the port if (protocol = HTTPS and port != 443 OR protocol = HTTP and port != 80)
		if ( (httpsEnabled && port != 443) || (!httpsEnabled && port != 80) ) {
			sb.append(":").append(port);
		}

		return sb.toString();
	}

	public static String getRemoteAddr(HttpServletRequest request) {

		final String remoteAddress = request.getHeader("X-FORWARDED-FOR");

		if (remoteAddress == null) {
			return request.getRemoteAddr();
		}

		return remoteAddress;
	}

	public void print(final Object... objects) {

		for (final Object obj : objects) {

			if (obj != null) {

				outputBuffer.append(Scripting.formatToDefaultDateOrString(obj));
			}
		}
	}

	public void clear() {
		outputBuffer.setLength(0);
	}

	public String getOutput() {
		final String out = outputBuffer.toString();
		// clear buffer after fetching the output
		clear();
		return out;
	}

	/**
	 * @return the javaScriptContext
	 */
	public boolean isJavaScriptContext() {
		return javaScriptContext;
	}

	/**
	 * @param javaScriptContext the javaScriptContext to set
	 */
	public void setJavaScriptContext(boolean javaScriptContext) {
		this.javaScriptContext = javaScriptContext;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(final Locale locale) {
		this.locale = locale;
	}

	public String getJavascriptLibraryCode(String fileName) {

		synchronized (libraryCache) {

			String cachedSource = libraryCache.get(fileName);
			if (cachedSource == null) {

				final StringBuilder buf = new StringBuilder();
				final App app           = StructrApp.getInstance();

				try (final Tx tx = app.tx()) {

					final List<JavaScriptSource> jsFiles = app.nodeQuery(JavaScriptSource.class)
							.and(JavaScriptSource.name, fileName)
							.and(StructrApp.key(JavaScriptSource.class, "useAsJavascriptLibrary"), true)
							.getAsList();

					if (jsFiles.isEmpty()) {
						logger.warn("No JavaScript library file found with fileName: {}", fileName );
					} else if (jsFiles.size() > 1) {
						logger.warn("Multiple JavaScript library files found with fileName: {}. This may cause problems!", fileName );
					}

					for (final JavaScriptSource jsLibraryFile : jsFiles) {

						final String contentType = jsLibraryFile.getContentType();
						if (contentType != null) {

							final String lowerCaseContentType = contentType.toLowerCase();
							if ("text/javascript".equals(lowerCaseContentType) || "application/javascript".equals(lowerCaseContentType)) {

								buf.append(jsLibraryFile.getJavascriptLibraryCode());

							} else {

								logger.info("Ignoring file {} for use as a Javascript library, content type {} not allowed. Use text/javascript or application/javascript.", new Object[] { jsLibraryFile.getName(), contentType } );
							}

						} else {

							logger.info("Ignoring file {} for use as a Javascript library, content type not set. Use text/javascript or application/javascript.", new Object[] { jsLibraryFile.getName(), contentType } );
						}
					}

					tx.success();

				} catch (FrameworkException fex) {
					logger.warn("", fex);
				}

				cachedSource = buf.toString();
				libraryCache.put(fileName, cachedSource);
			}

			return cachedSource;
		}
	}

	public void setPredicate(final Predicate predicate) {
		this.predicate = predicate;
	}

	public Predicate getPredicate() {
		return predicate;
	}

	public ContextStore getContextStore() {
		return this.securityContext.getContextStore();
	}
}