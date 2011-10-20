/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.constraint;

import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SuperUser;
import org.structr.core.node.FindNodeCommand;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.exception.PathException;
import org.structr.rest.wrapper.PropertySet;

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
		return (AbstractNode)Services.command(securityContext, FindNodeCommand.class).execute(new SuperUser(), getId());
	}
	
	@Override
	public boolean checkAndConfigure(String part, HttpServletRequest request) {

		try {
			this.setId(Long.parseLong(part));
			return true;

		} catch(Throwable t) {
		}

		return false;
	}

	@Override
	public List<GraphObject> doGet() throws PathException {

		GraphObject obj = getNode();
		if(obj != null) {
			
			List<GraphObject> results = new LinkedList<GraphObject>();
			results.add(obj);

			return results;
		}

		throw new NotFoundException();
	}

	@Override
	public void doDelete() throws PathException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void doPost(PropertySet propertySet) throws Throwable {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void doPut(PropertySet propertySet) throws PathException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void doHead() throws PathException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void doOptions() throws PathException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException {
		return super.tryCombineWith(next);
	}
}
