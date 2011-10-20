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

import java.util.List;
import org.structr.rest.RestMethodResult;
import org.structr.rest.VetoableGraphObjectListener;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.wrapper.PropertySet;

/**
 * A resource constraint that implements the generic ability to
 * wrap another constraint.
 *
 * @author Christian Morgner
 */
public abstract class WrappingConstraint extends ResourceConstraint {

	protected ResourceConstraint wrappedConstraint = null;

	@Override
	public RestMethodResult doPost(PropertySet propertySet, List<VetoableGraphObjectListener> listeners) throws Throwable {

		if(wrappedConstraint != null) {
			return wrappedConstraint.doPost(propertySet, listeners);
		}

		throw new IllegalPathException();
	}

	@Override
	public RestMethodResult doHead() throws Throwable {

		if(wrappedConstraint != null) {
			return wrappedConstraint.doHead();
		}

		throw new IllegalPathException();
	}

	@Override
	public RestMethodResult doOptions() throws Throwable {

		if(wrappedConstraint != null) {
			return wrappedConstraint.doOptions();
		}

		throw new IllegalPathException();
	}

	protected void wrapConstraint(ResourceConstraint wrappedConstraint) {
		this.wrappedConstraint = wrappedConstraint;
	}
}
