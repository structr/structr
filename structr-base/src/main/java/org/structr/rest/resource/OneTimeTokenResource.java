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

import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.OneTimeTokens;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Documentation;
import org.structr.docs.ontology.ConceptType;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.parameter.RESTParameter;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.exception.NotFoundException;

import java.util.Map;
import java.util.Set;

/**
 * Creates a one-time token with which a browser opens a session as the given user, for an admin who wants to see
 * the application as that user. See OneTimeTokens for the token and UiAuthenticator for where it is redeemed.
 */
@Documentation(name="One-time token endpoint", type= ConceptType.RestEndpoint, parent="System endpoints")
public class OneTimeTokenResource extends ExactMatchEndpoint {

	public OneTimeTokenResource() {

		super(RESTParameter.forStaticString("_oneTimeToken", true));
	}

	@Override
	public RESTCallHandler accept(final RESTCall call) throws FrameworkException {

		return new OneTimeTokenResourceHandler(call);
	}

	private class OneTimeTokenResourceHandler extends RESTCallHandler {

		public OneTimeTokenResourceHandler(final RESTCall call) {

			super(call);
		}

		@Override
		public RestMethodResult doPost(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {

			final Principal admin = (securityContext != null) ? securityContext.getUser(false) : null;

			if (admin == null || !securityContext.isSuperUser()) {
				throw new NotAllowedException("One-time tokens are restricted to admin users");
			}

			if (!Settings.TestSessionsEnabled.getValue()) {
				throw new NotAllowedException("Test sessions are switched off, see " + Settings.TestSessionsEnabled.getKey());
			}

			final Object userId      = propertySet.get("user");
			final NodeInterface node = (userId != null) ? StructrApp.getInstance(securityContext).getNodeById(StructrTraits.USER, String.valueOf(userId)) : null;

			if (node == null) {
				throw new NotFoundException("User not found");
			}

			final Principal user = node.as(Principal.class);

			if (user.isBlocked()) {
				throw new NotAllowedException("A blocked user cannot be logged in");
			}

			// a session as an admin would hand out everything; this is for seeing the application as an ordinary user
			if (user.isAdmin()) {
				throw new NotAllowedException("An admin user cannot be the target of a test session");
			}

			final String token            = OneTimeTokens.create(OneTimeTokens.PURPOSE_TEST_SESSION, user.getUuid(), admin.getUuid(), admin.getName());
			final RestMethodResult result = new RestMethodResult(200);

			result.setNonGraphObjectResult(Map.of(
				"token",           token,
				"parameter",       OneTimeTokens.PARAMETER,
				"validForSeconds", OneTimeTokens.getValidityMillis() / 1000
			));

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
