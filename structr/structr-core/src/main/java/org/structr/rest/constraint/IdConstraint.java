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
import org.structr.core.entity.SuperUser;
import org.structr.core.node.FindNodeCommand;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.exception.PathException;
import org.structr.rest.adapter.ResultGSONAdapter;

/**
 * Represents an exact ID match. An IdConstraint will always result in a
 * single element when it is the last element in an URI. IdConstraints
 * must be tied to a preceding TypeConstraint.
 * 
 * @author Christian Morgner
 */
public class IdConstraint extends ResourceConstraint {
	
	private long id = -1;
	
	@Override
	public boolean acceptUriPart(String part) {

		try {
			this.setId(Long.parseLong(part));
			return true;

		} catch(Throwable t) {
		}

		return false;
	}

	@Override
	public List<GraphObject> process(List<GraphObject> results, HttpServletRequest request) throws PathException {

		GraphObject obj = (GraphObject)Services.command(FindNodeCommand.class).execute(new SuperUser(), getId());
		if(obj != null) {
			
			if(results == null) {
				results = new LinkedList<GraphObject>();
			}

			results.add(obj);

			return results;
		}

		throw new NotFoundException();
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) {
		return null;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}
