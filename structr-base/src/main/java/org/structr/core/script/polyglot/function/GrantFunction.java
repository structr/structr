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
package org.structr.core.script.polyglot.function;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;

import java.util.Arrays;

public class GrantFunction<T extends GraphObject> implements ProxyExecutable {
	private Logger logger = LoggerFactory.getLogger(GrantFunction.class);
	private final ActionContext actionContext;
	private final T node;

	public GrantFunction(final ActionContext actionContext, final T node) {

		this.actionContext = actionContext;
		this.node = node;
	}

	@Override
	public Object execute(Value... arguments) {

		if (arguments != null && arguments.length > 0) {
			Object[] parameters = Arrays.stream(arguments).map(arg -> PolyglotWrapper.unwrap(actionContext, arg)).toArray();

			if (parameters.length > 0 && parameters[0] != null) {

				try {

					if (parameters.length >= 2 && parameters[1] != null) {

						// principal, node, string
						final Object principal = parameters[0];
						String permissions = parameters[1].toString();

						// append additional parameters to permission string
						if (parameters.length > 2) {

							for (int i = 2; i < parameters.length; i++) {

								if (parameters[i] != null) {
									permissions += "," + parameters[i].toString();
								}
							}
						}

						// call function, entity can be null here!
						new org.structr.core.function.GrantFunction().apply(actionContext, null, new Object[]{principal, node, permissions});
					}

					return null;

				} catch (FrameworkException ex) {

					logger.error("Unexpected function in Grant function.", ex);
				}
			}
		}

		return null;
	}
}
