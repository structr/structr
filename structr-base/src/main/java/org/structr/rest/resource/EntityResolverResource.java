/*
 * Copyright (C) 2010-2024 Structr GmbH
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


import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.rest.RestMethodResult;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.structr.common.SecurityContext;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.parameter.RESTParameter;


/**
 *
 *
 */
public class EntityResolverResource extends ExactMatchEndpoint {

	public EntityResolverResource() {
		super(RESTParameter.forStaticString("resolver", true));
	}

	@Override
	public RESTCallHandler accept(final RESTCall call) throws FrameworkException {
		return new ResolverResourceHandler(call);
	}

	private class ResolverResourceHandler extends RESTCallHandler {

		public ResolverResourceHandler(final RESTCall call) {
			super(call);
		}

		@Override
		public RestMethodResult doPost(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {

			final RestMethodResult result = new RestMethodResult(200);
			final Object src              = propertySet.get("ids");

			if (src != null && src instanceof Collection) {

				final Collection list = (Collection)src;
				for (final Object obj : list) {

					if (obj instanceof String) {

						AbstractNode node = (AbstractNode) StructrApp.getInstance().getNodeById((String)obj);
						if (node != null) {

							result.addContent(node);
						}
					}
				}

			} else {

				throw new FrameworkException(422, "Send a JSON object containing an array named 'ids' to use this endpoint.");
			}

			return result;
		}

		@Override
		public boolean isCollection() {
			return true;
		}

		@Override
		public Class getEntityClass(final SecurityContext securityContext) {
			return null;
		}

		@Override
		public Set<String> getAllowedHttpMethodsForOptionsCall() {
			return Set.of("OPTIONS", "POST");
		}
	}
}
