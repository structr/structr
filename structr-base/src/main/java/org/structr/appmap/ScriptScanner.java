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
package org.structr.appmap;

import org.structr.api.config.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a script (a method, a function property, the content and function query of an HTML
 * element) for what it connects to: the types it names in find() and its relatives, the
 * methods it calls, and the systems outside Structr it talks to through built-in functions.
 * Type and method references only decide which artifacts take part in the map, what a script
 * does to the data is the business of the CRUD analysis; the integrations are edges of their
 * own, since nothing else shows them: requests to a web host (GET and its relatives, FETCH),
 * to the geocoding and translation services; queries to a foreign database (jdbc, mongodb,
 * remoteCypher); executes for a script configured for exec and execBinary; mails for sending
 * mail. An integration whose target the script names as a literal is one external service
 * per target, one whose target is computed one per kind of integration, so the dependency
 * stays visible. An external service is named after what the application does with it and
 * where: call structr.com, query db.example.com (JDBC), send e-mail via the configured
 * SMTP server.
 */
public class ScriptScanner {

	public static final String MAIL_ID = "ext:mail";

	// the translation providers by the key the translate function takes
	private static final Map<String, String> TRANSLATION_PROVIDERS = Map.of("google", "Google", "deepl", "DeepL");

	static final Pattern TYPE_CALL      = Pattern.compile("\\b(?:find|find_privileged|findPrivileged|search|create|getOrCreate|get_or_create|delete|search_fulltext|merge_unique|mergeUnique)\\s*\\(\\s*['\"]([A-Z][A-Za-z0-9_]*)['\"]");
	static final Pattern STATIC_CALL    = Pattern.compile("\\$\\.([A-Z][A-Za-z0-9_]*)\\.([a-zA-Z_][A-Za-z0-9_]*)\\s*\\(");
	static final Pattern CALL           = Pattern.compile("(?:\\$\\.|\\bcall\\(\\s*['\"]|\\b)([a-zA-Z_][A-Za-z0-9_]*)\\s*\\(");
	// a built-in function that talks to a system outside Structr: $.GET() in JavaScript, Structr.GET() in older JavaScript,
	// GET() in StructrScript, but not a method of the same name on another object, like exec() of a regular expression
	static final Pattern INTEGRATION    = Pattern.compile("(?:\\$\\.|\\bStructr\\.|(?<![\\w.$]))(GET|POST|PUT|DELETE|PATCH|HEAD|FETCH|jdbc|mongodb|remoteCypher|remote_cypher|geocode|translate|exec|execBinary|exec_binary)\\s*\\(");
	static final Pattern LITERAL        = Pattern.compile("^(['\"`])([^'\"`]*)\\1$");
	static final Pattern MAIL           = Pattern.compile("\\b(send_?html_?mail|send_?plaintext_?mail|mail_?begin)\\b", Pattern.CASE_INSENSITIVE);

	public static void scan(final ApplicationMap map, final String source, final String origin) {

		if (source == null || source.isEmpty() || origin == null) {
			return;
		}

		final SchemaIndex schema = map.getSchema();
		Matcher matcher          = TYPE_CALL.matcher(source);

		while (matcher.find()) {
			map.connect(origin, schema.getTypeIds().get(matcher.group(1)));
		}

		matcher = STATIC_CALL.matcher(source);

		while (matcher.find()) {

			final String method = schema.getTypeMethod(matcher.group(1), matcher.group(2));

			map.connect(origin, method != null ? method : schema.getTypeIds().get(matcher.group(1)));
		}

		matcher = CALL.matcher(source);

		while (matcher.find()) {
			map.connect(origin, schema.getGlobalMethod(matcher.group(1)));
		}

		matcher = INTEGRATION.matcher(source);

		while (matcher.find()) {
			integration(map, origin, matcher.group(1), arguments(source, matcher.end()));
		}

		if (MAIL.matcher(source).find()) {

			final String host = Settings.SmtpHost.getValue();

			map.addNode(MAIL_ID, ApplicationMap.EXTERNAL, host == null || host.isBlank() ? "Send e-mail" : "Send e-mail via " + host);
			map.addReference(origin, MAIL_ID, "mails", 1.0);
		}
	}

