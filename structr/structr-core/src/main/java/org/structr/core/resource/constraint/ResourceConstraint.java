/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.resource.constraint;

import javax.servlet.http.HttpServletRequest;
import org.structr.core.resource.PathException;

/**
 * Base class for all resource constraints. The implementation of this
 * class follows the Composite pattern.
 * 
 * @author Christian Morgner
 */
public interface ResourceConstraint {

	public Result processParentResult(Result result, HttpServletRequest request) throws PathException;
	public boolean acceptUriPart(String part);

	public ResourceConstraint tryCombineWith(ResourceConstraint next);
}
