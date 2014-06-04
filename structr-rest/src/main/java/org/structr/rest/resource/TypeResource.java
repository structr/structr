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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.common.GraphObjectComparator;
import org.structr.common.SecurityContext;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.graph.search.SearchRelationshipCommand;
import org.structr.core.notion.Notion;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.servlet.JsonRestServlet;
import org.structr.schema.SchemaHelper;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a bulk type match. A TypeResource will always result in a
 * list of elements when it is the last element in an URI. A TypeResource
 * that is not the first element in an URI will try to find a pre-defined
 * relationship between preceding and the node type (defined by
 * {@see AbstractNode#getRelationshipWith}) and follow that path.
 *
 * @author Christian Morgner
 */
public class TypeResource extends SortableResource {

	private static final Logger logger = Logger.getLogger(TypeResource.class.getName());

	//~--- fields ---------------------------------------------------------

	protected Class<? extends SearchCommand> searchCommandType = null;
	protected Class entityClass                                = null;
	protected String rawType                                   = null;
	protected HttpServletRequest request                       = null;
	protected Query query                                      = null;
	protected boolean isNode                                   = true;

	//~--- methods --------------------------------------------------------

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

//		this.searchContext   = new SearchContext();
		this.securityContext = securityContext;
		this.request         = request;
		this.rawType         = part;

		if (rawType != null) {

			final boolean inexactSearch = parseInteger(request.getParameter(JsonRestServlet.REQUEST_PARAMETER_LOOSE_SEARCH)) == 1;
			final App app               = StructrApp.getInstance(securityContext);

			// test if resource class exists
			entityClass = SchemaHelper.getEntityClassForRawType(rawType);
			if (entityClass != null) {

				if (AbstractRelationship.class.isAssignableFrom(entityClass)) {

					searchCommandType = SearchRelationshipCommand.class;
					query             = app.relationshipQuery(entityClass, !inexactSearch);
					isNode            = false;
					return true;

				} else {

					// include interfaces here
					searchCommandType = SearchNodeCommand.class;
					query             = app.nodeQuery(entityClass, !inexactSearch);
					isNode            = true;
					return true;
				}
			}
		}

		return true;

	}

	@Override
	public Result doGet(final PropertyKey sortKey, final boolean sortDescending, final int pageSize, final int page, final String offsetId) throws FrameworkException {

		boolean includeDeletedAndHidden        = false;
		boolean publicOnly                     = false;
		PropertyKey actualSortKey              = sortKey;
		boolean actualSortOrder                = sortDescending;

		if (rawType != null) {

			if (entityClass == null) {
				throw new NotFoundException();
			}

			collectSearchAttributes(query);

			// default sort key & order
			if (actualSortKey == null) {

				try {

					GraphObject templateEntity  = ((GraphObject)entityClass.newInstance());
					PropertyKey sortKeyProperty = templateEntity.getDefaultSortKey();
					actualSortOrder             = GraphObjectComparator.DESCENDING.equals(templateEntity.getDefaultSortOrder());

					if (sortKeyProperty != null) {

						actualSortKey = sortKeyProperty;

					} else {

						actualSortKey = AbstractNode.name;
					}

				} catch(Throwable t) {

					// fallback to name
					actualSortKey = AbstractNode.name;
				}
			}

			// do search: FIXME: this doesn't work for inexact search because
			// the type search attribute has to be lowercase in the fulltext indices..
			return query
				.includeDeletedAndHidden(includeDeletedAndHidden)
				.publicOnly(publicOnly)
				.sort(actualSortKey)
				.order(actualSortOrder)
				.pageSize(pageSize)
				.page(page)
				.offsetId(offsetId)
				.getResult();

		} else {

			logger.log(Level.WARNING, "type was null");
		}

		List emptyList = Collections.emptyList();
		return new Result(emptyList, null, isCollectionResource(), isPrimitiveArray());
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

		if (isNode) {

			final App app         = StructrApp.getInstance(securityContext);
			NodeInterface newNode = null;

			newNode = createNode(propertySet);

			RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_CREATED);
			if (newNode != null) {

				result.addHeader("Location", buildLocationHeader(newNode));
			}

			// finally: return 201 Created
			return result;

		} else {

			final App app                         = StructrApp.getInstance(securityContext);
			final Relation template               = getRelationshipTemplate();
			final ErrorBuffer errorBuffer         = new ErrorBuffer();

			if (template != null) {

				final NodeInterface sourceNode        = identifyStartNode(template, propertySet);
				final NodeInterface targetNode        = identifyEndNode(template, propertySet);
				final PropertyMap properties          = PropertyMap.inputTypeToJavaType(securityContext, entityClass, propertySet);
				RelationshipInterface newRelationship = null;

				if (sourceNode == null) {
					errorBuffer.add(entityClass.getSimpleName(), new EmptyPropertyToken(template.getSourceIdProperty()));
				}

				if (targetNode == null) {
					errorBuffer.add(entityClass.getSimpleName(), new EmptyPropertyToken(template.getTargetIdProperty()));
				}

				if (errorBuffer.hasError()) {
					throw new FrameworkException(422, errorBuffer);
				}

				newRelationship = app.create(sourceNode, targetNode, entityClass, properties);

				RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_CREATED);
				if (newRelationship != null) {

					result.addHeader("Location", buildLocationHeader(newRelationship));
				}

				// finally: return 201 Created
				return result;
			}

			// shouldn't happen
			throw new NotFoundException();
		}
	}

	@Override
	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalPathException();
	}

	public NodeInterface createNode(final Map<String, Object> propertySet) throws FrameworkException {

		if (entityClass != null) {

			final App app                = StructrApp.getInstance(securityContext);
			final PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, entityClass, propertySet);

			return app.create(entityClass, properties);
		}

		throw new NotFoundException();
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

	public void setEntityClass(final Class<? extends GraphObject> type) {
		this.entityClass = type;
	}

	public void setSearchCommandType(final Class<? extends SearchCommand> searchCommand) {
		this.searchCommandType = searchCommand;
	}

	@Override
	public String getResourceSignature() {
		return SchemaHelper.normalizeEntityName(getUriPart());
	}

	@Override
	public boolean isCollectionResource() {
		return true;
	}

	public void collectSearchAttributes(final Query query) throws FrameworkException {

		// first step: extract searchable attributes from request
		extractSearchableAttributes(securityContext, entityClass, request, query);

		// second step: distance search?
		extractDistanceSearch(request, query);
	}

	// ----- private methods -----
	private Relation getRelationshipTemplate() {

		try {

			return (Relation)entityClass.newInstance();

		} catch (Throwable t) {

		}

		return null;
	}

	private NodeInterface identifyStartNode(final Relation template, final Map<String, Object> properties) throws FrameworkException {

		final Property<String> sourceIdProperty = template.getSourceIdProperty();
		final Class sourceType                  = template.getSourceType();
		final Notion notion                     = template.getStartNodeNotion();

		notion.setType(sourceType);

                PropertyKey startNodeIdentifier = notion.getPrimaryPropertyKey();

                if (startNodeIdentifier != null) {

                        Object identifierValue = properties.get(startNodeIdentifier.dbName());

                        properties.remove(sourceIdProperty.dbName());

                        return (NodeInterface)notion.getAdapterForSetter(securityContext).adapt(identifierValue);

                }

		return null;
	}

	private NodeInterface identifyEndNode(final Relation template, final Map<String, Object> properties) throws FrameworkException {

		final Property<String> targetIdProperty = template.getTargetIdProperty();
		final Class targetType                  = template.getTargetType();
		final Notion notion                     = template.getEndNodeNotion();

		notion.setType(targetType);

                final PropertyKey endNodeIdentifier = notion.getPrimaryPropertyKey();
                if (endNodeIdentifier != null) {

                        Object identifierValue = properties.get(endNodeIdentifier.dbName());

                        properties.remove(targetIdProperty.dbName());

                        return (NodeInterface)notion.getAdapterForSetter(securityContext).adapt(identifierValue);

                }

		return null;
	}
}
