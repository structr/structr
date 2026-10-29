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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a script (a method, a function property, the content and function query of an HTML
 * element) for what it connects to: the types it names in find() and its relatives, the
 * methods it calls, the external hosts it sends HTTP requests to and whether it sends mail.
 * Type and method references only decide which artifacts take part in the map, what a script
 * does to the data is the business of the CRUD analysis; a request to an external host and
 * sending mail are edges of their own (requests, mails), since nothing else shows them.
 */
public class ScriptScanner {

	public static final String MAIL_ID = "ext:mail";

	static final Pattern TYPE_CALL      = Pattern.compile("\\b(?:find|find_privileged|findPrivileged|search|create|getOrCreate|get_or_create|delete|search_fulltext|merge_unique|mergeUnique)\\s*\\(\\s*['\"]([A-Z][A-Za-z0-9_]*)['\"]");
	static final Pattern STATIC_CALL    = Pattern.compile("\\$\\.([A-Z][A-Za-z0-9_]*)\\.([a-zA-Z_][A-Za-z0-9_]*)\\s*\\(");
	static final Pattern CALL           = Pattern.compile("(?:\\$\\.|\\bcall\\(\\s*['\"]|\\b)([a-zA-Z_][A-Za-z0-9_]*)\\s*\\(");
	// $.GET() in JavaScript, Structr.GET() in older JavaScript, GET() in StructrScript
	static final Pattern URL            = Pattern.compile("(?:\\$\\.|\\bStructr\\.|\\b)(?:GET|POST|PUT|DELETE|PATCH|HEAD)\\s*\\(\\s*['\"`](https?://[^'\"`,)\\s]+)");
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

		matcher = URL.matcher(source);

		while (matcher.find()) {

			final String host = hostOf(matcher.group(1));

			if (host != null && host.contains(".") && !host.contains("$")) {

				map.addNode("ext:" + host, ApplicationMap.EXTERNAL, host);
				map.addReference(origin, "ext:" + host, "requests", 1.0);
			}
		}

		if (MAIL.matcher(source).find()) {

			map.addNode(MAIL_ID, ApplicationMap.EXTERNAL, "Mail");
			map.addReference(origin, MAIL_ID, "mails", 1.0);
		}
	}

	// ----- private methods -----
	private static String hostOf(final String url) {

		final String rest = url.substring(url.indexOf("://") + 3);
		final String host = rest.split("[/?#:]", 2)[0].toLowerCase();

		return host.isEmpty() ? null : host;
	}
}
