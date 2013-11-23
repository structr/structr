/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.resource;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotFoundException;
import org.structr.core.entity.PropertyDefinition;
import org.structr.core.graph.NodeInterface;
import org.structr.core.schema.SchemaHelper;

/**
 *
 * @author Christian Morgner
 */
public class DynamicTypeResource extends TypeResource {

	private static final Logger logger = Logger.getLogger(TypeResource.class.getName());

	private String normalizedTypeName = null;
	
	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

		this.securityContext = securityContext;
		this.request         = request;
		this.rawType         = part;

		if (part != null) {

			this.normalizedTypeName = SchemaHelper.normalizeEntityName(part);
			
			if (PropertyDefinition.exists(this.normalizedTypeName)) {
				
				entityClass = PropertyDefinition.nodeExtender.getType(normalizedTypeName);
				
				return true;
			}
		}

		return false;

	}

	@Override
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		List<SearchAttribute> searchAttributes = new LinkedList<>();
		boolean includeDeletedAndHidden        = true;
		boolean publicOnly                     = false;

		if (rawType != null) {

			if (entityClass == null) {

				throw new NotFoundException();
			}

			searchAttributes.add(Search.andExactType(entityClass));
			searchAttributes.addAll(extractSearchableAttributes(securityContext, entityClass, request));
			
			// do search
			Result results = StructrApp.getInstance(securityContext).command(SearchNodeCommand.class).execute(
				includeDeletedAndHidden,
				publicOnly,
				searchAttributes,
				sortKey,
				sortDescending,
				pageSize,
				page,
				offsetId
			);
			
			return results;
			
		} else {

			logger.log(Level.WARNING, "type was null");
		}

		List emptyList = Collections.emptyList();
		return new Result(emptyList, null, isCollectionResource(), isPrimitiveArray());
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

		final App app         = StructrApp.getInstance(securityContext);
		NodeInterface newNode = null;
		// create transaction closure
		
		try {
			app.beginTx();
			newNode = createNode(propertySet);
			app.commitTx();

		} finally {
			app.finishTx();
		}

		// execute transaction: create new node
		RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_CREATED);

		if (newNode != null) {

			result.addHeader("Location", buildLocationHeader(newNode));
		}
		
		// finally: return 201 Created
		return result;
	}

	@Override
	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {

		throw new IllegalPathException();

	}

	@Override
	public RestMethodResult doHead() throws FrameworkException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public NodeInterface createNode(final Map<String, Object> propertySet) throws FrameworkException {

		final App app    = StructrApp.getInstance(securityContext);
		final Class type = SchemaHelper.getEntityClassForRawType(normalizedTypeName);
		
		if (type != null) {
			
			final PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, entityClass, propertySet);
			
			try {
				
				app.beginTx();
				NodeInterface node = app.create(type, properties);
				app.commitTx();
				
				return node;
				
			} finally {
				
				app.finishTx();
			}
		}
		
		throw new FrameworkException(500, "Cannot create node with unknow type " + normalizedTypeName);
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if (next instanceof UuidResource) {

			TypedIdResource constraint = new TypedIdResource(securityContext, (UuidResource) next, this);

			constraint.configureIdProperty(idProperty);

			return constraint;

		} else if (next instanceof TypeResource) {

			throw new IllegalPathException();
		}

		return super.tryCombineWith(next);

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getUriPart() {

		return rawType;

	}

	@Override
	public Class getEntityClass() {
		return entityClass;
	}

	@Override
	public String getResourceSignature() {

		return SchemaHelper.normalizeEntityName(rawType);

	}

	@Override
	public boolean isCollectionResource() {

		return true;

	}
}
