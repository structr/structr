/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.parser.Functions;
import org.structr.core.property.DateProperty;
import org.structr.schema.parser.DatePropertyParser;

/**
 *
 * @author Christian Morgner
 */
public class ActionContext {

	private static final Logger logger = Logger.getLogger(ActionContext.class.getName());

	protected SecurityContext securityContext = null;
	protected Map<String, String> headers     = new HashMap<>();
	protected Map<String, Object> constants   = new HashMap<>();
	protected Map<String, Object> tmpStore    = new HashMap<>();
	protected Map<Integer, Integer> counters  = new HashMap<>();
	protected ErrorBuffer errorBuffer         = new ErrorBuffer();
	protected StringBuilder outputBuffer      = new StringBuilder();
	protected Locale locale                   = Locale.getDefault();
	private boolean javaScriptContext         = false;

	public ActionContext(final SecurityContext securityContext) {
		this(securityContext, null);
	}

	public ActionContext(final SecurityContext securityContext, final Map<String, Object> parameters) {

		if (parameters != null) {
			this.tmpStore.putAll(parameters);
		}

		this.securityContext = securityContext;

		this.locale = securityContext.getEffectiveLocale();
	}

	public ActionContext(final ActionContext other) {

		this.tmpStore        = other.tmpStore;
		this.counters        = other.counters;
		this.errorBuffer     = other.errorBuffer;
		this.constants       = other.constants;
		this.securityContext = other.securityContext;
		this.locale          = other.locale;
	}

	public ActionContext(final ActionContext other, final Object data) {

		this(other);

		init(data);
	}

	public ActionContext(final SecurityContext securityContext, final Object data) {

		this.securityContext = securityContext;

		this.locale = securityContext.getEffectiveLocale();

		init(data);
	}

	private void init(final Object data) {

		constants.put("data", data);
		constants.put("true", true);
		constants.put("false", false);
	}

	public SecurityContext getSecurityContext() {
		return securityContext;
	}

	public boolean returnRawValue() {
		return false;
	}

	public Object getReferencedProperty(final GraphObject entity, final String refKey, final Object initialData) throws FrameworkException {

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

			_data = evaluate(entity, key, _data, defaultValue);
		}

		return _data;
	}

	public void raiseError(final String type, final ErrorToken errorToken) {
		errorBuffer.add(type, errorToken);
	}

	public ErrorBuffer getErrorBuffer() {
		return errorBuffer;
	}

	public boolean hasError() {
		return errorBuffer.hasError();
	}

	public void incrementCounter(final int level) {

		Integer value = counters.get(level);
		if (value == null) {

			value = 0;
		}

		counters.put(level, value+1);
	}

	public int getCounter(final int level) {

		Integer value = counters.get(level);
		if (value == null) {

			return 0;
		}

		return value;
	}

	public void resetCounter(final int level) {
		counters.put(level, 0);
	}

	public void store(final String key, final Object value) {
		tmpStore.put(key, value);
	}

	public Object retrieve(final String key) {
		return tmpStore.get(key);
	}

	public Map<String, Object> getAllVariables () {
		return tmpStore;
	}

	public void addHeader(final String key, final String value) {
		headers.put(key, value);
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public Object evaluate(final GraphObject entity, final String key, final Object data, final String defaultValue) throws FrameworkException {

		Object value = constants.get(key);
		if (value == null) {

			// special HttpServletRequest handling
			if (data instanceof HttpServletRequest) {
				value = ((HttpServletRequest)data).getParameter(key);
			}

			// special handling of maps..
			if (data instanceof Map) {
				value = ((Map)data).get(key);
			}

			if (data != null) {

				if (data instanceof GraphObject) {

					value = ((GraphObject)data).evaluate(securityContext, key, defaultValue);

				} else {

					switch (key) {

						case "size":
							if (data instanceof Collection) {
								return ((Collection)data).size();
							}
							if (data.getClass().isArray()) {
								return ((Object[])data).length;
							}
							break;
					}

				}

			} else {

				// "data-less" keywords to start the evaluation chain
				switch (key) {

					case "request":
						return securityContext.getRequest();

					case "now":
						return DatePropertyParser.format(new Date(), DateProperty.DEFAULT_FORMAT);

					case "me":
						return securityContext.getUser(false);

					case "element":
					case "this":
						return entity;

					case "locale":
						return locale != null ? locale.toString() : null;
				}

			}

		}

		if (value == null && defaultValue != null) {
			return Functions.numberOrString(defaultValue);
		}

		return value;
	}

	public void print(final Object... objects) {

		for (final Object obj : objects) {

			if (obj != null) {

				outputBuffer.append(obj.toString());
			}
		}
	}

	public void clear() {
		outputBuffer.setLength(0);
	}

	public String getOutput() {
		return outputBuffer.toString();
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

	public String getJavascriptLibraryCode() {

		final StringBuilder buf = new StringBuilder();
		final App app           = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final JavaScriptSource jsLibraryFile : app.nodeQuery(JavaScriptSource.class).and(JavaScriptSource.useAsJavascriptLibrary, true).getAsList()) {

				final String contentType = jsLibraryFile.getContentType();
				if (contentType != null) {

					final String lowerCaseContentType = contentType.toLowerCase();
					if ("text/javascript".equals(lowerCaseContentType) || "application/javascript".equals(lowerCaseContentType)) {

						buf.append(jsLibraryFile.getJavascriptLibraryCode());

					} else {

						logger.log(Level.INFO, "Ignoring file {0} for use as a Javascript library, content type {1} not allowed. Use text/javascript or application/javascript.", new Object[] { jsLibraryFile.getName(), contentType } );
					}

				} else {

					logger.log(Level.INFO, "Ignoring file {0} for use as a Javascript library, content type not set. Use text/javascript or application/javascript.", new Object[] { jsLibraryFile.getName(), contentType } );
				}
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return buf.toString();
	}
}
