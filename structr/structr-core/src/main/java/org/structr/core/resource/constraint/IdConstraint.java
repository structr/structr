/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.resource.constraint;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SuperUser;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.resource.IllegalPathException;
import org.structr.core.resource.PathException;

/**
 * Represents an exact ID match. An IdConstraint will always result in a
 * single element when it is the last element in an URI. IdConstraints
 * must be tied to a preceding TypeConstraint.
 * 
 * @author Christian Morgner
 */
public class IdConstraint extends ResourceConstraint<AbstractNode> {
	
	private long id = -1;
	
	@Override
	public boolean acceptUriPart(String part) {

		try {
			this.id = Long.parseLong(part);
			return true;

		} catch(Throwable t) {
		}

		return false;
	}

	@Override
	public Result<AbstractNode> processParentResult(Result<AbstractNode> result, HttpServletRequest request) throws PathException {

		return new Result(
			(AbstractNode)Services.command(FindNodeCommand.class).execute(new SuperUser(), id)
		);
	}

	@Override
	public boolean supportsMethod(String method) {
		return true;
	}

	@Override
	public boolean supportsNesting() {
		return true;
	}
}
