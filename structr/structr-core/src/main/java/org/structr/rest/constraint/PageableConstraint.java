/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.constraint;

import org.structr.rest.exception.PathException;

/**
 * A resource constraint that implements the generic ability to be
 * combined with a {@see SortConstraint) in order to sort the result
 * set.
 *
 * @author Christian Morgner
 */
public abstract class PageableConstraint extends FilterableConstraint {

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException {

		if(next instanceof SortConstraint) {
			((SortConstraint)next).wrapConstraint(this);
			return next;
		}

		return null;
	}
}
