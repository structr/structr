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

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.TransactionCommand;
import org.structr.docs.Example;
import org.structr.docs.Experimental;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Experimental
public class Prefetch2Function extends CoreFunction {

	@Override
	public String getName() {
		return "prefetch2";
	}

	@Override
	public String getShortDescription() {
		return "Prefetches a subgraph using a query that returns explicit node and relationship collections.";
	}

	@Override
	public String getLongDescription() {
		return """
				The cypher query must return a collection of nodes `AS nodes` and a list of relationships between them `AS rels`.
				Optionally, an `$id` parameter can be used to make the query more specific.

				For every relationship in the `rels` collection, the `listOfOutgoingKeys` are marked as prefetched for the start node
				and the `listOfIncomingKeys` are marked as prefetched for the end node.
				This means that for those relationships, no more automatic database queries are being made later on.

				A common pitfall can be that a relationship is not included in the cypher query but is still included in those lists, resulting in a scenario where nodes look like they do not have related nodes.
				""";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("query, listOfOutgoingKeys, listOfIncomingKeys [, id]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		assertArrayHasMinLengthAndTypes(sources, 3, String.class, List.class, List.class);

		final String query             = (String)sources[0];
		final Set<String> outgoingKeys = new LinkedHashSet<>((List)sources[1]);
		final Set<String> incomingKeys = new LinkedHashSet<>((List)sources[2]);
		final String id                = sources.length > 3 && sources[3] instanceof String ? (String)sources[3] : null;

		TransactionCommand.getCurrentTransaction().prefetch2(query, outgoingKeys, incomingKeys, id);

		return null;
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
				Usage.javaScript("Usage: ${{ $.prefetch(query, listOfOutgoingKeys, listOfIncomingKeys [, id]); }}. Example: ${{ $.prefetch2('MATCH (n:User { id: $id })-[r:HAS_TASK]->(m:Task) RETURN collect(n) + collect(m) AS nodes, collect(r) AS rels', ['all/OUTGOING/HAS_TASK'], ['all/INCOMING/HAS_TASK'], uuid); }}"),
				Usage.structrScript("Usage: ${prefetch(query, listOfOutgoingKeys, listOfIncomingKeys [, id])}. Example: ${prefetch2('MATCH (n:User { id: $id })-[r:HAS_TASK]->(m:Task) RETURN collect(n) + collect(m) AS nodes, collect(r) AS rels', merge('all/OUTGOING/HAS_TASK'), merge('all/INCOMING/HAS_TASK'), uuid)}")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.javaScript("""
						${{
							let prefetchQuery = `
								MATCH
									(n:User { id: $id })-[r:HAS_TASK|CURRENT_TASK]->(m:Task)
								RETURN
									collect(n) + collect(m) AS nodes,
									collect(r) AS rels
							`;
							let idParameter = $.me.id;

							let listOfOutgoingKeys = [
								'all/OUTGOING/HAS_TASK',
								'all/OUTGOING/CURRENT_TASK'
							];
							let listOfIncomingKeys = [
								'all/INCOMING/HAS_TASK',
								'all/INCOMING/CURRENT_TASK'
							];

							$.prefetch2(prefetchQuery, listOfOutgoingKeys, listOfIncomingKeys, idParameter);
						}}
						""", "Prefetch all tasks for the current user and their current task")
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Database;
	}
}
