/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.web.datasource;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.datasources.GraphDataSource;
import org.structr.core.graph.NativeQueryCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.dom.DOMNode;

/**
 *
 *
 */
public class CypherGraphDataSource implements GraphDataSource<Iterable<GraphObject>> {

	@Override
	public Iterable<GraphObject> getData(final ActionContext actionContext, final NodeInterface referenceNode) throws FrameworkException {

		final PropertyKey<String> cypherQueryKey = StructrApp.key(DOMNode.class, "cypherQuery");
		final String cypherQuery                 = ((DOMNode) referenceNode).getPropertyWithVariableReplacement(actionContext, cypherQueryKey);

		if (StringUtils.isBlank(cypherQuery)) {

			return null;
		}

		return StructrApp.getInstance(actionContext.getSecurityContext()).command(NativeQueryCommand.class).execute(cypherQuery);
	}
}
