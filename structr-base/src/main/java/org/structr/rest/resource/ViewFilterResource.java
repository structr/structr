/*
 * Copyright (C) 2010-2023 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.search.SortOrder;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Value;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.schema.SchemaHelper;

import java.util.Map;

/**
 * A resource constraint whose only purpose is to configure the
 * property view. This constraint must be wrapped around another
 * resource constraint, otherwise it will throw an IllegalPathException.
 */
public class ViewFilterResource extends WrappingResource {

	private static final Logger logger       = LoggerFactory.getLogger(ViewFilterResource.class.getName());
	private String propertyView              = null;

	// no-arg constructor for automatic instantiation
	public ViewFilterResource() {}

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
	public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

		if (wrappedResource != null) {

			return wrappedResource.doGet(sortOrder, pageSize, page);
		}

		throw new IllegalPathException("GET not allowed on " + getResourceSignature());
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {

		if (wrappedResource != null) {

			return wrappedResource.doPost(propertySet);
		}

		throw new IllegalPathException("POST not allowed on " + getResourceSignature());
	}

	@Override
	public void configurePropertyView(Value<String> propertyView) {

		try {
			propertyView.set(securityContext, this.propertyView);

		} catch(FrameworkException fex) {

			logger.warn("Unable to configure property view", fex);
		}
	}

	@Override
	public boolean createPostTransaction() {

		if (wrappedResource != null) {

			return wrappedResource.createPostTransaction();
		}

		return true;
	}

	@Override
	public String getResourceSignature() {

		StringBuilder signature = new StringBuilder();
		String signaturePart    = wrappedResource.getResourceSignature();

		if (signaturePart.contains("/")) {

			String[] parts  = StringUtils.split(signaturePart, "/");

			for (String subPart : parts) {

				if (Settings.isValidUuid(subPart)) {

					signature.append(subPart);
					signature.append("/");
				}

			}

		} else {

			signature.append(signaturePart);
		}

		if (propertyView != null) {

			// append view / scope part
			if (!signature.toString().endsWith("/")) {
				signature.append("/");
			}

			signature.append("_");
			signature.append(StringUtils.capitalize(propertyView));

		}

		return StringUtils.stripEnd(signature.toString(), "/");
	}

	public String getPropertyView() {
		return propertyView;
	}
}
