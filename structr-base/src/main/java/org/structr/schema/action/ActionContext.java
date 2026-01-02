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
package org.structr.schema.action;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.graalvm.polyglot.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.config.Settings;
import org.structr.api.util.Iterables;
import org.structr.common.ContextStore;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.AdvancedMailContainer;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.Methods;
import org.structr.core.api.NamedArguments;
import org.structr.core.graph.NodeInterface;
import org.structr.core.script.Scripting;
import org.structr.core.script.polyglot.context.ContextFactory;
import org.structr.core.script.polyglot.wrappers.HttpSessionWrapper;
import org.structr.core.traits.Traits;
import org.structr.schema.parser.DatePropertyGenerator;

import java.io.IOException;
import java.util.*;

/**
 *
 *
 */
public class ActionContext {

	private static final Logger logger = LoggerFactory.getLogger(ActionContext.class.getName());

	// Regular members
	private Map<String, ContextFactory.LockedContext> scriptingContexts       = new HashMap<>();
	private final ContextStore temporaryContextStore                          = new ContextStore();
	private final StringBuilder outputBuffer                                  = new StringBuilder();
	private ErrorBuffer errorBuffer                                           = new ErrorBuffer();
	private Locale locale                                                     = Locale.getDefault();
	private AbstractMethod currentMethod                                      = null;
	private SecurityContext securityContext                                   = null;
	private Predicate predicate                                               = null;
	private boolean disableVerboseExceptionLogging                            = false;
	private boolean javaScriptContext                                         = false;

	public int level = 0;

	public ActionContext(final SecurityContext securityContext) {
		this(securityContext, null);
	}

