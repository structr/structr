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

import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

public class MergeUniqueFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_MERGE_UNIQUE = "Usage: ${merge_unique(list1, list2, list3...)}. Example: ${merge_unique(this.children, this.siblings)}";

	@Override
	public String getName() {
		return "merge_unique";
	}

	@Override
	public String getSignature() {
		return "list1, list2, list3...";
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
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_MERGE_UNIQUE;
	}

	@Override
	public String shortDescription() {
		return "Merges the given collections / objects into a single collection, removing duplicates";
	}

}
