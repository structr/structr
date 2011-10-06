/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.resource.constraint;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.SuperUser;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.resource.IllegalPathException;
import org.structr.core.resource.NotFoundException;
import org.structr.core.resource.PathException;

/**
 * Represents an exact ID match. An IdConstraint will always result in a
 * single element when it is the last element in an URI. IdConstraints
 * must be tied to a preceding TypeConstraint.
 * 
 * @author Christian Morgner
 */
public class TypedIdConstraint implements ResourceConstraint {

	private static final Logger logger = Logger.getLogger(TypedIdConstraint.class.getName());

	private String type = null;
	private long id = -1;

	public TypedIdConstraint(String type, long id) {
		this.type = type;
		this.id = id;
	}

	@Override
	public boolean acceptUriPart(String part) {
		return false;	// we will not accept URI parts directly
	}

	@Override
	public Result processParentResult(Result result, HttpServletRequest request) throws PathException {

		GraphObject obj = (GraphObject)Services.command(FindNodeCommand.class).execute(new SuperUser(), id);
		if(obj != null) {

			if(type.endsWith("s")) {
				logger.log(Level.FINEST, "Removing trailing plural 's' from type {0}", type);
				type = type.substring(0, type.length() - 1);
			}

			if(type.equalsIgnoreCase(obj.getType())) {

				return new Result(obj);

			} else {

				new IllegalPathException();
			}
		}

		throw new NotFoundException();
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) {
		return null;
	}
}
