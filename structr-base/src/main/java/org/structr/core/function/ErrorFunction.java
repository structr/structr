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
import org.structr.common.error.SemanticErrorToken;
import org.structr.core.GraphObject;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class ErrorFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "error";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("propertyName, errorToken [, errorMessage]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		Class entityType = GraphObject.class;
		String type      = StructrTraits.GRAPH_OBJECT;

		if (caller != null && caller instanceof GraphObject) {

			entityType = caller.getClass();
			type = ((GraphObject) caller).getType();
		}

		try {

			if (sources == null) {
				throw new IllegalArgumentException();
			}

			switch (sources.length) {

				case 2: {
					ctx.raiseError(422, new SemanticErrorToken(type, (String)sources[0], (String)sources[1]));

					break;
				}

				case 3: {
					ctx.raiseError(422, new SemanticErrorToken(type, (String)sources[0], (String)sources[1]).withDetail(sources[2]));

					break;
				}

				default:
					throw new IllegalArgumentException();

			}

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${$.error(property, token[, detail])}. Example: ${$.error(\"name\", \"already_taken\"[, \"Another node with that name already exists\"])}"),
			Usage.structrScript("Usage: ${error(property, token[, detail])}. Example: ${error(\"name\", \"already_taken\"[, \"Another node with that name already exists\"])}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Signals an error to the caller";
	}

}
