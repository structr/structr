/*
 *  Copyright (C) 2011 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.rest.constraint;

import org.structr.rest.exception.PathException;

/**
 * A resource constraint that implements the generic ability to be
 * combined with a {@see ViewFilterConstraint) in order to configure
 * a specific property view.
 *
 * @author Christian Morgner
 */
public abstract class FilterableConstraint extends WrappingConstraint {

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException {

		if(next instanceof ViewFilterConstraint) {
			((ViewFilterConstraint)next).wrapConstraint(this);
			return next;
		}

		return super.tryCombineWith(next);
	}
}
