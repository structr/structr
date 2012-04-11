/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.resource;

import org.structr.common.error.FrameworkException;

/**
 * A resource constraint that implements the generic ability to be
 * combined with a {@see SortConstraint) in order to sort the result
 * set.
 *
 * @author Christian Morgner
 */
public abstract class PageableResource extends FilterableResource {

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if(next instanceof PagingResource) {
			((PagingResource)next).wrapResource(this);
			return next;
		}

		return super.tryCombineWith(next);
	}
}
