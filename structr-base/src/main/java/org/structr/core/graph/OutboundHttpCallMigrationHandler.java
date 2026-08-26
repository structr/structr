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
package org.structr.core.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.MailTemplateTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reports calls to the outbound HTTP functions that still use the pre-7.0 signature.
 *
 * In 7.0 everything optional after the content type moved into an options object, so a call like
 * `POST(url, body, 'application/json', 'UTF-8')` now passes a string where an object is expected and
 * is rejected at runtime. This step finds those calls and says, per call, whether it could be
 * rewritten mechanically or needs a person to look at it. It changes nothing.
 *
 * The judgement is deliberately conservative: only literal arguments can be judged. As soon as an
 * argument is an expression, what it evaluates to is unknown, and rewriting it would risk moving a
 * value into the wrong option.
 */
public class OutboundHttpCallMigrationHandler {

	private static final Logger logger = LoggerFactory.getLogger(OutboundHttpCallMigrationHandler.class.getName());

	/** Verbs whose optional tail moved into the options object, with the index the tail starts at. */
	private static final Map<String, Integer> OPTIONS_INDEX = Map.of(
		"GET",            2,
		"HEAD",           1,
		"DELETE",         3,
		"POST",           3,
		"PUT",            3,
		"PATCH",          3,
		"POSTMultiPart",  2,
		"FETCH",          4
	);

	/** Where the content type sits, for the verbs whose content type used to change what the call does. */
	private static final Map<String, Integer> CONTENT_TYPE_INDEX = Map.of(
		"GET",   1,
		"POST",  2,
		"PUT",   2,
		"PATCH", 2,
		"FETCH", 3
	);

	private static final Pattern CALL = Pattern.compile("\\b(GET|HEAD|DELETE|POST|PUT|PATCH|POSTMultiPart|FETCH)\\s*\\(");

	public enum Verdict {

		/** already valid under 7.0 */
		UP_TO_DATE,

		/** the old arguments are literals and map onto the options object without judgement */
		AUTOMATIC,

		/** an argument is an expression, or its meaning depends on a value only known at runtime */
		MANUAL
	}

	public record Finding(String type, String id, String name, String property, String call, Verdict verdict, String reason) {}

	public static void execute() throws FrameworkException {

		final List<Finding> findings = new LinkedList<>();

		scan(findings, StructrTraits.SCHEMA_METHOD,   SchemaMethodTraitDefinition.SOURCE_PROPERTY);
		scan(findings, StructrTraits.SCHEMA_PROPERTY, SchemaPropertyTraitDefinition.READ_FUNCTION_PROPERTY);
		scan(findings, StructrTraits.SCHEMA_PROPERTY, SchemaPropertyTraitDefinition.WRITE_FUNCTION_PROPERTY);
		scan(findings, StructrTraits.MAIL_TEMPLATE,   MailTemplateTraitDefinition.TEXT_PROPERTY);
		scan(findings, StructrTraits.CONTENT,         "content");

		report(findings);
	}

	// ----- private methods -----
	private static void scan(final List<Finding> findings, final String type, final String propertyName) throws FrameworkException {

		final Traits traits = Traits.of(type);

		if (!traits.hasKey(propertyName)) {

			return;
		}

		final PropertyKey<String> key     = traits.key(propertyName);
		final PropertyKey<String> nameKey = traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

		try (final Tx tx = StructrApp.getInstance().tx()) {

			for (final NodeInterface node : StructrApp.getInstance().nodeQuery(type).getResultStream()) {

				final String source = node.getProperty(key);

				if (source != null && !source.isEmpty()) {

					for (final String call : findCalls(source)) {

						final Finding finding = assess(type, node.getUuid(), node.getProperty(nameKey), propertyName, call);

						if (finding.verdict() != Verdict.UP_TO_DATE) {

							findings.add(finding);
						}
					}
				}
			}

			tx.success();
		}
	}

	/** Every call to one of the HTTP functions in the given source, from the name to its closing brace. */
	public static List<String> findCalls(final String source) {

		final List<String> calls = new ArrayList<>();
		final Matcher matcher    = CALL.matcher(source);

		while (matcher.find()) {

			final int end = matchingBrace(source, matcher.end() - 1);

			if (end > 0) {

				calls.add(source.substring(matcher.start(), end + 1));
			}
		}

		return calls;
	}

