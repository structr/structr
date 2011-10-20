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
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.structr.common.PropertyView;
import org.structr.core.GraphObject;
import org.structr.core.Value;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.PathException;
import org.structr.rest.wrapper.PropertySet;

/**
 * A resource constraint whose only purpose is to configure the
 * property view. This constraint must be wrapped around another
 * resource constraint, otherwise it will throw an IllegalPathException.
 *
 * @author Christian Morgner
 */
public class ViewFilterConstraint extends WrappingConstraint {

	private PropertyView propertyView = null;

	// no-arg constructor for automatic instantiation
	public ViewFilterConstraint() {
	}

	@Override
	public boolean checkAndConfigure(String part, HttpServletRequest request) {

		try {

			propertyView = PropertyView.valueOf(StringUtils.capitalize(part));
			return true;

		} catch(Throwable t) {

			propertyView = PropertyView.Public;
		}

		// only accept valid views
		return false;
	}

	@Override
	public List<GraphObject> doGet() throws PathException {
		if(wrappedConstraint != null) {
			return wrappedConstraint.doGet();
		}

		throw new IllegalPathException();
	}

	@Override
	public void doDelete() throws PathException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void doPost(PropertySet propertySet) throws PathException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void doPut(PropertySet propertySet) throws PathException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void doHead() throws PathException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void doOptions() throws PathException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException {
		return null;
	}

	@Override
	public void configurePropertyView(Value<PropertyView> propertyView) {
		propertyView.set(this.propertyView);
	}
}
