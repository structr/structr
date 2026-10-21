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
package org.structr.common;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;

import java.util.*;

/**
 * Maps property values between Structr objects and an external representation, e.g. the
 * columns of a CSV file or the cells of a spreadsheet. The source name always denotes the
 * Structr property, the target name the external name. Each entry can transform the value
 * with a script, which sees the value (input direction) or the source object (output
 * direction) as the constant "input". The constant is set on the context that is passed
 * in, so a caller whose context must not see it passes a copy.
 */
public class PropertyMapping {

	private final List<Entry> entries = new LinkedList<>();

	public PropertyMapping(final List<Entry> entries) throws FrameworkException {

		for (final Entry entry : entries) {

			if (entry.sourceName() == null && entry.outputFunction() == null) {

				throw new FrameworkException(422, "Property mapping for target '" + entry.targetName() + "' needs a source name or an output function");
			}

			// a missing target name means: don't rename
			if (entry.targetName() == null) {

				this.entries.add(new Entry(entry.sourceName(), entry.sourceName(), entry.inputFunction(), entry.outputFunction()));

			} else {

				this.entries.add(entry);
			}
		}
	}

	/**
	 * Converts external input into Structr property values, in place: every value is moved
	 * from its target name to its source name, applying the input function if there is one.
	 * Keys that are not a target name of this mapping are removed. All values are read before
	 * any is written, so swapped or shared target names see the original input. Entries
	 * without a source name only apply to the output direction and are skipped.
	 */
	public void transformInput(final ActionContext actionContext, final Map<String, Object> input) throws FrameworkException {

		final Map<String, Object> original = new LinkedHashMap<>(input);

		input.clear();

		for (final Entry entry : entries) {

			Object value = original.get(entry.targetName());
			if (value != null && entry.sourceName() != null) {

				if (entry.inputFunction() != null) {

					actionContext.setConstant("input", value);
					value = Scripting.evaluate(actionContext, null, "${" + entry.inputFunction().trim() + "}", "property mapping " + entry.sourceName(), null);
				}

				input.put(entry.sourceName(), value);
			}
		}
	}

	/**
	 * Reads the mapped values of the given object, keyed by target name. An entry with an
	 * output function yields the result of the function, all others the raw property value.
	 */
	public Map<String, Object> transformOutput(final ActionContext actionContext, final GraphObject source) throws FrameworkException {

		final Map<String, Object> output = new LinkedHashMap<>();

		for (final Entry entry : entries) {

			if (entry.outputFunction() != null) {

				actionContext.setConstant("input", source);
				output.put(entry.targetName(), Scripting.evaluate(actionContext, null, "${" + entry.outputFunction().trim() + "}", "property mapping " + entry.targetName(), null));

			} else {

				final PropertyKey key = source.getTraits().key(entry.sourceName());

				output.put(entry.targetName(), key != null ? source.getProperty(key) : null);
			}
		}

		return output;
	}

	public record Entry(String sourceName, String targetName, String inputFunction, String outputFunction) {}
}
