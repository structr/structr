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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Documentation;
import org.structr.docs.ontology.ConceptType;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.parameter.RESTParameter;
import org.structr.rest.exception.NotAllowedException;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 *
 */
@Documentation(name="User information endpoint", type= ConceptType.RestEndpoint, shortDescription="HTTP endpoint that always returns the current user as a JSON object. URL path is /me.", parent="System endpoints")
public class MeResource extends ExactMatchEndpoint {

    public MeResource() {
        super(RESTParameter.forStaticString("me", true, StructrTraits.USER));
    }

    @Override
    public RESTCallHandler accept(final RESTCall call) throws FrameworkException {
        return new MeResourceHandler(call);
    }

    private class MeResourceHandler extends RESTCallHandler {

        public MeResourceHandler(final RESTCall call) {
            super(call);
        }

        @Override
        public ResultStream doGet(final SecurityContext securityContext, final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

            Principal user = securityContext.getUser(true);
            if (user != null) {

                return new PagingIterable<>(getURL(), Arrays.asList(user));

            } else {

                throw new NotAllowedException("No user");
            }
        }

        @Override
        public RestMethodResult doPut(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {
            return genericPut(securityContext, propertySet);
        }

        @Override
        public RestMethodResult doDelete(final SecurityContext securityContext) throws FrameworkException {
            return genericDelete(securityContext);
        }

        @Override
        public boolean isCollection() {
            return false;
        }

        @Override
        public String getTypeName(final SecurityContext securityContext) {
            return StructrTraits.USER;
        }

        @Override
        public Set<String> getAllowedHttpMethodsForOptionsCall() {
            return Set.of("DELETE", "GET", "OPTIONS", "PUT");
        }
    }

}
