/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.resource.constraint;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.PropertyView;
import org.structr.core.GraphObject;
import org.structr.core.Value;
import org.structr.core.resource.PathException;

/**
 * Base class for all resource constraints. The implementation of this
 * class follows the Composite pattern.
 * 
 * @author Christian Morgner
 */
public abstract class ResourceConstraint {

	/**
	 *
	 * @param result
	 * @param request
	 * @return
	 * @throws PathException
	 */
	public abstract List<GraphObject> process(List<GraphObject> result, HttpServletRequest request) throws PathException;

	/**
	 *
	 * @param part
	 * @return
	 */
	public abstract boolean acceptUriPart(String part);

	/**
	 *
	 * @param next
	 * @return
	 * @throws PathException
	 */
	public abstract ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException;

	/**
	 *
	 * @param propertyView
	 */
	public void configurePropertyView(Value<PropertyView> propertyView) {
	}
}
