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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.search.SortOrder;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.graph.Tx;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotAllowedException;
import org.structr.schema.SchemaHelper;

import java.util.List;
import java.util.Map;

/**
 *
 *
 */
public class GlobalSchemaMethodResource extends Resource {

	private static final Logger logger = LoggerFactory.getLogger(GlobalSchemaMethodResource.class);
	private String methodName          = null;
	private SchemaMethod method        = null;

	@Override
	public boolean checkAndConfigure(final String part, final SecurityContext securityContext, final HttpServletRequest request) {

		this.securityContext = securityContext;
		final App app        = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final List<SchemaMethod> methods = app.nodeQuery(SchemaMethod.class).andName(part).and(SchemaMethod.schemaNode, null).getAsList();

			tx.success();

			if (!methods.isEmpty()) {

				method = methods.get(0);

				this.methodName = part;

				return true;
			}

		} catch (FrameworkException fex) {
			logger.error(ExceptionUtils.getStackTrace(fex));
		}

		return false;
	}

	@Override
	public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {
		throw new NotAllowedException("GET not allowed, use POST to execute schema methods.");
	}

	@Override
	public RestMethodResult doPut(Map<String, Object> propertySet) throws FrameworkException {
		throw new NotAllowedException("PUT not allowed, use POST to execute schema methods.");
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

		//return SchemaMethodResource.wrapInResult(Actions.callWithSecurityContext(methodName, securityContext, propertySet));

		final App app           = StructrApp.getInstance(securityContext);
		RestMethodResult result = null;

		if (method != null) {

			try (final Tx tx = app.tx()) {

				final String source = method.getProperty(SchemaMethod.source);

				result = SchemaMethodResource.invoke(securityContext, null, source, propertySet, methodName, method.getUuid());

				tx.success();
			}
		}

		if (result == null) {
			throw new IllegalPathException("Type and method name do not match the given path.");
		}

		return result;
	}

	@Override
	public Resource tryCombineWith(final Resource next) throws FrameworkException {
		return null;
	}

	@Override
	public Class getEntityClass() {
		return null;
	}

	@Override
	public String getUriPart() {
		return methodName;
	}

	@Override
	public boolean isCollectionResource() {
		return false;
	}

	@Override
	public String getResourceSignature() {
		return SchemaHelper.normalizeEntityName(getUriPart());
	}
}
