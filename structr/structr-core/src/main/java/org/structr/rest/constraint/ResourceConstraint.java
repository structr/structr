/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.constraint;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Value;
import org.structr.rest.exception.PathException;

/**
 * Base class for all resource constraints. Constraints can be
 * combined with succeeding constraints to avoid unneccesary
 * evaluation.
 *
 * 
 * @author Christian Morgner
 */
public abstract class ResourceConstraint {

	protected SecurityContext securityContext = null;

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

	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}
}
