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
package org.structr.core.function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.script.Scripting;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.ConceptType;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;

/**
 * Writes a message to the server log at one specific level: log.warn(), log.error(), log.info() and
 * log.debug().
 *
 * They share one logger with log(), so a single configuration entry governs all script logging and an
 * existing setup keeps working. The level is the only difference between them, which is what makes
 * log.debug() useful: it costs nothing in production until someone turns that level on.
 *
 * The name carries a dot, like the find.* predicates. StructrScript resolves such a name directly, and
 * in JavaScript FunctionWrapper exposes it as a member of $.log.
 */
public abstract class LogLevelFunction extends CoreFunction {

	/** Deliberately the logger of log(): one name to configure for everything a script writes. */
	protected static final Logger logger = LoggerFactory.getLogger(LogFunction.class.getName());

	/** warn, error, info or debug - the level this function writes at. */
	protected abstract String getLevel();

	@Override
	public String getName() {

		return "log." + getLevel();
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			final String message = buildMessage(ctx.getScriptLocation(), caller, sources);

			switch (getLevel()) {

				case "error" -> logger.error(message);
				case "warn"  -> logger.warn(message);
				case "info"  -> logger.info(message);
				case "debug" -> logger.debug(message);
			}

			return null;

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return null;
		}
	}

	/**
	 * The line itself, in the shape log() uses: where the call was written, then the caller, then the
	 * objects that were passed. Kept separate from apply() so the text can be asserted without
	 * attaching a log appender.
	 *
	 * The location is "row:column" and is present for StructrScript only - JavaScript passes null,
	 * and the line then reads exactly as log() has always rendered it.
	 */
	public static String buildMessage(final String location, final Object caller, final Object[] sources) {

		final StringBuilder buf = new StringBuilder();

		if (location != null) {

			buf.append("(").append(location).append(") ");
		}

		if (caller != null) {

			buf.append("Caller: ").append(Scripting.formatForLogging(caller)).append(" - ");
		}

		if (sources != null) {

			for (final Object obj : sources) {

				if (obj != null) {

					buf.append(Scripting.formatForLogging(obj));
				}
			}
		}

		return buf.toString();
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("objects...");
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.javaScript("Usage: ${$.log." + getLevel() + "(objects...)}. Example: ${$.log." + getLevel() + "('import finished, ', count, ' rows')}"),
			Usage.structrScript("Usage: ${log." + getLevel() + "(objects...)}. Example: ${log." + getLevel() + "('import finished, ', count, ' rows')}")
		);
	}

	@Override
	public String getShortDescription() {

		return "Logs the given objects to the logfile at " + getLevel().toUpperCase() + " level.";
	}

	@Override
	public String getLongDescription() {

		return "This function takes one or more arguments and logs the string representation of all of them to the "
			+ "Structr logfile at " + getLevel().toUpperCase() + " level. The individual objects are logged in a single "
			+ "line, one after another, without a separator, exactly as `log()` does.\n\n"
			+ "All logging functions share one logger, so a single entry in the log configuration governs everything "
			+ "your application code writes. The level is what distinguishes them: `log.debug()` costs nothing in "
			+ "production until someone raises the level for that logger, which makes it a practical place to leave "
			+ "detail that is only interesting while investigating something.\n\n"
			+ "In StructrScript the message is prefixed with the position the call was written at, as `(row:column)`, "
			+ "so a line can be traced back to its place in the script. JavaScript does not report a position.";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("objects...", "object or list of objects to log"));
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${log." + getLevel() + "('quota not set for ', $.me.name)}", "Logs a message at " + getLevel().toUpperCase() + " level, prefixed with the position in the script"),
			Example.javaScript("${{ $.log." + getLevel() + "('quota not set for ', $.me.name); }}", "The same call in JavaScript")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"Shares its logger with `log()`, which writes at INFO level. See also `log()`.",
			"In StructrScript the line is prefixed with the position of the call, as `(row:column)`. JavaScript does not report one."
		);
	}

	@Override
	public List<Link> getLinkedConcepts() {

		final List<Link> linkedConcepts = super.getLinkedConcepts();

		linkedConcepts.add(Link.to("ispartof", ConceptReference.of(ConceptType.Topic, "Logging")));

		return linkedConcepts;
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.InputOutput;
	}
}