	public ActionContext(final SecurityContext securityContext, final Map<String, Object> parameters) {

		this.securityContext = securityContext;

		if (this.securityContext != null) {

			this.locale = this.securityContext.getEffectiveLocale();

			if (parameters != null) {
				this.securityContext.getContextStore().setTemporaryParameters(parameters);
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

	public Object getReferencedProperty(final GraphObject entity, final String initialRefKey, final Object initialData, final int depth, final EvaluationHints hints, final int row, final int column) throws FrameworkException {

		// split
		final String[] refs  = StringUtils.split(initialRefKey, "!");
		final String refKey  = refs[0];
		final String[] parts = StringUtils.split(refKey, ".");
		Object _data         = initialData;
		String defaultValue  = null;

		if (refs.length > 1) {
			defaultValue = refs[1];
		}

		// walk through template parts
		for (int i = 0; i < parts.length; i++) {

			final String key = parts[i];

			if (_data instanceof NodeInterface n) {

				_data = n.evaluate(this, key, null, hints, row, column);

			} else if (_data instanceof GraphObject obj) {

				_data = obj.evaluate(this, key, null, hints, row, column);

			} else {

				_data = evaluate(entity, key, _data, null, i+depth, hints, row, column);
			}

			// stop evaluation on null
			if (_data == null) {
				break;
			}
		}

		if (_data == null && defaultValue != null) {
			return Function.numberOrString(defaultValue);
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

	public Map<String, Object> getRequestStore() {
		return getContextStore().getRequestStore();
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

	public void clearHeaders() {
		getContextStore().clearHeaders();
	}

	public Map<String, String> getHeaders() {
		return getContextStore().getHeaders();
	}

	public void setValidateCertificates(final boolean validate) {
		getContextStore().setValidateCertificates(validate);
	}

	public boolean isValidateCertificates() {
		return getContextStore().isValidateCertificates();
	}

	public AdvancedMailContainer getAdvancedMailContainer() {
		return getContextStore().getAdvancedMailContainer();
	}

	public Object evaluate(final GraphObject entity, final String key, final Object data, final String defaultValue, final int depth, final EvaluationHints hints, final int row, final int column) throws FrameworkException {

		// report usage for toplevel keys only
		if (data == null) {

			// report key as used to identify unresolved keys later
			hints.reportUsedKey(key, row, column);
		}

		Object value = getContextStore().getConstant(key);
		if (this.temporaryContextStore.getConstant(key) != null) {
			hints.reportExistingKey(key);
			value = this.temporaryContextStore.getConstant(key);
		} else if (this.temporaryContextStore.hasConstant(key)) {
			hints.reportExistingKey(key);
			value = null;
		} else {

			if (value == null) {

				// special Request handling
				if (data instanceof HttpServletRequest) {

					value = ((HttpServletRequest) data).getParameterValues(key);
					if (value != null) {

						hints.reportExistingKey(key);
						if (((String[]) value).length == 1) {

							value = ((String[]) value)[0];

						} else {

							value = Arrays.asList((String[]) value);
						}
					}
				}

				// HttpSession
				if (data instanceof HttpSessionWrapper sessionWrapper) {

					if (StringUtils.isNotBlank(key)) {

						hints.reportExistingKey(key);

						// use "user." prefix to separate user and system data
						value = sessionWrapper.getMember(key);
					}
				}

				// special handling of maps..
				if (data instanceof Map) {

					hints.reportExistingKey(key);
					value = ((Map) data).get(key);
				}

				if (data != null) {

					if (data instanceof GraphObject graphObject) {

						value = graphObject.evaluate(this, key, defaultValue, hints, row, column);

					} else if (data instanceof Traits traits) {

						final AbstractMethod method = Methods.resolveMethod(traits, key);
						if (method != null) {

							hints.reportExistingKey(key);

							final ContextStore contextStore = getContextStore();
							final Map<String, Object> temp  = contextStore.getTemporaryParameters();
							final Arguments arguments       = NamedArguments.fromMap(temp);

							return method.execute(securityContext, null, arguments, hints);
						}

					} else {

						switch (key) {

							case "size":
								if (data instanceof Collection) {
									hints.reportExistingKey(key);
									return ((Collection) data).size();
								}
								if (data instanceof Iterable) {
									hints.reportExistingKey(key);
									return Iterables.count((Iterable) data);
								}
								if (data.getClass().isArray()) {
									hints.reportExistingKey(key);
									return ((Object[]) data).length;
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
								hints.reportExistingKey(key);
								return securityContext.getRequest();

							case "session":
								if (securityContext.getRequest() != null) {
									hints.reportExistingKey(key);
									return new HttpSessionWrapper(new ActionContext(securityContext), securityContext.getRequest().getSession(false));
								}
								break;

							case "baseUrl":
							case "base_url":
								hints.reportExistingKey(key);
								return getBaseUrl(securityContext.getRequest());

							case "applicationRootPath":
								hints.reportExistingKey(key);
								return Settings.ApplicationRootPath.getValue();

							case "me":
								hints.reportExistingKey(key);
								return securityContext.getUser(false);

							case "depth":
								hints.reportExistingKey(key);
								return securityContext.getSerializationDepth() - 1;

						}

						// 2. keywords which require a request
						final HttpServletRequest request = securityContext.getRequest();
						if (request != null) {

							switch (key) {

								case "host":
									hints.reportExistingKey(key);
									return request.getServerName();

								case "ip":
									hints.reportExistingKey(key);
									return request.getLocalAddr();

								case "port":
									hints.reportExistingKey(key);
									return request.getServerPort();

								case "pathInfo":
								case "path_info":
									hints.reportExistingKey(key);
									return request.getPathInfo();

								case "queryString":
								case "query_string":
									hints.reportExistingKey(key);
									return request.getQueryString();

								case "parameterMap":
								case "parameter_map":
									hints.reportExistingKey(key);
									return request.getParameterMap();

								case "remoteAddress":
								case "remote_address":
									hints.reportExistingKey(key);
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
										hints.reportExistingKey(key);
										return response.getOutputStream();

									} catch (IOException ioex) {
										logger.warn("", ioex);
									}
									return null;
								}

								case "statusCode":
								case "status_code":
									hints.reportExistingKey(key);
									return response.getStatus();
							}
						}
					}

					// keywords that do not need a security context
					switch (key) {

						case "now":
							hints.reportExistingKey(key);
							return this.isJavaScriptContext() ? new Date() : DatePropertyGenerator.format(new Date(), Settings.DefaultDateFormat.getValue());

						case "this":
							hints.reportExistingKey(key);
							return entity;

						case "locale":
							hints.reportExistingKey(key);
							return locale != null ? locale.toString() : null;

						case "tenantIdentifier":
						case "tenant_identifier":
							hints.reportExistingKey(key);
							return Settings.TenantIdentifier.getValue();

						case "applicationStore":
						case "application_store":
							hints.reportExistingKey(key);
							return Services.getInstance().getApplicationStore();

						default:

							// Do the (slow) class check only if key value starts with uppercase character or could have a package path
							if (StringUtils.isNotEmpty(key) && (Character.isUpperCase(key.charAt(0)) || StringUtils.contains(key, "."))) {

								if (Traits.exists(key)) {

									hints.reportExistingKey(key);

									return Traits.of(key);
								}
							}

							break;
					}
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
			final Map pm    = getRequestStore();

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

	public void print(final Object[] objects, final Object caller) {

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

	public void setPredicate(final Predicate predicate) {
		this.predicate = predicate;
	}

	public Predicate getPredicate() {
		return predicate;
	}

	public ContextStore getContextStore() {
		return this.securityContext.getContextStore();
	}

	public ContextFactory.LockedContext getScriptingContext(final String language) {
		return scriptingContexts.get(language);
	}

	public void putScriptingContext(final String language, final ContextFactory.LockedContext context) {
		scriptingContexts.put(language, context);
	}

	public void removeScriptingContextByValue(final ContextFactory.LockedContext context) {
		scriptingContexts.entrySet().removeIf(entry -> entry.getValue().equals(context));
	}

	public void setScriptingContexts(final Map<String, ContextFactory.LockedContext> contexts) {
		scriptingContexts = contexts;
	}

	public Map<String,ContextFactory.LockedContext> getScriptingContexts() {
		return scriptingContexts;
	}

	public boolean isRenderContext() {
		return false;
	}

	public void setCurrentMethod(final AbstractMethod currentMethod) {
		this.currentMethod = currentMethod;
	}

	public AbstractMethod getCurrentMethod() {
		return currentMethod;
	}

	// ----- public static methods -----
	public static String getBaseUrl() {
		return getBaseUrl(null);
	}

	public static String getBaseUrl(final HttpServletRequest request) {
		return getBaseUrl(request, false);
	}

	public static String getBaseUrl(final HttpServletRequest request, final boolean forceConfigForPort) {

		final String baseUrlOverride = Settings.BaseUrlOverride.getValue();

		if (StringUtils.isNotEmpty(baseUrlOverride)) {
			return baseUrlOverride;
		}

		final StringBuilder sb = new StringBuilder("http");

		final Boolean httpsEnabled       = Settings.HttpsEnabled.getValue();
		final String name                = (request != null) ? request.getServerName() : Settings.ApplicationHost.getValue();
		final Integer port               = (request != null && forceConfigForPort != true) ? request.getServerPort() : ((httpsEnabled) ? Settings.getSettingOrMaintenanceSetting(Settings.HttpsPort).getValue() : Settings.getSettingOrMaintenanceSetting(Settings.HttpPort).getValue());

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
}