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

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class HasIncomingRelationshipFunction extends CoreFunction {

	@Override
	public String getName() {
		return "hasIncomingRelationship";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("source, target [, relType ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 2, 3);

			final Object source = sources[0];
			final Object target = sources[1];

			NodeInterface sourceNode = null;
			NodeInterface targetNode = null;

			if (source instanceof NodeInterface && target instanceof NodeInterface) {

				sourceNode = (NodeInterface)source;
				targetNode = (NodeInterface)target;

			} else {

				logger.warn("Error: entities are not nodes. Parameters: {}", getParametersAsString(sources));
				return "Error: entities are not nodes.";
			}

			if (sources.length == 2) {

				for (final RelationshipInterface rel : sourceNode.getIncomingRelationships()) {

					final NodeInterface s = rel.getSourceNode();
					final NodeInterface t = rel.getTargetNode();

					// We need to check if current user can see source and target node which is often not the case for OWNS or SECURITY rels
					if (s != null & t != null
						&& s.equals(targetNode) && t.equals(sourceNode)) {
						return true;
					}
				}

			} else if (sources.length == 3) {

				// dont try to create the relClass because we would need to do that both ways!!! otherwise it just fails if the nodes are in the "wrong" order (see tests:890f)
				final String relType = (String)sources[2];

				for (final RelationshipInterface rel : sourceNode.getIncomingRelationships()) {

					final NodeInterface s = rel.getSourceNode();
					final NodeInterface t = rel.getTargetNode();

					// We need to check if current user can see source and target node which is often not the case for OWNS or SECURITY rels
					if (s != null & t != null
						&& rel.getRelType().name().equals(relType)
						&& s.equals(targetNode) && t.equals(sourceNode)) {
						return true;
					}
				}
			}

		} catch (ArgumentNullException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return false;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${hasIncomingRelationship(from, to [, relType])}."),
			Usage.javaScript("Usage: ${{$.hasIncomingRelationship(from, to [, relType])}}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns true if the given entity has incoming relationships of the given type.";
	}

	@Override
	public String getLongDescription() {
		return "Returns a boolean value indicating whether **at least one** incoming relationship exists between the given entities, with an optional qualifying relationship type. See also `incoming()`, `outgoing()`, `has_relationship()` and `has_outgoing_relationship()`.";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${hasIncomingRelationship(me, page, 'OWNS')}"),
				Example.javaScript("${{ $.hasIncomingRelationship($.me, $.page, 'OWNS') }}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("from", "entity the relationship goes from"),
				Parameter.mandatory("to", "entity the relationship goes to"),
				Parameter.optional("relType", "type of relationship")
				);
	}
}
