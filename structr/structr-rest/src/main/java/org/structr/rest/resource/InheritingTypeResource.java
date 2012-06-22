
/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package org.structr.rest.resource;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.rest.exception.IllegalPathException;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

//~--- classes ----------------------------------------------------------------

/**
 * Like {@link TypeResource} but matches inheriting subclasses as well
 *
 * @author Axel Morgner
 */
public class InheritingTypeResource extends TypeResource {

	private static final Logger logger = Logger.getLogger(InheritingTypeResource.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

		return super.checkAndConfigure(part, securityContext, request);

	}

	@Override
	public List<GraphObject> doGet() throws FrameworkException {

		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		AbstractNode topNode                   = null;
		boolean includeDeletedAndHidden        = false;
		boolean publicOnly                     = false;

		if (rawType != null) {

			// searchAttributes.add(new TextualSearchAttribute("type", type, SearchOperator.OR));
			searchAttributes.add(Search.andExactTypeAndSubtypes(EntityContext.normalizeEntityName(rawType)));

			// searchable attributes from EntityContext
			hasSearchableAttributes(searchAttributes);

			// do search
			List<GraphObject> results = (List<GraphObject>) Services.command(securityContext, SearchNodeCommand.class).execute(topNode, includeDeletedAndHidden, publicOnly,
							    searchAttributes);

			if (!results.isEmpty()) {

				return results;
			}
		} else {

			logger.log(Level.WARNING, "type was null");
		}

		return Collections.emptyList();

	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if (next instanceof UuidResource) {

			return new InheritingTypedIdResource(securityContext, (UuidResource) next, this);
		} else if (next instanceof TypeResource) {

			throw new IllegalPathException();
		}

		return super.tryCombineWith(next);

	}

}
