/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.resource.constraint;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.structr.core.entity.AbstractNode;
import org.structr.core.resource.PathException;

/**
 * The root container for nested resource constraints.
 * 
 * @author Christian Morgner
 */
public class RootResourceConstraint extends ResourceConstraint<AbstractNode> {

	public Result<AbstractNode> getNestedResults(HttpServletRequest request) throws PathException {

		// ignore our own result, as it is null anyways
		ResourceConstraint resourceConstraint = this;
		Result result = null;
		
		while(resourceConstraint != null) {
			
			// obtain result from current element
			result = resourceConstraint.getResult(result, request);
			
			// advance to child and transform result
			resourceConstraint = resourceConstraint.getChild();
		}
		
		return result;
	}

	@Override
	public boolean acceptUriPart(String part) {
		return false;
	}

	@Override
	public Result<AbstractNode> processParentResult(Result result, HttpServletRequest request) throws PathException {
		return null;
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