	public static Finding assess(final String type, final String id, final String name, final String property, final String call) {

		final String verb        = call.substring(0, call.indexOf('(')).trim();
		final Integer optionsAt  = OPTIONS_INDEX.get(verb);
		final List<String> args  = splitArguments(call.substring(call.indexOf('(') + 1, call.length() - 1));

		// DELETE gained a body, so its options moved from the second position to the fourth. A call that
		// passes anything as the second argument now sends it as the request body.
		if ("DELETE".equals(verb) && args.size() == 2) {

			if (isObjectLiteral(args.get(1))) {

				return new Finding(type, id, name, property, call, Verdict.AUTOMATIC,
					"the options moved behind the new body and content type: DELETE(url, null, null, { ... })");
			}

			return new Finding(type, id, name, property, call, Verdict.MANUAL,
				"the second argument is the request body now, not the response content type. It used to decide whether the "
				+ "response was parsed as JSON, which is options.parseResponse today");
		}

		if (optionsAt == null) {

			return new Finding(type, id, name, property, call, Verdict.UP_TO_DATE, "not a verb whose signature changed");
		}

		final String contentType = literalContentType(verb, args);

		// application/octet-stream used to switch GET and POST to binary transport. It no longer does,
		// so such a call keeps working but silently returns a string instead of a stream.
		if ("GET".equals(verb) || "POST".equals(verb)) {

			if ("application/octet-stream".equals(contentType)) {

				// the key differs by verb: GET streams the RESPONSE, POST sends the BODY as a stream, and
				// an unknown option is refused, so naming the wrong one produces a call that fails
				final String key = "GET".equals(verb) ? "binaryResponse" : "binaryBody";

				return new Finding(type, id, name, property, call, Verdict.AUTOMATIC,
					"application/octet-stream no longer switches to binary transport: add { " + key + ": true }");
			}

			final Integer contentTypeAt = CONTENT_TYPE_INDEX.get(verb);

			if (contentTypeAt != null && args.size() > contentTypeAt && !isLiteral(args.get(contentTypeAt))) {

				return new Finding(type, id, name, property, call, Verdict.MANUAL,
					"the content type is an expression (" + args.get(contentTypeAt).trim() + "); if it can be "
					+ "application/octet-stream the call needs { " + ("GET".equals(verb) ? "binaryResponse" : "binaryBody")
					+ ": true }, and for GET whether the next argument is a selector or a user name depends on it");
			}
		}

		if (args.size() <= optionsAt) {

			return new Finding(type, id, name, property, call, Verdict.UP_TO_DATE, "no arguments beyond the new signature");
		}

		final List<String> tail = args.subList(optionsAt, args.size());

		// a single object literal in the options position is already the new form
		if (tail.size() == 1 && isObjectLiteral(tail.get(0))) {

			return new Finding(type, id, name, property, call, Verdict.UP_TO_DATE, "already uses an options object");
		}

		for (final String argument : tail) {

			if (!isLiteral(argument)) {

				return new Finding(type, id, name, property, call, Verdict.MANUAL,
					"argument " + (args.indexOf(argument) + 1) + " is an expression (" + argument.trim() + "), so its value is only known at runtime");
			}
		}

		return new Finding(type, id, name, property, call, Verdict.AUTOMATIC, describeRewrite(verb, contentType, tail));
	}

	private static String describeRewrite(final String verb, final String contentType, final List<String> tail) {

		final List<String> moved = new LinkedList<>();

		if ("POST".equals(verb) || "PUT".equals(verb) || "PATCH".equals(verb) || "FETCH".equals(verb)) {

			// charset, username, password, configMap in that order
			if (tail.size() >= 1) { moved.add("charset into the content type"); }
			if (tail.size() >= 2) { moved.add("username into the options object"); }
			if (tail.size() >= 3) { moved.add("password into the options object"); }
			if (tail.size() >= 4) { moved.add("configMap becomes the options object"); }

		} else if ("GET".equals(verb) && "text/html".equals(contentType)) {

			// with text/html the argument after the content type was a CSS selector, not a user name
			if (tail.size() >= 1) { moved.add("selector into the options object"); }
			if (tail.size() >= 2) { moved.add("further arguments need review"); }

		} else {

			if (tail.size() >= 1) { moved.add("username into the options object"); }
			if (tail.size() >= 2) { moved.add("password into the options object"); }
		}

		return "all arguments are literals: " + String.join(", ", moved);
	}

