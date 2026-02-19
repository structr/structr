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
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
		return "";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("query, listOfOutgoingKeys, listOfIncomingKeys[, id]");
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
			Usage.javaScript("$.prefetch2('MATCH (n:Customer)-[r:HAS_TASK]->(m:Task) RETURN collect(n) + collect(m) AS nodes, collect(r) AS rels', ['Customer/all/OUTGOING/HAS_TASK'], ['Task/all/INCOMING/HAS_TASK'])"),
			Usage.structrScript("prefetch2('MATCH (n:Customer)-[r:HAS_TASK]->(m:Task) RETURN collect(n) + collect(m) AS nodes, collect(r) AS rels', merge('Customer/all/OUTGOING/HAS_TASK'), merge('Task/all/INCOMING/HAS_TASK'))")
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Database;
	}
}
