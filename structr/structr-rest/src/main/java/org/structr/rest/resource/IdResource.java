
/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package org.structr.rest.resource;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.FindNodeCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotFoundException;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import org.structr.common.error.FrameworkException;

//~--- classes ----------------------------------------------------------------

/**
 * Represents an exact ID match. An IdResource will always result in a
 * single element when it is the last element in an URI. IdConstraints
 * must be tied to a preceding TypeConstraint.
 *
 * @author Christian Morgner
 */
public class IdResource extends FilterableResource {

	private long id = -1;

	//~--- methods --------------------------------------------------------

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.securityContext = securityContext;

		try {

			this.setId(Long.parseLong(part));

			return true;

		} catch (Throwable t) {}

		return false;
	}

	@Override
	public List<? extends GraphObject> doGet() throws FrameworkException {

		GraphObject obj = getNode();

		if (obj != null) {

			List<GraphObject> results = new LinkedList<GraphObject>();

			results.add(obj);

			return results;
		}

		throw new NotFoundException();
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {

		// POST cannot be done on a single ID
		throw new IllegalPathException();
	}

	@Override
	public RestMethodResult doHead() throws FrameworkException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public RestMethodResult doOptions() throws FrameworkException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if(next instanceof RelationshipResource) {

			// make rel constraint wrap this
			((RelationshipResource)next).wrapResource(this);
			return next;
		}

		return super.tryCombineWith(next);
	}

	//~--- get methods ----------------------------------------------------

	public AbstractNode getNode() throws FrameworkException {
		return (AbstractNode) Services.command(securityContext, FindNodeCommand.class).execute(getId());
	}

	public AbstractNode getRelationship() throws FrameworkException {

		// find relationship by id!
		return null;
	}

	public long getId() {
		return id;
	}

	@Override
	public String getUriPart() {
		return Long.toString(id);
	}

	@Override
	public boolean isCollectionResource() {
		return false;
	}

	//~--- set methods ----------------------------------------------------

	public void setId(long id) {
		this.id = id;
	}
}
