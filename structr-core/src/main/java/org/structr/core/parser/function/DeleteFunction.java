/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class DeleteFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_DELETE = "Usage: ${delete(entity)}. Example: ${delete(this)}";

	@Override
	public String getName() {
		return "delete()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (sources != null) {

			final App app = StructrApp.getInstance(entity != null ? entity.getSecurityContext() : ctx.getSecurityContext());
			for (final Object obj : sources) {

				if (obj instanceof NodeInterface) {

					app.delete((NodeInterface)obj);
					continue;
				}

				if (obj instanceof RelationshipInterface) {

					app.delete((RelationshipInterface)obj);
					continue;
				}
			}
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_DELETE;
	}

	@Override
	public String shortDescription() {
		return "Deletes the given entity from the database";
	}
}
