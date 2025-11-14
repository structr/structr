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
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ComplementFunction extends CoreFunction {

	@Override
	public String getName() {
		return "complement";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("sourceList, obj, ...");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		// it should be a list so we can use it in a repeater!
		final LinkedList resultingList = new LinkedList();

		if (sources[0] instanceof Iterable) {

			for (Object o : ((Iterable) sources[0])) {

				resultingList.add(o);
			}

		} else {

			logger.warn("Argument 1 for complement() must be a Collection. Parameters: {}", new Object[] { getDisplayName(), getParametersAsString(sources) });
			return "Argument 1 for complement() must be a Collection";

		}

		for (int cnt = 1; cnt < sources.length; cnt++) {

			final Object source = sources[cnt];

			if (source instanceof Iterable) {

				for (Object o : ((Iterable) source)) {
					final List mockList = new ArrayList();
					mockList.add(o);
					resultingList.removeAll(mockList);
				}

			} else if (source != null) {

				final List mockList = new ArrayList();
				mockList.add(source);
				resultingList.removeAll(mockList);

			}

		}

		return resultingList;
	}


	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{$.complement(sourceList, obj, ...)}}. (The resulting list contains no duplicates) Example: ${{$.complement(allUsers, $.me)}} => List of all users except myself"),
			Usage.structrScript("Usage: ${complement(sourceList, obj, ...)}. (The resulting list contains no duplicates) Example: ${complement(allUsers, me)} => List of all users except myself")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the complement of all lists.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

}
