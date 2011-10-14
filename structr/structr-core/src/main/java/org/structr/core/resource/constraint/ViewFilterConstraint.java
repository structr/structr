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

package org.structr.core.resource.constraint;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.structr.common.PropertyView;
import org.structr.core.GraphObject;
import org.structr.core.resource.PathException;
import org.structr.core.resource.adapter.ResultGSONAdapter;

/**
 *
 * @author Christian Morgner
 */
public class ViewFilterConstraint implements ResourceConstraint {

	private PropertyView propertyView = null;

	@Override
	public List<GraphObject> process(List<GraphObject> result, HttpServletRequest request) throws PathException {
		return result;
	}

	@Override
	public boolean acceptUriPart(String part) {

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
	public void configureContext(ResultGSONAdapter resultRenderer) {
		resultRenderer.setThreadLocalPropertyView(propertyView);
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException {
		return null;
	}
}
