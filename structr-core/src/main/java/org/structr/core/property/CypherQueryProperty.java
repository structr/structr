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
package org.structr.core.property;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.neo4j.helpers.Predicate;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CypherQueryCommand;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;

/**
 *
 *
 */
public class CypherQueryProperty extends AbstractReadOnlyProperty<List<GraphObject>> {

	public CypherQueryProperty(final String name, final String cypherQuery) {

		super(name);
		this.format = cypherQuery;
	}

	@Override
	public Class relatedType() {
		return null;
	}

	@Override
	public Class valueType() {
		return GraphObject.class;
	}

	@Override
	public List<GraphObject> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public List<GraphObject> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, Predicate<GraphObject> predicate) {

		if (obj instanceof AbstractNode) {

			try {

				final String query = Scripting.replaceVariables(new ActionContext(securityContext), obj, this.format);
				final Map<String, Object> parameters = new LinkedHashMap<>();

				parameters.put("id", obj.getUuid());
				parameters.put("type", obj.getType());


				return StructrApp.getInstance(securityContext).command(CypherQueryCommand.class).execute(query, parameters);

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		return null;
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public Integer getSortType() {
		return null; // use string sorting
	}
}
