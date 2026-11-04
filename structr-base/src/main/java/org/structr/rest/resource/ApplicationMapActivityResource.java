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

import org.structr.api.search.SortOrder;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.appmap.ApplicationMap;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.docs.Documentation;
import org.structr.docs.ontology.ConceptType;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.parameter.RESTParameter;
import org.structr.rest.exception.NotAllowedException;

import java.util.List;
import java.util.Set;

/**
 * The current use of the elements of the application map, per element id as hits
 * (see ApplicationMap.toActivityObject), so the map shows fresh numbers without loading again. Restricted to admin
 * users, as the map is.
 */
@Documentation(name="Application map activity endpoint", type= ConceptType.RestEndpoint, parent="System endpoints")
public class ApplicationMapActivityResource extends ExactMatchEndpoint {

	public ApplicationMapActivityResource() {

		super(RESTParameter.forStaticString(ApplicationMapResource.UriPart._appmap.name(), true), RESTParameter.forStaticString("activity", true));
	}

	@Override
	public RESTCallHandler accept(final RESTCall call) throws FrameworkException {

		return new ApplicationMapActivityResourceHandler(call);
	}

	private class ApplicationMapActivityResourceHandler extends RESTCallHandler {

		public ApplicationMapActivityResourceHandler(final RESTCall call) {

			super(call);
		}

		@Override
		public ResultStream doGet(final SecurityContext securityContext, final SortOrder sortOrder, final int pageSize, final int page) throws FrameworkException {

			if (securityContext == null || !securityContext.isSuperUser()) {
				throw new NotAllowedException("Access to the application map is restricted to admin users");
			}

			final ApplicationMap map = ApplicationMap.build(StructrApp.getInstance(securityContext));

			return new PagingIterable(getURL(), List.of(map.toActivityObject()));
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

			return Set.of("GET", "OPTIONS");
		}
	}
}
