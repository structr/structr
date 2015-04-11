/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
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
package org.structr.core.property;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.helpers.Predicate;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;

/**
 *
 * @author Christian Morgner
 */
public class FunctionProperty<T> extends AbstractReadOnlyProperty<T> {

	private static final Logger logger = Logger.getLogger(FunctionProperty.class.getName());

	public FunctionProperty(final String name) {
		super(name);
	}

	@Override
	public Class relatedType() {
		return null;
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, Predicate<GraphObject> predicate) {

		if (obj instanceof AbstractNode) {

			try {

				return (T)Scripting.evaluate(new ActionContext(securityContext), obj, "${".concat(format).concat("}"));

			} catch (Throwable t) {

				logger.log(Level.WARNING, "Exception while evaluating function property {0}.", jsonName());

				t.printStackTrace();
			}
		}

		return null;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public Integer getSortType() {
		return null; // use string search
	}

	@Override
	public Class valueType() {
		return null;
	}
}
