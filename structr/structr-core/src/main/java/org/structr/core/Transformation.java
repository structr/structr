package org.structr.core;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;

/**
 * A transformation that can be applied to an object in the
 * presence of a {@link SecurityContext}.
 *
 * @author Christian Morgner
 */
public interface Transformation<T> {
	
	/**
	 * Transforms the given object.
	 * 
	 * @param securityContext the current security context
	 * @param obj the object to transform
	 * 
	 * @throws FrameworkException 
	 */
	public void apply(SecurityContext securityContext, T obj) throws FrameworkException;
	
	/**
	 * Returns the desired position of this transformation in a list. Return
	 * a low value here to get called early, and a high value to get called
	 * late.
	 * 
	 * @return the desired position of this transformation in a list
	 */
	public int getOrder();
}