	// ----- private methods -----
	private static void integration(final ApplicationMap map, final String origin, final String function, final List<String> arguments) {

		switch (function) {

			case "GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "FETCH" -> {

				final String url = literal(arguments, 0);

				if (url == null) {

					connect(map, origin, "ext:http", "Call computed URLs", "requests");

				} else if (url.startsWith("http://") || url.startsWith("https://")) {

					final String host = hostOf(url);

					// a host without a domain is the server itself or one in the same network, not an integration
					if (host != null && host.contains(".")) {
						connect(map, origin, "ext:" + host, "Call " + host, "requests");
					}
				}

				// a relative URL is a request to Structr itself
			}

			case "jdbc"                           -> database(map, origin, literal(arguments, 0), "jdbc", "JDBC");
			case "mongodb"                        -> database(map, origin, literal(arguments, 0), "mongodb", "MongoDB");
			case "remoteCypher", "remote_cypher"  -> database(map, origin, literal(arguments, 0), "cypher", "Neo4j");

			case "geocode" -> {

				// the provider is configured, its class is called like GoogleGeoCodingProvider
				final String provider = Settings.GeocodingProvider.getValue();
				final String name     = provider.substring(provider.lastIndexOf('.') + 1).replace("GeoCodingProvider", "");

				connect(map, origin, "ext:geocoding", name.isEmpty() ? "Geocode addresses" : "Geocode via " + name, "requests");
			}

			case "translate" -> {

				// the provider is the fourth argument, Google without it
				final String provider = arguments.size() > 3 ? literal(arguments, 3) : "google";

				if (provider == null) {

					connect(map, origin, "ext:translation", "Translate via a computed provider", "requests");

				} else {

					final String key = provider.toLowerCase();

					connect(map, origin, "ext:translation:" + key, "Translate via " + TRANSLATION_PROVIDERS.getOrDefault(key, provider), "requests");
				}
			}

			case "exec", "execBinary", "exec_binary" -> {

				// execBinary takes the output first, then the key of the script in structr.conf
				final String key = literal(arguments, "exec".equals(function) ? 0 : 1);

				if (key == null) {

					connect(map, origin, "ext:exec", "Run computed scripts", "executes");

				} else {

					connect(map, origin, "ext:exec:" + key, "Run " + key, "executes");
				}
			}
		}
	}

	// a foreign database, one per host its URL names; a URL without a host, like an Oracle thin URL, as it is written
	private static void database(final ApplicationMap map, final String origin, final String url, final String kind, final String label) {

		if (url == null) {

			connect(map, origin, "ext:" + kind, "Query computed " + label + " databases", "queries");

			return;
		}

		final String host = url.contains("://") ? hostOf(url) : url;

		if (host != null) {
			connect(map, origin, "ext:" + kind + ":" + host, "Query " + host + " (" + label + ")", "queries");
		}
	}

	private static void connect(final ApplicationMap map, final String origin, final String id, final String name, final String kind) {

		map.addNode(id, ApplicationMap.EXTERNAL, name);
		map.addReference(origin, id, kind, 1.0);
	}

	// the arguments of a call, as written, from just behind its opening parenthesis to the closing one; commas inside
	// brackets, braces, nested calls and strings do not separate arguments
	static List<String> arguments(final String source, final int start) {

		final List<String> arguments = new ArrayList<>();
		final StringBuilder current  = new StringBuilder();
		char quote                   = 0;
		int depth                    = 0;

		for (int i = start; i < source.length(); i++) {

			final char c = source.charAt(i);

			if (quote != 0) {

				current.append(c);

				if (c == '\\' && i + 1 < source.length()) {

					current.append(source.charAt(++i));

				} else if (c == quote) {

					quote = 0;
				}

				continue;
			}

			switch (c) {

				case '\'', '"', '`' -> {

					quote = c;
					current.append(c);
				}

				case '(', '[', '{' -> {

					depth++;
					current.append(c);
				}

				case ')', ']', '}' -> {

					if (depth == 0) {

						if (!current.isEmpty() || !arguments.isEmpty()) {
							arguments.add(current.toString().trim());
						}

						return arguments;
					}

					depth--;
					current.append(c);
				}

				case ',' -> {

					if (depth == 0) {

						arguments.add(current.toString().trim());
						current.setLength(0);

					} else {

						current.append(c);
					}
				}

				default -> current.append(c);
			}
		}

		// the script ends inside the call
		arguments.add(current.toString().trim());

		return arguments;
	}

	// the text of an argument that is a plain string literal, or null for anything computed, including a template string
	// with an expression
	static String literal(final List<String> arguments, final int index) {

		if (index >= arguments.size()) {
			return null;
		}

		final Matcher matcher = LITERAL.matcher(arguments.get(index));

		if (!matcher.matches() || matcher.group(2).contains("${")) {
			return null;
		}

		return matcher.group(2);
	}

	// the host of a URL, without the credentials it may carry
	private static String hostOf(final String url) {

		final String rest      = url.substring(url.indexOf("://") + 3);
		final String authority = rest.split("[/?#]", 2)[0];
		final String host      = authority.substring(authority.lastIndexOf('@') + 1).split(":", 2)[0].toLowerCase();

		return host.isEmpty() ? null : host;
	}
}
