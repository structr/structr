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
package org.structr.core.function;

import org.structr.common.error.FrameworkException;
import org.structr.common.error.SemanticErrorToken;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class ErrorFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_ERROR = "Usage: ${error(...)}. Example: ${error(\"base\", \"must_equal\", int(5))}";

	@Override
	public String getName() {
		return "error()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		final Class entityType;
		final String type;

		if (entity != null) {

			entityType = entity.getClass();
			type = entity.getType();

		} else {

			entityType = GraphObject.class;
			type = "Base";

		}

		
		try {
			
			if (sources == null) {
				throw new IllegalArgumentException();
			}

			switch (sources.length) {

				case 1:
					throw new IllegalArgumentException();

				case 2:
					{
						arrayHasLengthAndAllElementsNotNull(sources, 2);

						final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(entityType, sources[0].toString());
						ctx.raiseError(422, new SemanticErrorToken(type, key, sources[1].toString()));
						break;
					}
				case 3:
					{
						arrayHasLengthAndAllElementsNotNull(sources, 3);

						final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(entityType, sources[0].toString());
						ctx.raiseError(422, new SemanticErrorToken(type, key, sources[1].toString(), sources[2]));
						break;
					}
				default:
					logParameterError(entity, sources, ctx.isJavaScriptContext());
					break;
					
				}
			
		} catch (final IllegalArgumentException e) {
		
			logParameterError(entity, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
			
		}
		
		

		return null;
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_ERROR;
	}

	@Override
	public String shortDescription() {
		return "Signals an error to the caller";
	}

}
