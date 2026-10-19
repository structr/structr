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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.RequestParameters;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.MailTemplateTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.structr.web.entity.File;
import org.structr.web.traits.definitions.FileTraitDefinition;
import org.structr.web.traits.definitions.dom.ContentTraitDefinition;

import static org.structr.core.graph.OutboundHttpCallMigrationHandler.isLiteral;
import static org.structr.core.graph.OutboundHttpCallMigrationHandler.isObjectLiteral;
import static org.structr.core.graph.OutboundHttpCallMigrationHandler.matchingBrace;
import static org.structr.core.graph.OutboundHttpCallMigrationHandler.splitArguments;

/**
 * Reports, and in apply mode rewrites, calls to toExcel() that still use the pre-7.0 signature.
 *
 * Before 7.0 the function took one collection and up to six positional arguments:
 * `toExcel(nodes, propertiesOrView [, includeHeader, localizeHeader, headerLocalizationDomain, maxCellLength, overflowMode])`.
 * Since 7.0 it takes pairs of a collection and an options object, one pair per sheet:
 * `toExcel(data_1, { view: ... }, data_2, { properties: [...] }, ...)`. An old call now passes a string or a
 * list where the options object is expected and is rejected at runtime.
 *
 * Every old position has exactly one key in the options object, so the rewrite does not need to guess
 * what a value means - with one exception: the second argument was a view name if it was a string and a
 * list of property names if it was a list, and those are two different keys now. That can only be decided
 * when the argument is written out in the source.
 *
 * The other limit is StructrScript. An object literal there is parsed as a JSON constant, so an expression
 * cannot move into it. In JavaScript the options object is evaluated like any other, and an expression in
 * the old positions moves over unchanged.
 *
 * A call inside commented out code is migrated too, for the same reason as in {@link OutboundHttpCallMigrationHandler}.
 */
public class ToExcelMigrationHandler {

	private static final Logger logger = LoggerFactory.getLogger(ToExcelMigrationHandler.class.getName());

	/**
	 * The builtin, called bare (StructrScript) or as $.toExcel / Structr.toExcel (JavaScript). A method of
	 * some other object (foo.toExcel) and a declaration of a function of that name are someone else's code.
	 */
	private static final Pattern CALL = Pattern.compile("(?<!function\\s{1,10})(?:\\$\\.|\\bStructr\\.|(?<![\\w.$]))toExcel\\s*\\(");

	/** The options keys for the old positional arguments after propertiesOrView, in their old order. */
	private static final List<String> OPTION_KEYS = List.of("includeHeader", "localizeHeader", "localizationDomain", "maxCellLength", "overflowMode");

	private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

	public enum Verdict {

		/** nothing to migrate: already in the 7.0 form, or not a call in the old signature */
		UP_TO_DATE,

		/** an old call whose arguments map onto the options object without judgement */
		AUTOMATIC,

		/** an old call, or possibly one, that needs a person to look at it */
		MANUAL
	}

	public record Finding(String type, String id, String name, String property, String call, Verdict verdict, String reason) {}

	/** What assess() and rewrite() both read, so the two cannot disagree about a call. */
	private record Assessment(Verdict verdict, String reason, String rewrite) {}

	public static void execute() throws FrameworkException {

		execute(false);
	}

	/**
	 * Reports the pre-7.0 calls, and in apply mode rewrites the ones it reported as AUTOMATIC.
	 */
	public static void execute(final boolean apply) throws FrameworkException {

		final List<Finding> findings = new LinkedList<>();

		scan(findings, apply, StructrTraits.SCHEMA_METHOD,   SchemaMethodTraitDefinition.SOURCE_PROPERTY);
		scan(findings, apply, StructrTraits.SCHEMA_PROPERTY, SchemaPropertyTraitDefinition.READ_FUNCTION_PROPERTY);
		scan(findings, apply, StructrTraits.SCHEMA_PROPERTY, SchemaPropertyTraitDefinition.WRITE_FUNCTION_PROPERTY);
		scan(findings, apply, StructrTraits.MAIL_TEMPLATE,   MailTemplateTraitDefinition.TEXT_PROPERTY);
		scan(findings, apply, StructrTraits.CONTENT,         ContentTraitDefinition.CONTENT_PROPERTY);

		scanDynamicFiles(findings, apply);

		report(findings, apply);
	}

	/** Every call to toExcel() in the given source, from the name (with $. or Structr. if present) to its closing brace. */
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

		final Assessment assessment = examine(call);

