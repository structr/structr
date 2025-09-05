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

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.ActionContext;

import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class ExtractFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_EXTRACT = "Usage: ${extract(list, propertyName)}. Example: ${extract(this.children, \"amount\")}";

	@Override
	public String getName() {
		return "extract";
	}

	@Override
	public String getSignature() {
		return "list, propertyName";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			if (sources == null) {
				throw new IllegalArgumentException();
			}


			if (sources.length == 1) {

				// no property key given, maybe we should extract a list of lists?
				if (sources[0] instanceof Iterable i) {

					final List extraction = new LinkedList();

					for (final Object obj : i) {

						if (obj instanceof Iterable o) {

							Iterables.addAll(extraction, Iterables.toList(o));
						}
					}

					return extraction;
				}

			}


			if (sources.length == 2) {

				if (sources[0] == null) {
					return null;
				}

				if (sources[0] instanceof Iterable && sources[1] instanceof String) {

					final List extraction = new LinkedList();
					final String keyName  = (String)sources[1];

					for (final Object obj : (Iterable)sources[0]) {

						if (obj instanceof GraphObject g) {

							final PropertyKey key = g.getTraits().key(keyName);
							final Object value    = g.getProperty(key);

							if (value != null) {

								if (value instanceof Iterable i) {

									extraction.add(Iterables.toList(i));

								} else {

									extraction.add(value);
								}
							}
						}
					}

					return extraction;
				}
			}

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());

		}

		return null;

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_EXTRACT;
	}

	@Override
	public String shortDescription() {
		return "Returns a collection of all the elements with a given name from a collection";
	}

}
