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
import jakarta.servlet.http.HttpServletResponse;
import org.structr.api.search.SortOrder;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.GenericNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.graph.search.SearchCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.exception.NotFoundException;
import org.structr.schema.SchemaHelper;

import java.util.Arrays;
import java.util.Map;

/**
 * Represents a type-constrained ID match. A TypedIdResource will always
 * result in a single element.
 *
 *
 */
public class TypedIdResource extends FilterableResource {

	private GraphObject entity          = null;
	protected TypeResource typeResource = null;
	protected UuidResource idResource   = null;

	protected TypedIdResource(SecurityContext securityContext) {
		this.securityContext = securityContext;
		// empty protected constructor
	}

	public TypedIdResource(SecurityContext securityContext, UuidResource idResource, TypeResource typeResource) {
		this.securityContext = securityContext;
		this.typeResource = typeResource;
		this.idResource = idResource;
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {
		return false;	// we will not accept URI parts directly
	}

	@Override
	public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {
		return new PagingIterable<>("/" + getUriPart(), Arrays.asList(getEntity()));
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalMethodException("POST not allowed on " + typeResource.getRawType() + " element resource");
	}

	@Override
	public Class getEntityClass() {
		return typeResource.getEntityClass();
	}

	public TypeResource getTypeResource() {
		return typeResource;
	}

	public UuidResource getIdResource() {
		return idResource;
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if (next instanceof SchemaMethodResource) {

			// make this type resource available to the next resource
			((SchemaMethodResource)next).wrapResource(this);

		} else if (next instanceof TypeResource) {

			// next constraint is a type constraint
			// => follow predefined statc relationship
			//    between the two types
			return new StaticRelationshipResource(securityContext, this, (TypeResource)next);

		} else if (next instanceof RelationshipResource) {

			// make rel constraint wrap this
			((RelationshipResource)next).wrapResource(this);
			return next;
		}

		return super.tryCombineWith(next);
	}

	@Override
	public String getUriPart() {
		return typeResource.getUriPart().concat("/").concat(idResource.getUriPart());
	}

	@Override
	public String getResourceSignature() {
		return typeResource.getResourceSignature();
	}

	@Override
	public boolean isCollectionResource() {
		return false;
	}

	// ----- public methods -----
	public GraphObject getEntity() throws FrameworkException {

		if (entity == null) {

			final App app    = StructrApp.getInstance(securityContext);

			final Class type  = typeResource.entityClass;
			final String uuid = idResource.getUuid();

			if (type == null) {
				if (uuid != null) {
					throw new NotFoundException("Type " + typeResource.getRawType() + " does not exist for request with entity ID " + uuid);
				} else {
					throw new NotFoundException("Request specifies no value for type and entity ID");
				}
			}

			entity = app.nodeQuery(type).uuid(uuid).getFirst();
			if (entity == null) {

				entity = app.relationshipQuery(type).uuid(uuid).getFirst();
			}

			if (entity == null) {
				throw new NotFoundException("Entity with ID " + uuid + " not found");
			}

			final String rawType    = SchemaHelper.normalizeEntityName(typeResource.getRawType());
			final String entityType = entity.getClass().getSimpleName();

			if (GenericNode.class.equals(entity.getClass()) || SearchCommand.getAllSubtypesAsStringSet(rawType).contains(entityType)) {
				return entity;
			}

			// reset cached value
			entity = null;

			throw new NotFoundException("Entity with ID " + idResource.getUuid() + " of type " +  typeResource.getRawType() +  " does not exist");
}

		return entity;
	}

	@Override
	public RestMethodResult doDelete() throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx(true, true, false)) {

			final GraphObject obj = getEntity();

			if (obj.isNode()) {

				final NodeInterface node = (NodeInterface)obj;

				if (!node.isGranted(Permission.delete, securityContext)) {

					return new RestMethodResult(HttpServletResponse.SC_FORBIDDEN);

				} else {

					app.delete(node);

				}

			} else {

				app.delete((RelationshipInterface) obj);
			}

			tx.success();
		}

		return new RestMethodResult(HttpServletResponse.SC_OK);
	}
}