	/** The content type of the call if it is written out in the source, otherwise null. */
	private static String literalContentType(final String verb, final List<String> args) {

		final Integer index = CONTENT_TYPE_INDEX.get(verb);

		if (index == null || args.size() <= index || !isLiteral(args.get(index))) {

			return null;
		}

		final String literal = args.get(index).trim();

		return literal.substring(1, literal.length() - 1).trim();
	}

	private static void report(final List<Finding> findings) {

		if (findings.isEmpty()) {

			logger.info("MigrationService: no calls to the HTTP functions use the pre-7.0 signature.");

			return;
		}

		final Map<Verdict, Integer> counts = new LinkedHashMap<>();

		for (final Finding finding : findings) {

			counts.merge(finding.verdict(), 1, Integer::sum);
		}

		logger.warn("MigrationService: {} call(s) to the HTTP functions still use the pre-7.0 signature ({} could be rewritten mechanically, {} need review). "
			+ "They are NOT changed automatically. Since 7.0 the arguments after the content type are one options object, so these calls fail at runtime.",
			findings.size(), counts.getOrDefault(Verdict.AUTOMATIC, 0), counts.getOrDefault(Verdict.MANUAL, 0));

		for (final Finding finding : findings) {

			logger.warn("MigrationService:   [{}] {} {} ({}).{}: {} - {}",
				finding.verdict(), finding.type(), finding.name() != null ? "'" + finding.name() + "'" : "", finding.id(),
				finding.property(), finding.call(), finding.reason());
		}
	}

	/** The index of the brace closing the one at openIndex, ignoring braces inside strings. */
	public static int matchingBrace(final String source, final int openIndex) {

		int depth       = 0;
		char quote      = 0;

		for (int i = openIndex; i < source.length(); i++) {

			final char c = source.charAt(i);

			if (quote != 0) {

				if (c == '\\') { i++; } else if (c == quote) { quote = 0; }

				continue;
			}

			switch (c) {

				case '\'', '"', '`' -> quote = c;
				case '('           -> depth++;
				case ')'           -> { depth--; if (depth == 0) { return i; } }
				default            -> { }
			}
		}

		return -1;
	}

	/** Splits an argument list on the commas that separate arguments, not on those inside them. */
	public static List<String> splitArguments(final String arguments) {

		final List<String> result = new ArrayList<>();
		final StringBuilder buf   = new StringBuilder();
		int depth                 = 0;
		char quote                = 0;

		for (int i = 0; i < arguments.length(); i++) {

			final char c = arguments.charAt(i);

			if (quote != 0) {

				buf.append(c);

				if (c == '\\' && i + 1 < arguments.length()) { buf.append(arguments.charAt(++i)); } else if (c == quote) { quote = 0; }

				continue;
			}

			switch (c) {

				case '\'', '"', '`'      -> { quote = c; buf.append(c); }
				case '(', '{', '['       -> { depth++; buf.append(c); }
				case ')', '}', ']'       -> { depth--; buf.append(c); }
				case ','                 -> {

					if (depth == 0) {

						result.add(buf.toString().trim());
						buf.setLength(0);

					} else {

						buf.append(c);
					}
				}
				default                  -> buf.append(c);
			}
		}

		if (!buf.isEmpty() || !result.isEmpty()) {

			result.add(buf.toString().trim());
		}

		return result;
	}

	/** A single quoted string or an object literal, i.e. something whose value is visible in the source. */
	public static boolean isLiteral(final String argument) {

		final String trimmed = argument.trim();

		if (trimmed.length() < 2) {

			return false;
		}

		final char quote = trimmed.charAt(0);

		if (quote == '\'' || quote == '"') {

			// the closing quote has to be the LAST character, otherwise this is an expression such as
			// 'a' + b, where only the beginning looks like a literal
			for (int i = 1; i < trimmed.length(); i++) {

				final char c = trimmed.charAt(i);

				if (c == '\\') {

					i++;

				} else if (c == quote) {

					return i == trimmed.length() - 1;
				}
			}

			return false;
		}

		return isObjectLiteral(trimmed);
	}

	public static boolean isObjectLiteral(final String argument) {

		final String trimmed = argument.trim();

		return trimmed.startsWith("{") && trimmed.endsWith("}");
	}
}
