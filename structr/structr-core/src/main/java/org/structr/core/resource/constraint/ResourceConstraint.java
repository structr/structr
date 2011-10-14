/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.resource.constraint;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.structr.core.GraphObject;
import org.structr.core.resource.PathException;
import org.structr.core.resource.adapter.ResultGSONAdapter;

/**
 * Base class for all resource constraints. The implementation of this
 * class follows the Composite pattern.
 * 
 * @author Christian Morgner
 */
public interface ResourceConstraint {

	/**
	 *
	 * @param result
	 * @param request
	 * @return
	 * @throws PathException
	 */
	public List<GraphObject> process(List<GraphObject> result, HttpServletRequest request) throws PathException;

	/**
	 *
	 * @param part
	 * @return
	 */
	public boolean acceptUriPart(String part);
	
	/**
	 * 
	 * @param resultRenderer
	 */
	public void configureContext(ResultGSONAdapter resultRenderer);

	/**
	 *
	 * @param next
	 * @return
	 * @throws PathException
	 */
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException;
}
