/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.structr.appmap.ApplicationMap;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.docs.Documentation;
import org.structr.docs.ontology.ConceptType;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.parameter.RESTParameter;
import org.structr.rest.exception.NotAllowedException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tells where objects of the application lie on the application map: POST { ids: [...] } answers per known id with
 * the ids of the map elements it may be shown as, the object itself first (see ApplicationMap.locate). The map uses it
 * to show the results of the global search, which name HTML elements, schema properties and parameter mappings that
 * are no elements of the map themselves. Restricted to admin users, as the map is.
 */
@Documentation(name="Application map locate endpoint", type= ConceptType.RestEndpoint, parent="System endpoints")
public class ApplicationMapLocateResource extends ExactMatchEndpoint {

	public ApplicationMapLocateResource() {

		super(RESTParameter.forStaticString(ApplicationMapResource.UriPart._appmap.name(), true), RESTParameter.forStaticString("locate", true));
	}

	@Override
	public RESTCallHandler accept(final RESTCall call) throws FrameworkException {

		return new ApplicationMapLocateResourceHandler(call);
	}

	private class ApplicationMapLocateResourceHandler extends RESTCallHandler {

		public ApplicationMapLocateResourceHandler(final RESTCall call) {

			super(call);
		}

		@Override
		public RestMethodResult doPost(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {

			if (securityContext == null || !securityContext.isSuperUser()) {
				throw new NotAllowedException("Access to the application map is restricted to admin users");
			}

			final Object ids              = propertySet.get("ids");
			final RestMethodResult result = new RestMethodResult(200);

			result.setNonGraphObjectResult(ApplicationMap.locate(StructrApp.getInstance(securityContext), ids instanceof Collection<?> list ? list.stream().map(String::valueOf).toList() : List.of()));

			return result;
		}

		@Override
		public String getTypeName(final SecurityContext securityContext) {

			return null;
		}

		@Override
		public boolean isCollection() {

			return false;
		}

		@Override
		public Set<String> getAllowedHttpMethodsForOptionsCall() {

			return Set.of("POST", "OPTIONS");
		}
	}
}
