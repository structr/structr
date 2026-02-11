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

import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.List;

public class UnwindFunction extends CoreFunction {

	@Override
	public String getName() {
		return "unwind";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("list1, list2, ...");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final List list = new ArrayList();
		for (final Object source : sources) {

			if (source instanceof Iterable) {

				// filter null objects
				for (Object obj : (Iterable)source) {
					if (obj != null) {

						if (obj instanceof Iterable) {

							for (final Object elem : (Iterable)obj) {

								if (elem != null) {

									list.add(elem);
								}
							}

						} else {

							list.add(obj);
						}
					}
				}

			} else if (source != null) {

				list.add(source);
			}
		}

		return list;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{ $.unwind(list1, ...) }}."),
			Usage.structrScript("Usage: ${unwind(list1, ...)}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Converts a list of lists into a flat list.";
	}

	@Override
	public String getLongDescription() {
		return """
		Combines the given nested collections into to a single, "flat" collection. 
		This method is the reverse of `extract()` and can be used to flatten collections of related nodes that were 
		created with nested `extract()` calls etc. It is often used in conjunction with the `find()` method like in the example below.
		""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${unwind(this.children)}"),
				Example.javaScript("""
						${{ $.unwind([[1,2,3],4,5,[6,7,8]])}}
						> [1, 2, 3, 4, 5, 6, 7, 8]
						""")
		);
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("collections", "collection(s) to unwind")
				);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"`unwind()` is quite similar to `merge()`. The big difference is that `unwind()` filters out empty collections."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Collection;
	}
}
