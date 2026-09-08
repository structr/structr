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

import org.structr.api.util.Category;
import org.structr.common.ChannelInput;
import org.structr.common.error.FrameworkException;
import org.structr.core.datasources.Channel;
import org.structr.core.datasources.ChannelResult;
import org.structr.docs.*;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class GetDataSourceFunction extends AdvancedScriptingFunction {

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

		final Object firstArgument = sources[0];
		Channel      channel       = null;

		// data source name?
		if (firstArgument instanceof String dataSourceName) {

			channel = Channel.forName(dataSourceName);
		}

		// alternative: allow passing a data source directly
		if (firstArgument instanceof Channel dataSource) {

			channel = dataSource;
		}

		if (channel != null) {

			final int pageSize         = getIntOrDefault(sources, 1, Integer.MAX_VALUE);
			final int page             = getIntOrDefault(sources, 2, 1);
			final ChannelInput input   = new ChannelInput(null, null, pageSize, page);
			final ChannelResult result = channel.getResult(ctx, input, null);

			return result.getData();
		}

		return null;
	}

	@Override
	public String getName() {

		return "getDataSource";
	}

	@Override
	public String getShortDescription() {

		return "Returns data from the data source with the given name.";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("name, pageSize, page");
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("name", "Name of the data source to query"),
			Parameter.optional("pageSize", "Number of results per page"),
			Parameter.optional("page", "Page number")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("getDataSource('node:Page', 10, 1)", "Fetch the first ten visible pages from the system data source for pages."),
			Example.javaScript("$.getDataSource('node:Page', 10, 1)", "Fetch the first ten visible pages from the system data source for pages.")
		);
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(Usage.javaScript("Usage: ${{$.getDataSource('node:Page', 10, 1)}}"), Usage.structrScript("Usage: ${getDataSource('node:Page', 10, 1)}"));
	}

	@Override
	public Category getCategory() {
		return FunctionCategory.Rendering;
	}
}
