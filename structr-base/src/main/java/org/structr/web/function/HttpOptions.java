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
package org.structr.web.function;

import org.apache.http.ParseException;
import org.apache.http.entity.ContentType;
import org.structr.common.error.ArgumentTypeException;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The trailing options argument of the outbound HTTP functions.
 *
 * Everything optional lives here instead of in a positional tail, so a caller never has to pass nulls
 * to reach the last argument, and an unsupported key is visible rather than silently dropped between
 * two positions.
 *
 * StructrScript hands over its numbers as Double, so numeric values are read as Number.
 */
public class HttpOptions {

	private final Map<String, Object> options;

	private HttpOptions(final Map<String, Object> options) {

		this.options = options != null ? options : Collections.emptyMap();
	}

	/**
	 * Reads the options argument at the given position.
	 *
	 * Anything other than an object is rejected rather than ignored: until 7.0 this position held the
	 * charset, so a string here is a call that has not been migrated and would otherwise fail silently
	 * with the wrong encoding.
	 */
	public static HttpOptions from(final String functionName, final Object[] sources, final int index) {

		if (sources == null || sources.length <= index || sources[index] == null) {

			return new HttpOptions(null);
		}

		final Object source = sources[index];

		if (source instanceof Map map) {

			return new HttpOptions(map);
		}

		throw new ArgumentTypeException(functionName + "(): expected an options object as argument " + (index + 1)
			+ ", got " + source.getClass().getSimpleName() + " '" + source + "'. The charset is part of the content type now, "
			+ "for example '" + functionName + "(url, body, \"application/json; charset=ISO-8859-1\")'. Credentials and the "
			+ "remaining settings go into the options object, for example { username: \"u\", password: \"p\", timeout: 30 }.");
	}

	/** The charset named in a content type, or the given default. */
	public static String charsetOf(final String contentType, final String defaultCharset) {

		if (contentType != null && contentType.contains("charset=")) {

			try {

				final Charset charset = ContentType.parse(contentType).getCharset();
				if (charset != null) {

					return charset.toString();
				}

			} catch (ParseException | UnsupportedCharsetException e) {

				// fall through to the default: an unusable charset in the content type is not worth
				// failing the request over, and the content type itself is still sent as given
			}
		}

		return defaultCharset;
	}

	/** timeout and redirects in the shape HttpHelper expects, with the timeout in milliseconds. */
	public Map<String, Object> asRequestConfig() {

		final Map<String, Object> config = new LinkedHashMap<>();
		final Integer timeout            = getTimeoutMillis();

		if (timeout != null) {

			config.put("timeout", timeout);
		}

		if (options.get("redirects") instanceof Boolean redirects) {

			config.put("redirects", redirects);
		}

		return config;
	}

	public String getString(final String key) {

		final Object value = options.get(key);

		return value != null ? value.toString() : null;
	}

	public boolean getBoolean(final String key, final boolean defaultValue) {

		final Object value = options.get(key);

		return value instanceof Boolean b ? b : defaultValue;
	}

	/** Seconds in the options object, milliseconds on the wire. */
	public Integer getTimeoutMillis() {

		final Object value = options.get("timeout");

		return value instanceof Number n ? (int) (n.doubleValue() * 1000) : null;
	}

	/** The headers of the ActionContext with the ones from the options object merged over them. */
	public Map<String, String> mergeHeaders(final Map<String, String> contextHeaders) {

		final Map<String, String> merged = new LinkedHashMap<>();

		if (contextHeaders != null) {

			merged.putAll(contextHeaders);
		}

		if (options.get("headers") instanceof Map headers) {

			for (final Object key : headers.keySet()) {

				final Object value = headers.get(key);

				if (key != null && value != null) {

					merged.put(key.toString(), value.toString());
				}
			}
		}

		return merged;
	}
}
