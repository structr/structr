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


import jakarta.servlet.http.HttpServletRequest;
import org.structr.api.search.SortOrder;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.schema.SchemaHelper;

import java.util.Map;

/**
 *
 */
public class SchemaTypeResource extends Resource {

	protected HttpServletRequest request = null;
	protected TypeResource typeResource  = null;
	protected Class entityClass          = null;
	private String propertyView          = null;
	private String rawType               = null;

	public SchemaTypeResource(SecurityContext securityContext, TypeResource typeResource) {
		this.securityContext = securityContext;
		this.typeResource = typeResource;
		this.rawType = typeResource.getRawType();
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {
		return true;
	}

	@Override
	public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {
		final Class type = typeResource.getEntityClass();
		return getSchemaTypeResult(securityContext, rawType, type, propertyView);
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalMethodException("POST not allowed on " + getResourceSignature());
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if (next instanceof ViewFilterResource) {

			propertyView = ((ViewFilterResource) next).getPropertyView();
		}

		return this;
	}

	@Override
	public String getUriPart() {
		return rawType;
	}

	public String getRawType() {
		return rawType;
	}

	@Override
	public Class getEntityClass() {
		return entityClass;
	}

	@Override
	public String getResourceSignature() {
		return SchemaResource.UriPart._schema.name().concat("/").concat(SchemaHelper.normalizeEntityName(getUriPart()));
	}

	@Override
	public boolean isCollectionResource() {
		return true;
	}

	// ----- public static methods -----
	public static ResultStream getSchemaTypeResult(final SecurityContext securityContext, final String rawType, final Class type, final String propertyView) throws FrameworkException {
		return new PagingIterable<>("getSchemaTypeResult(" + rawType + ")", SchemaHelper.getSchemaTypeInfo(securityContext, rawType, type, propertyView));
	}
}
