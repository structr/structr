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

import java.nio.charset.StandardCharsets;

import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
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

	public static final String USERNAME              = "username";
	public static final String PASSWORD              = "password";
	public static final String HEADERS               = "headers";
	public static final String TIMEOUT               = "timeout";
	public static final String REDIRECTS             = "redirects";
	public static final String VALIDATE_CERTIFICATES = "validateCertificates";
	/**
	 * Two directions, two names. "binary" meant sending the body as a binary stream on POST and
	 * streaming the response on GET, so one key stood for opposite things depending on the verb.
	 */
	public static final String BINARY_BODY          = "binaryBody";
	public static final String BINARY_RESPONSE      = "binaryResponse";
	public static final String PARSE_RESPONSE        = "parseResponse";
	public static final String SELECTOR              = "selector";
	public static final String PREEMPTIVE            = "preemptive";

	private final Map<String, Object> options;

	/** Meaningful for every verb: they say how the request is made, not what it means. */
	private static final Set<String> TRANSPORT = Set.of(USERNAME, PASSWORD, PREEMPTIVE, HEADERS, TIMEOUT, REDIRECTS, VALIDATE_CERTIFICATES);

	private HttpOptions(final Map<String, Object> options) {

		this.options = options != null ? options : Collections.emptyMap();
	}

	/**
	 * Rejects keys the function does not act on.
	 *
	 * An option that is accepted and then ignored cannot be observed from a script: no error, no log,
	 * nothing in the response. A timeout that does nothing surfaces much later as a hung call. Naming
	 * the key and the function turns that into something the caller can fix.
	 */
	public HttpOptions accepting(final String functionName, final String... semanticKeys) {

		final Set<String> allowed = new LinkedHashSet<>(TRANSPORT);

		Collections.addAll(allowed, semanticKeys);

		for (final String key : options.keySet()) {

			if (!allowed.contains(key)) {

				throw new ArgumentTypeException(functionName + "(): unknown option '" + key + "'. " + functionName
					+ " accepts " + String.join(", ", new TreeSet<>(allowed)) + ".");
			}
		}

		return this;
	}

	/**
	 * The options object, wherever the caller actually put it.
	 *
	 * The options follow an OPTIONAL argument, so the natural call that omits it - POST(url, body, { ... })
	 * - leaves the object one position early. Read positionally that map becomes the content type via
	 * toString(), the request goes out with a Content-Type of "{timeout=5}" and the options are silently
	 * dropped: no error, and nothing in the response says why the timeout had no effect.
	 *
	 * A Map is never a valid content type, so finding one in an earlier slot is unambiguous.
	 */
	public static HttpOptions fromAnyOf(final String functionName, final Object[] sources, final int index, final int... earlierSlots) {

		for (final int slot : earlierSlots) {

			if (sources != null && sources.length > slot && sources[slot] instanceof Map) {

				return new HttpOptions((Map)sources[slot]);
			}
		}

		return from(functionName, sources, index);
	}

	/** Whether the argument at the given position is an options object rather than a value. */
	public static boolean isOptionsAt(final Object[] sources, final int index) {

		return sources != null && sources.length > index && sources[index] instanceof Map;
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

			config.put(TIMEOUT, timeout);
		}

		if (options.get(REDIRECTS) instanceof Boolean redirects) {

			config.put(REDIRECTS, redirects);
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

		final Object value = options.get(TIMEOUT);

		return value instanceof Number n ? (int) (n.doubleValue() * 1000) : null;
	}

	/** The headers of the ActionContext with the ones from the options object merged over them. */
	public Map<String, String> mergeHeaders(final Map<String, String> contextHeaders) {

		final Map<String, String> merged = new LinkedHashMap<>();

		// Preemptive basic auth: HttpClient otherwise sends the credentials only after the server has
		// answered 401 with a challenge, and a server that just expects them gets an anonymous request.
		// Set as a header rather than through the client, so it is on the very first request.
		if (getBoolean(PREEMPTIVE, false)) {

			final String username = getString(USERNAME);
			final String password = getString(PASSWORD);

			if (username != null && password != null) {

				merged.put("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8)));
			}
		}

		if (contextHeaders != null) {

			merged.putAll(contextHeaders);
		}

		if (options.get(HEADERS) instanceof Map headers) {

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
