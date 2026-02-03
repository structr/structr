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
import org.structr.docs.Language;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class MergeUniqueFunction extends CoreFunction {


	@Override
	public String getName() {
		return "mergeUnique";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("list1, list2, list3...");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final Set result = new LinkedHashSet<>();

		for (final Object source : sources) {

			if (source instanceof Iterable iterable) {

				// filter null objects
				for (Object obj : iterable) {

					if (obj != null) {

						result.add(obj);
					}
				}

			} else if (source != null) {

				result.add(source);
			}
		}

		return new LinkedList<>(result);
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${mergeUnique(list1, list2, list3...)}. Example: ${mergeUnique(this.children, this.siblings)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Merges collections and objects into a single collection, removing duplicates.";
	}

	@Override
	public String getLongDescription() {
		return """
		You can use this function to create collections of objects, add objects to a collection, or to merge multiple collections into a single one. All objects that are passed to this function will be added to the resulting collection. If an argument is a collection, all objects in that collection are added to the resulting collection as well.

		This function is very similar to `merge()` except that the resulting collection will **not** contain duplicate entries.
		""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("objects...", "collections or objects to merge into a single collection")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"This function will remove duplicate entries. If you don't want that, use `merge()`."
		);
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.StructrScript);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Collection;
	}
}
