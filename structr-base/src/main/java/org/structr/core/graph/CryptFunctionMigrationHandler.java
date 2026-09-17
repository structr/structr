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
import org.structr.core.function.CryptFunction;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.MailTemplateTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reports calls to encrypt() and decrypt() that were written before the key derivation scheme became
 * the first argument (ticket 1601).
 *
 * <p>Reporting only, and deliberately so. The two schemes are not interchangeable: rewriting a call to
 * {@code legacy} would keep every existing ciphertext readable and keep the unsalted MD5 derivation that
 * makes a stolen one worth guessing at, while rewriting it to {@code aes-gcm-pbkdf2} would produce a
 * function that can no longer read what it wrote last week. Which of the two a given call wants is a
 * question about the data behind it, and this class cannot see that data - so it names the places and
 * leaves the decision where it belongs.
 */
public class CryptFunctionMigrationHandler {

	private static final Logger logger = LoggerFactory.getLogger(CryptFunctionMigrationHandler.class.getName());

	private static final Pattern CALL = Pattern.compile("\\b(encrypt|decrypt)\\s*\\(");

	public record Finding(String type, String id, String name, String property, String call) {}

	public static void execute() throws FrameworkException {

		final List<Finding> findings = new LinkedList<>();

		scan(findings, StructrTraits.SCHEMA_METHOD,   SchemaMethodTraitDefinition.SOURCE_PROPERTY);
		scan(findings, StructrTraits.SCHEMA_PROPERTY, SchemaPropertyTraitDefinition.READ_FUNCTION_PROPERTY);
		scan(findings, StructrTraits.SCHEMA_PROPERTY, SchemaPropertyTraitDefinition.WRITE_FUNCTION_PROPERTY);
		scan(findings, StructrTraits.MAIL_TEMPLATE,   MailTemplateTraitDefinition.TEXT_PROPERTY);
		scan(findings, StructrTraits.CONTENT,         "content");

		report(findings);
	}

	/**
	 * Every call to encrypt() or decrypt() in the given source, from the name to its closing brace.
	 */
	public static List<String> findCalls(final String source) {

		final List<String> calls = new ArrayList<>();
		final Matcher matcher    = CALL.matcher(source);

		while (matcher.find()) {

			final int end = OutboundHttpCallMigrationHandler.matchingBrace(source, matcher.end() - 1);
			if (end > 0) {

				calls.add(source.substring(matcher.start(), end + 1));
			}
		}

		return calls;
	}

	/**
	 * Whether the call still passes its value where the scheme now goes.
	 *
	 * <p>Decided by the first argument alone: a quoted literal naming one of the known schemes is a call
	 * that has been updated, anything else is one that has not. A call that computes the scheme rather
	 * than writing it out reads as not updated, which is the safe way round - it is named in a report
	 * that a person then looks at, and a false name costs a glance where a missed one costs a value
	 * nobody can decrypt any more.
	 */
	public static boolean usesOldSignature(final String call) {

		final String firstArgument = firstArgumentOf(call);
		if (firstArgument == null) {

			return true;
		}

		final String unquoted = unquote(firstArgument);

		return unquoted == null || !CryptFunction.isKnownScheme(unquoted);
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

						if (usesOldSignature(call)) {

							findings.add(new Finding(type, node.getUuid(), node.getProperty(nameKey), propertyName, call));
						}
					}
				}
			}

			tx.success();
		}
	}

	private static void report(final List<Finding> findings) {

		if (findings.isEmpty()) {

			logger.info("MigrationService: no calls to encrypt() or decrypt() use the pre-7.0 signature.");

			return;
		}

		logger.warn("MigrationService: {} call(s) to encrypt() or decrypt() still use the pre-7.0 signature and fail at runtime. Since 7.0 the first argument names the key derivation scheme, one of: {}. "
			+ "Use '{}' for values that have to stay readable - it is what these calls did - and '{}' for everything else, which derives the key with PBKDF2 and a salt of its own. "
			+ "Switching a value from one to the other means decrypting it with the old scheme and encrypting it again with the new one; nothing was changed here.",
			findings.size(), CryptFunction.getKnownSchemes(), CryptFunction.SCHEME_LEGACY, CryptFunction.SCHEME_PBKDF2);

		for (final Finding finding : findings) {

			logger.warn("MigrationService:   {} {} ({}), {}: {}", finding.type(), finding.name(), finding.id(), finding.property(), finding.call());
		}
	}

	private static String firstArgumentOf(final String call) {

		final int open = call.indexOf('(');
		if (open < 0) {

			return null;
		}

		int depth = 0;

		for (int i = open; i < call.length(); i++) {

			final char c = call.charAt(i);
			if (c == '(') {

				depth++;

			} else if (c == ')') {

				depth--;

				if (depth == 0) {

					return call.substring(open + 1, i).trim();
				}

			} else if (c == ',' && depth == 1) {

				return call.substring(open + 1, i).trim();
			}
		}

		return null;
	}

	private static String unquote(final String value) {

		if (value.length() > 1 && (value.startsWith("'") && value.endsWith("'") || value.startsWith("\"") && value.endsWith("\""))) {

			return value.substring(1, value.length() - 1);
		}

		return null;
	}
}