		return new Finding(type, id, name, property, call, assessment.verdict(), assessment.reason());
	}

	/**
	 * The migrated form of a call, or null when it is not one assess() reports as AUTOMATIC.
	 */
	public static String rewrite(final String call) {

		final Assessment assessment = examine(call);

		return assessment.verdict() == Verdict.AUTOMATIC ? assessment.rewrite() : null;
	}

	// ----- private methods -----
	private static Assessment examine(final String call) {

		final int open           = call.indexOf('(');
		final String function    = call.substring(0, open).trim();
		final boolean javaScript = function.contains(".");
		final List<String> args  = splitArguments(call.substring(open + 1, call.length() - 1));
		final int count          = args.size();

		// both signatures require at least two arguments, so a shorter call is broken either way and not ours to fix
		if (count < 2) {

			return new Assessment(Verdict.UP_TO_DATE, "fewer than two arguments, which neither signature accepts: nothing to migrate", null);
		}

		if (count > 7) {

			return new Assessment(Verdict.UP_TO_DATE, "more arguments than the old signature took, so it can only be the new one", null);
		}

		final String second = args.get(1);

		// the old signature never took an object in the second position
		if (isObjectLiteral(second)) {

			return new Assessment(Verdict.UP_TO_DATE, "already passes an options object", null);
		}

		final boolean isView       = isStringLiteral(second);
		final boolean isProperties = isArrayLiteral(second);

		if (!isView && !isProperties) {

			// an odd count, or a flag where the new signature expects the next collection, can only be the old signature
			final boolean certainlyOld = count % 2 != 0 || (count > 2 && isBooleanLiteral(args.get(2)));
			if (certainlyOld) {

				return new Assessment(Verdict.MANUAL, "the second argument is an expression (" + second + "), and whether it is a view name or a list of "
					+ "property names is only known at runtime: it becomes { view: " + second + " } or { properties: " + second + " }", null);
			}

			return new Assessment(Verdict.MANUAL, "the second argument is an expression (" + second + "). If it holds an options object, the call is "
				+ "up to date. If it holds a view name or a list of property names, it becomes { view: " + second + " } or { properties: " + second + " }", null);
		}

		// an options object in StructrScript is a JSON constant, nothing in it is evaluated
		if (!javaScript) {

			if (isProperties && !allStringLiterals(second)) {

				return new Assessment(Verdict.MANUAL, "the list of property names contains an expression (" + second + "), and an options object in "
					+ "StructrScript is a constant, so the expression cannot move into it", null);
			}

			for (int i = 2; i < count; i++) {

				final String argument = args.get(i);
				if (!isConstant(argument)) {

					return new Assessment(Verdict.MANUAL, "argument " + (i + 1) + " is an expression (" + argument + "), and an options object in "
						+ "StructrScript is a constant, so the expression cannot move into it", null);
				}
			}
		}

		final Map<String, String> options = new LinkedHashMap<>();

		options.put(isView ? "view" : "properties", second);

		for (int i = 2; i < count; i++) {

			options.put(OPTION_KEYS.get(i - 2), args.get(i));
		}

		final List<String> entries = new ArrayList<>();

		for (final Map.Entry<String, String> entry : options.entrySet()) {

			entries.add(entry.getKey() + ": " + entry.getValue());
		}

		final String rewritten = function + "(" + args.get(0) + ", { " + String.join(", ", entries) + " })";

		return new Assessment(Verdict.AUTOMATIC, describeRewrite(isView, options), rewritten);
	}

	private static String describeRewrite(final boolean isView, final Map<String, String> options) {

		final List<String> moved = new LinkedList<>();

		moved.add(isView ? "the view name becomes 'view'" : "the property list becomes 'properties'");

		for (final String key : options.keySet()) {

			if ("localizationDomain".equals(key)) {

				moved.add("headerLocalizationDomain becomes 'localizationDomain'");

			} else if (OPTION_KEYS.contains(key)) {

				moved.add(key);
			}
		}

		return "the positional arguments move into the options object: " + String.join(", ", moved);
	}

	private static void scan(final List<Finding> findings, final boolean apply, final String type, final String propertyName) throws FrameworkException {

		final Traits traits = Traits.of(type);
		if (!traits.hasKey(propertyName)) {

			return;
		}

		final PropertyKey<String> key = traits.key(propertyName);

		try (final Tx tx = StructrApp.getInstance().tx()) {

			for (final NodeInterface node : StructrApp.getInstance().nodeQuery(type).getResultStream()) {

				final String source = node.getProperty(key);
				if (source != null && !source.isEmpty()) {

					final String migrated = migrateSources(findings, type, propertyName, source, node, apply);

					if (apply && !migrated.equals(source)) {

						node.setProperty(key, migrated);
					}
				}
			}

			tx.success();
		}
	}

	private static void scanDynamicFiles(final List<Finding> findings, final boolean apply) throws FrameworkException {

		final String type = StructrTraits.FILE;
		final Traits traits = Traits.of(type);

		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
		securityContext.setRequestParameter(RequestParameters.EditMode.getName(), "1");

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			for (final NodeInterface node : app.nodeQuery(type).and().key(traits.key(FileTraitDefinition.IS_TEMPLATE_PROPERTY), true).getResultStream()) {

				final File file = node.as(File.class);
				final String source = getFileContent(file);

				if (source != null && !source.isEmpty()) {

					final String migrated = migrateSources(findings, type, "content", source, file, apply);

					if (apply && !migrated.equals(source)) {

						setFileContent(file, migrated);
					}
				}
			}

			tx.success();
		}
	}

	private static String migrateSources(final List<Finding> findings, final String type, final String propertyName, final String source, final NodeInterface node, final boolean apply) {

		String migrated = source;

		for (final String call : findCalls(source)) {

			final Finding finding = assess(type, node.getUuid(), node.getName(), propertyName, call);
			if (finding.verdict() == Verdict.UP_TO_DATE) {

				continue;
			}

			findings.add(finding);

			if (apply && finding.verdict() == Verdict.AUTOMATIC) {

				migrated = migrated.replace(call, rewrite(call));
			}
		}

		return migrated;
	}

	private static String getFileContent(final File file) {

		try (final InputStream is = file.getInputStream()) {

			return new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A").next();

		} catch (IOException ioex) {

			logger.warn("MigrationService: Unable to read file content of '{}', leaving it alone.", file.getPath(), ioex);

			return null;
		}
	}

	private static void setFileContent(final File file, final String content) throws FrameworkException {

		file.setProperty(file.getTraits().key(FileTraitDefinition.IS_TEMPLATE_PROPERTY), false);

		try (final OutputStream fos = file.getOutputStream(true, false)) {

			fos.write(content.getBytes(StandardCharsets.UTF_8));

		} catch (IOException ioex) {

			logger.warn("MigrationService: Unable to auto-migrate file content of '{}', leaving it alone.", file.getPath(), ioex);
		}

		file.setProperty(file.getTraits().key(FileTraitDefinition.IS_TEMPLATE_PROPERTY), true);
	}

	private static void report(final List<Finding> findings, final boolean apply) {

		if (findings.isEmpty()) {

			logger.debug("MigrationService: no calls to toExcel() use the pre-7.0 signature.");

			return;
		}

		final Map<Verdict, Integer> counts = new LinkedHashMap<>();

		for (final Finding finding : findings) {

			counts.merge(finding.verdict(), 1, Integer::sum);
		}

		if (apply) {

			logger.warn("MigrationService: {} call(s) to toExcel() used the pre-7.0 signature. {} were rewritten, {} need review and were left alone.",
				findings.size(), counts.getOrDefault(Verdict.AUTOMATIC, 0), counts.getOrDefault(Verdict.MANUAL, 0));

		} else {

			logger.warn("MigrationService: {} call(s) to toExcel() still use the pre-7.0 signature ({} would be rewritten, {} need review). "
				+ "Nothing was changed: this is a dry run. Since 7.0 toExcel() takes pairs of a collection and an options object, one pair per sheet, so these calls fail at runtime.",
				findings.size(), counts.getOrDefault(Verdict.AUTOMATIC, 0), counts.getOrDefault(Verdict.MANUAL, 0));
		}

		logger.warn("MigrationService: An empty collection used to produce an empty string and now produces a workbook with an empty sheet.");

		for (final Finding finding : findings) {

			logger.warn("MigrationService:   [{}] {} {} ({}).{}: {} - {}",
				finding.verdict(), finding.type(), finding.name() != null ? "'" + finding.name() + "'" : "", finding.id(),
				finding.property(), finding.call(), finding.reason());
		}
	}

	private static boolean isStringLiteral(final String argument) {

		final String trimmed = argument.trim();

		return !trimmed.isEmpty() && (trimmed.charAt(0) == '\'' || trimmed.charAt(0) == '"') && isLiteral(trimmed);
	}

	private static boolean isBooleanLiteral(final String argument) {

		final String trimmed = argument.trim();

		return "true".equals(trimmed) || "false".equals(trimmed);
	}

	/** A value StructrScript can carry inside an object literal, which it parses as JSON. */
	private static boolean isConstant(final String argument) {

		final String trimmed = argument.trim();

		return isStringLiteral(trimmed) || isBooleanLiteral(trimmed) || "null".equals(trimmed) || NUMBER.matcher(trimmed).matches();
	}

	/** Whether the argument is one list literal, not an expression that only starts with one, such as [a, b][0]. */
	private static boolean isArrayLiteral(final String argument) {

		final String trimmed = argument.trim();
		if (!trimmed.startsWith("[")) {

			return false;
		}

		int depth  = 0;
		char quote = 0;

		for (int i = 0; i < trimmed.length(); i++) {

			final char c = trimmed.charAt(i);

			if (quote != 0) {

				if (c == '\\') { i++; } else if (c == quote) { quote = 0; }

				continue;
			}

			switch (c) {

				case '\'', '"', '`' -> quote = c;
				case '(', '{', '['  -> depth++;
				case ')', '}', ']'  -> { depth--; if (depth == 0) { return i == trimmed.length() - 1; } }
				default             -> { }
			}
		}

		return false;
	}

	private static boolean allStringLiterals(final String arrayLiteral) {

		final String trimmed = arrayLiteral.trim();

		for (final String element : splitArguments(trimmed.substring(1, trimmed.length() - 1))) {

			if (!isStringLiteral(element)) {

				return false;
			}
		}

		return true;
	}
}
