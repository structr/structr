/*
 *  Copyright (C) 2010-2012 Axel Morgner
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



package org.structr.rest.resource;

import org.structr.core.Result;
import org.apache.commons.lang.StringUtils;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Value;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import org.structr.core.EntityContext;

//~--- classes ----------------------------------------------------------------

/**
 * A resource constraint whose only purpose is to configure the
 * property view. This constraint must be wrapped around another
 * resource constraint, otherwise it will throw an IllegalPathException.
 *
 * @author Christian Morgner
 */
public class ViewFilterResource extends WrappingResource {

	private static final Logger logger       = Logger.getLogger(ViewFilterResource.class.getName());
	private static final Pattern uuidPattern = Pattern.compile("[a-zA-Z0-9]{32}");
	private String propertyView              = null;

	//~--- constructors ---------------------------------------------------

	// no-arg constructor for automatic instantiation
	public ViewFilterResource() {}

	//~--- methods --------------------------------------------------------

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		if (this.wrappedResource == null) {

			this.securityContext = securityContext;
			propertyView         = part;

			return true;

		}

		return false;
	}

	@Override
	public Result doGet(String sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		if (wrappedResource != null) {

			return wrappedResource.doGet(sortKey, sortDescending, pageSize, page, offsetId);

		}

		throw new IllegalPathException();
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {

		if (wrappedResource != null) {

			return wrappedResource.doPost(propertySet);

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
		
		try {
			propertyView.set(securityContext, this.propertyView);
			
		} catch(FrameworkException fex) {
			
			logger.log(Level.WARNING, "Unable to configure property view", fex);
		}
	}

	//~--- get methods ----------------------------------------------------
	
	@Override
	public String getResourceSignature() {

		StringBuilder uri = new StringBuilder();
		String uriPart    = getUriPart();

		if (uriPart.contains("/")) {

			String[] parts  = StringUtils.split(uriPart, "/");
			Matcher matcher = uuidPattern.matcher("");

			for (String subPart : parts) {

				// re-use pattern matcher for better performance
				matcher.reset(subPart);
				
				if (!matcher.matches()) {

					uri.append(subPart);
					uri.append("/");

				}

			}

		} else {

			uri.append(uriPart);

		}

		if (propertyView != null) {
			
			// append view / scope part
			uri.append("/");
			uri.append(EntityContext.normalizeEntityName(propertyView));
		}

		return uri.toString();
	}
}
