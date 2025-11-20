/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class ConcatFunction extends CoreFunction {

	@Override
	public String getName() {
		return "concat";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("values...");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final List list = new ArrayList();

		try {
			if (sources == null) {
				throw new IllegalArgumentException();
			}

			for (final Object source : sources) {

				// collection can contain nulls..
				if (source != null) {

					if (source instanceof Iterable) {

						Iterables.addAll(list, (Iterable)source);

					} else if (source.getClass().isArray()) {

						list.addAll(Arrays.asList((Object[])source));

					} else {

						list.add(source);
					}
				}
			}

			return StringUtils.join(list, "");

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());

		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{$.concat(values...)}}. Example: ${{$.concat($.this.firstName, $.this.lastName)}}"),
			Usage.structrScript("Usage: ${concat(values...)}. Example: ${concat(this.firstName, this.lastName)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Concatenates the given list of objects into a single string without a separator between them.";
	}

	@Override
	public String getLongDescription() {
		return "The objects can be of any type: string, number, entity, collection. If a collection is encountered, all elements of that collection are concatenated.";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("objects...", "one or more objects to concatenate")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${concat('test', 1, me, ' a string')}", "Results in \"test1.04c8a42581fb74ea092552539d0b594f0 a string\"")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"If nodes and relationships are among the parameters, their UUIDs will be written into the result.",
			"`null` values are filtered and not concatenated."
		);
	}
}
