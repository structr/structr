/*
 * Copyright (C) 2010-2021 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.datasource;

import java.util.LinkedList;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.core.datasources.GraphDataSource;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.model.ListModel;
import org.structr.web.function.UiFunction;

/**
 * Data source that uses a ListModel as its source.
 */
public class ModelDataSource implements GraphDataSource<Iterable<GraphObject>> {

	@Override
	public Iterable<GraphObject> getData(final ActionContext actionContext, final NodeInterface referenceNode) throws FrameworkException {

		final RenderContext renderContext = (RenderContext) actionContext;
		final ListModel listModel         = ((DOMNode) referenceNode).getListModel();

		if (listModel == null) {
			return null;
		}

		try {

			return ModelDataSource.wrap(listModel.getListData(renderContext, referenceNode));

		} catch (UnlicensedScriptException ex) {

			ex.log(LoggerFactory.getLogger(ModelDataSource.class));
		}

		return null;
	}

	public static Iterable<GraphObject> wrap(final Object result) throws FrameworkException {

		if (result instanceof Iterable) {

			return FunctionDataSource.map((Iterable)result);

		} else if (result instanceof Object[]) {

			return (List<GraphObject>) UiFunction.toGraphObject(result, 1);

		} else if (result instanceof GraphObject) {

			// allow single-element results to be evaluated
			final List<GraphObject> wrapped = new LinkedList<>();

			wrapped.add((GraphObject)result);

			return wrapped;
		}

		return null;
	}
}
