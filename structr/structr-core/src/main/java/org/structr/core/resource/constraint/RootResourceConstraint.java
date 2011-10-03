/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.resource.constraint;

import javax.servlet.http.HttpServletRequest;
import org.structr.core.resource.PathException;

/**
 * The root container for nested resource constraints.
 * 
 * @author Christian Morgner
 */
public class RootResourceConstraint extends ResourceConstraint {

	public Result getNestedResults(HttpServletRequest request) throws PathException {

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
	public Result processParentResult(Result result, HttpServletRequest request) throws PathException {
		return null;
	}
}
