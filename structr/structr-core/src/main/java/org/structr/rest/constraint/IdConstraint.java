/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.constraint;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.FindNodeCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.VetoableGraphObjectListener;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.exception.PathException;

/**
 * Represents an exact ID match. An IdConstraint will always result in a
 * single element when it is the last element in an URI. IdConstraints
 * must be tied to a preceding TypeConstraint.
 * 
 * @author Christian Morgner
 */
public class IdConstraint extends FilterableConstraint {
	
	private long id = -1;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public AbstractNode getNode() {
		return (AbstractNode)Services.command(securityContext, FindNodeCommand.class).execute(getId());
	}
	
	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.securityContext = securityContext;

		try {
			this.setId(Long.parseLong(part));
			return true;

		} catch(Throwable t) {
		}

		return false;
	}

	@Override
	public List<? extends GraphObject> doGet(List<VetoableGraphObjectListener> listeners) throws PathException {

		GraphObject obj = getNode();
		if(obj != null) {
			
			List<GraphObject> results = new LinkedList<GraphObject>();
			results.add(obj);

			return results;
		}

		throw new NotFoundException();
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet, List<VetoableGraphObjectListener> listeners) throws Throwable {

		// POST cannot be done on a single ID
		throw new IllegalPathException();
	}

	@Override
	public RestMethodResult doHead() throws Throwable {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public RestMethodResult doOptions() throws Throwable {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException {
		return super.tryCombineWith(next);
	}

	@Override
	public String getUriPart() {
		return Long.toString(id);
	}
}
