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
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Value;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;

/**
 * A resource constraint whose only purpose is to configure the
 * property view. This constraint must be wrapped around another
 * resource constraint, otherwise it will throw an IllegalPathException.
 *
 * @author Christian Morgner
 */
public class ViewFilterConstraint extends WrappingConstraint {

	private String propertyView = null;

	// no-arg constructor for automatic instantiation
	public ViewFilterConstraint() {
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		if(this.wrappedConstraint == null) {
			this.securityContext = securityContext;
			propertyView = part;

			return true;
		}
		
		return false;

	}

	@Override
	public List<? extends GraphObject> doGet() throws FrameworkException {
		if(wrappedConstraint != null) {
			return wrappedConstraint.doGet();
		}

		throw new IllegalPathException();
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
		if(wrappedConstraint != null) {
			return wrappedConstraint.doPost(propertySet);
		}

		throw new IllegalPathException();
	}

	@Override
	public RestMethodResult doHead() throws FrameworkException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public RestMethodResult doOptions() throws FrameworkException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void configurePropertyView(Value<String> propertyView) {
		propertyView.set(this.propertyView);
	}
}
