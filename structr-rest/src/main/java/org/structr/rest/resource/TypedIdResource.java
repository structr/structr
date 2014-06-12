/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.resource;

import org.structr.core.Result;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.SchemaHelper;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.exception.NotFoundException;

/**
 * Represents a type-constrained ID match. A TypedIdResource will always
 * result in a single element.
 *
 * @author Christian Morgner
 */
public class TypedIdResource extends FilterableResource {

	protected TypeResource typeResource = null;
	protected UuidResource idResource = null;

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
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {
		return new Result(getEntity(), isPrimitiveArray());
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalMethodException();
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

		if (next instanceof TypeResource) {

			// next constraint is a type constraint
			// => follow predefined statc relationship
			//    between the two types
			StaticRelationshipResource resource = new StaticRelationshipResource(securityContext, this, (TypeResource)next);
			resource.configureIdProperty(idProperty);
			return resource;

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

		GraphObject entity = idResource.getEntity();
		String type        = SchemaHelper.normalizeEntityName(typeResource.getRawType());
		Class parentClass  = SchemaHelper.getEntityClassForRawType(type);
		Class entityClass  = entity.getClass();

		if (parentClass.isAssignableFrom(entityClass)) {
			return entity;
		}

		throw new NotFoundException();

	}
}
