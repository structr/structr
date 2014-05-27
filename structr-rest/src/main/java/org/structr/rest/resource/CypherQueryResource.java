/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.resource;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.graph.CypherQueryCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.exception.NotFoundException;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.rest.exception.NotAllowedException;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class CypherQueryResource extends Resource {

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

		if ("cypher".equals(part)) {

			return true;
		}

		return false;

	}

	@Override
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		throw new IllegalMethodException();

	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

		// Admins only
		if (!securityContext.isSuperUser()) {

			throw new NotAllowedException();

		}

		try {

			RestMethodResult result = new RestMethodResult(200);
			Object queryObject      = propertySet.get("query");

			if (queryObject != null) {

				String query                 = queryObject.toString();
				List<GraphObject> resultList = StructrApp.getInstance(securityContext).command(CypherQueryCommand.class).execute(query, propertySet);

				for (GraphObject obj : resultList) {

					result.addContent(obj);
				}

			}

			return result;

		} catch (org.neo4j.graphdb.NotFoundException nfe) {

			throw new NotFoundException();
		}
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		return null;

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getUriPart() {

		return "cypher";

	}

	@Override
	public Class getEntityClass() {

		return null;

	}

	@Override
	public String getResourceSignature() {

		return "cypher";

	}

	@Override
	public boolean isCollectionResource() throws FrameworkException {

		return true;

	}

}
