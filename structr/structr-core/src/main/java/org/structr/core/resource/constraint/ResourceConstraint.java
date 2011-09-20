/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.resource.constraint;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.core.entity.AbstractNode;
import org.structr.core.resource.IllegalPathException;
import org.structr.core.resource.NotAllowedException;
import org.structr.core.resource.PathException;

/**
 * Base class for all resource constraints. The implementation of this
 * class follows the Composite pattern.
 * 
 * @author Christian Morgner
 */
public abstract class ResourceConstraint<T extends AbstractNode> {

	private static final Logger logger = Logger.getLogger(ResourceConstraint.class.getName());

	private ResourceConstraint parent = null;
	private ResourceConstraint child = null;

	public abstract Result processParentResult(Result<T> result, HttpServletRequest request) throws PathException;
	public abstract boolean supportsMethod(String method);
	public abstract boolean supportsNesting();

	public abstract boolean acceptUriPart(String part);

	public ResourceConstraint() {
	}

	public final Result<T> getResult(Result<T> parentResult, HttpServletRequest request) throws PathException
	{
		if(!supportsMethod(request.getMethod())) {

			logger.log(Level.INFO, "{0} forbidden", request.getMethod());

			throw new NotAllowedException();
		}

		return processParentResult(parentResult, request);
	}

	public ResourceConstraint(ResourceConstraint parent) {
		this.parent = parent;
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();

		buf.append(getClass().getSimpleName());

		return buf.toString();
	}

	public final void setParent(ResourceConstraint parent) {
		this.parent = parent;
	}
	
	public final ResourceConstraint getParent() {
		return parent;
	}
	
	public final void setChild(ResourceConstraint child) throws PathException {

		// does not accept children => illegal path
		if(!supportsNesting()) {
			throw new IllegalPathException();
		}

		this.child = child;
		child.setParent(this);
	}
	
	public final ResourceConstraint getChild() {
		return child;
	}
	
	public final boolean hasChild() {
		return (child != null);
	}
}
