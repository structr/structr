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

import org.structr.api.graph.Direction;
import org.structr.api.graph.RelationshipType;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipFactory;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class IncomingFunction extends CoreFunction {

	@Override
	public String getName() {
		return "incoming";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("entity [, relType ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			final RelationshipFactory factory = new RelationshipFactory(ctx.getSecurityContext());
			final Object source = sources[0];

			if (source instanceof NodeInterface) {

				final NodeInterface node = (NodeInterface)source;
				if (sources.length > 1) {

					final Object relType = sources[1];
					if (relType != null && relType instanceof String) {

						final String relTypeName = (String)relType;
						return factory.bulkInstantiate(node.getNode().getRelationships(Direction.INCOMING, RelationshipType.forName(relTypeName)));
					}

				} else {

					return factory.bulkInstantiate(node.getNode().getRelationships(Direction.INCOMING));
				}

			} else {

				logger.warn("Error: entity is not a node. Parameters: {}", getParametersAsString(sources));
				return "Error: entity is not a node.";
			}

		} catch (ArgumentNullException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${incoming(entity [, relType])}. Example: ${incoming(this, 'PARENT_OF')}"),
			Usage.javaScript("Usage: ${{ $.incoming(entity [, relType]); }}. Example: ${{ $.incoming(Structr.this, 'PARENT_OF'); }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns all incoming relationships of a node, with an optional qualifying relationship type.";
	}

	@Override
	public String getLongDescription() {
		return "You can use this function in the Function Query section of a Repeater Element to access the relationships of a node. See also `outgoing()` and `hasRelationship()`.";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("entity", "entity to fetch relationships for"),
			Parameter.optional("relType", "relationship type")
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Database;
	}
}
