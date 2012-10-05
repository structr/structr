
/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package org.structr.rest.resource;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.node.CypherQueryCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.exception.NotFoundException;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

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
	public Result doGet(String sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		throw new IllegalMethodException();

	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {

		try {

			RestMethodResult result = new RestMethodResult(200);
			Object queryObject      = propertySet.get("query");

			if (queryObject != null) {

				String query                 = queryObject.toString();
				List<GraphObject> resultList = (List<GraphObject>) Services.command(securityContext, CypherQueryCommand.class).execute(query);

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
	public RestMethodResult doHead() throws FrameworkException {

		throw new IllegalMethodException();

	}

	@Override
	public RestMethodResult doOptions() throws FrameworkException {

		throw new IllegalMethodException();

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
