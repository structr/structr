/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.exception.IllegalPathException;
import org.structr.schema.action.Actions;

/**
 *
 */
public class SchemaMethodResource extends SortableResource {

	private String methodName = null;

	@Override
	public boolean checkAndConfigure(final String part, final SecurityContext securityContext, final HttpServletRequest request) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);
		try (final Tx tx = app.tx()) {

			// we only need to check if the method in question exists at all,
			// the actual check for correct type etc. are carried out elsewhere
			final boolean exists = app.nodeQuery(SchemaMethod.class).andName(part).getFirst() != null;

			tx.success();

			if (exists) {
				methodName = part;
				return true;
			}
		}

		return false;
	}

	@Override
	public String getResourceSignature() {
		return methodName;
	}

	@Override
	public Result doGet(final PropertyKey sortKey, final boolean sortDescending, final int pageSize, final int page, final String offsetId) throws FrameworkException {
		throw new IllegalMethodException("PUT not allowed on " + getResourceSignature());
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

		if (wrappedResource != null && wrappedResource instanceof TypeResource) {

			final App app            = StructrApp.getInstance(securityContext);
			final TypeResource other = (TypeResource)wrappedResource;
			final Class type         = other.getEntityClass();

			if (type != null) {

				try (final Tx tx = app.tx()) {

					final SchemaNode schemaNode = app.nodeQuery(SchemaNode.class).andName(type.getSimpleName()).getFirst();
					if (schemaNode != null) {

						final SchemaMethod method = app.nodeQuery(SchemaMethod.class)
							.and(SchemaMethod.name, methodName)
							.and(SchemaMethod.schemaNode, schemaNode)
							.getFirst();

						if (method != null) {

							final String source = method.getProperty(SchemaMethod.source);
							if (StringUtils.isNotBlank(source)) {

								final Object obj = Actions.execute(securityContext, null, "${" + source + "}", propertySet);
								if (obj instanceof RestMethodResult) {

									return (RestMethodResult)obj;

								} else {

									final RestMethodResult result = new RestMethodResult(200);

									// unwrap nested object(s)
									StaticRelationshipResource.unwrapTo(obj, result);

									return result;
								}
							}
						}
					}

					tx.success();
				}
			}
		}

		throw new IllegalPathException("Type and method name do not match the given path.");
	}

	@Override
	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalMethodException("PUT not allowed on " + getResourceSignature());
	}
}
