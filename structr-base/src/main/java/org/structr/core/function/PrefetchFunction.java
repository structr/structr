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
import org.structr.core.graph.TransactionCommand;
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PrefetchFunction extends CoreFunction {

	@Override
	public String getName() {
		return "prefetch";
	}

	@Override
	public String getShortDescription() {
		return "Prefetches a subgraph.";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("query, listOfKeys");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		assertArrayHasMinLengthAndTypes(sources, 2, String.class, List.class);

		final String query     = (String)sources[0];
		final Set<String> keys = new LinkedHashSet<>((List)sources[1]);

		TransactionCommand.getCurrentTransaction().prefetch(query, keys);

		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {

		if (inJavaScriptContext) {

			return "$.prefetch('(:Customer)-[]->(:Task)', ['', ''])";

		} else {

			return "prefetch('(:Customer)-[]->(:Task)', merge('', ''))";
		}
	}
}
