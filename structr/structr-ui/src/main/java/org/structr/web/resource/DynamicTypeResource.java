/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Direction;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.resource.Resource;
import org.structr.rest.resource.TypeResource;
import org.structr.rest.resource.UuidResource;
import org.structr.web.common.RelationshipHelper;
import org.structr.web.entity.Component;
import org.structr.web.entity.Content;
//~--- JDK imports ------------------------------------------------------------

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class DynamicTypeResource extends TypeResource {

	public static final Logger logger = Logger.getLogger(DynamicTypeResource.class.getName());

	//~--- fields ---------------------------------------------------------

	private final List<DynamicTypeResource> nestedResources = new ArrayList<DynamicTypeResource>();
	private UuidResource uuidResource                 = null;
	private boolean parentResults                     = false;

	//~--- methods --------------------------------------------------------

	@Override
	public boolean checkAndConfigure(final String part, final SecurityContext securityContext, final HttpServletRequest request) throws FrameworkException {

		this.securityContext = securityContext;
		this.request         = request;
		this.rawType         = part;

		super.checkAndConfigure(part, securityContext, request);

		// FIXME: do type check on existing dynamic resources here..
		return rawType != null;
	}

	@Override
	public String toString() {

		return "DynamicTypeResource(".concat(this.rawType).concat(")");

	}

	@Override
	public Result doGet(final PropertyKey sortKey, final boolean sortDescending, final int pageSize, final int page, final String offsetId) throws FrameworkException {

		List<GraphObject> uuidResults = null;

		// REST path contained uuid, return result of UuidResource
		if (uuidResource != null) {

			uuidResource.setSecurityContext(this.securityContext);

			uuidResults = (List<GraphObject>) uuidResource.doGet(sortKey, sortDescending, pageSize, page, offsetId).getResults();

		}

		// check for dynamic type, use super class otherwise
		final List<SearchAttribute> searchAttributes = getSearchAttributes(rawType);

		searchAttributes.addAll(extractSearchableAttributesFromRequest(securityContext));

		// do search
		List<Component> results = getComponents(securityContext, searchAttributes);

		if (!results.isEmpty()) {

			// intersect results with uuid result
			if (uuidResults != null) {

				results = ListUtils.intersection(results, uuidResults);
			}

			// check if nested DynamicTypeResources have valid results
			for (final DynamicTypeResource res : nestedResources) {

				if (res.doGet(sortKey, sortDescending, pageSize, page, offsetId).getResults().isEmpty()) {

					throw new NotFoundException();
				}

			}

			return new Result(results, null, isCollectionResource(), isPrimitiveArray());
		}

		parentResults = true;

		return super.doGet(sortKey, sortDescending, pageSize, page, offsetId);

	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

		// REST path contained uuid, POST not allowed here
		if (uuidResource != null) {

			throw new IllegalPathException();
		}

		final List<? extends GraphObject> templates = doGet(null, false, NodeFactory.DEFAULT_PAGE_SIZE, NodeFactory.DEFAULT_PAGE, null).getResults();

		if (parentResults) {

			return super.doPost(propertySet);
		} else if (!templates.isEmpty()) {

			// try to find ID if surrounding component
			String surroundingComponentId = null;

			if (wrappedResource != null && wrappedResource instanceof UuidResource) {

				surroundingComponentId = ((UuidResource) wrappedResource).getUuid();
			} else if (!nestedResources.isEmpty()) {

				final DynamicTypeResource nested = nestedResources.get(nestedResources.size() - 1);

				if (nested.uuidResource != null) {

					surroundingComponentId = nested.uuidResource.getUuid();
				}

			}

			final Component newComponent  = duplicateComponent(securityContext, propertySet, rawType, surroundingComponentId);
			final RestMethodResult result = new RestMethodResult(201);

			if (newComponent != null) {

				result.addHeader("Location", buildLocationHeader(newComponent));
			}

			return result;
		} else {

			return super.doPost(propertySet);
		}
	}

	@Override
	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {

		if (uuidResource != null) {

			uuidResource.setSecurityContext(this.securityContext);

			return uuidResource.doPut(propertySet);

		}

		throw new IllegalPathException();

	}

	@Override
	public Resource tryCombineWith(final Resource next) throws FrameworkException {

		final int x = 0;

		if (next instanceof UuidResource) {

			this.uuidResource = (UuidResource) next;

			return this;

		} else if (next instanceof DynamicTypeResource) {

			((DynamicTypeResource) next).nestedResources.add(this);

			return next;

		} else if (next instanceof TypeResource) {

			throw new IllegalPathException();
		}

		return super.tryCombineWith(next);

	}

	public static Component duplicateComponent(final SecurityContext securityContext, final Map<String, Object> propertySet, final String rawType, final String surroundingComponentId)
		throws FrameworkException {

		final List<Component> templates            = getComponents(SecurityContext.getSuperUserInstance(), getSearchAttributes(rawType));
		final CreateNodeCommand createNodeCommand  = Services.command(securityContext, CreateNodeCommand.class);
		final String componentId		   = UUID.randomUUID().toString().replaceAll("[\\-]+", "");
		final Component template		   = templates.get(templates.size()-1);
		final PropertyMap templateProperties	   = new PropertyMap();

		// copy properties to map
		templateProperties.put(AbstractNode.type,                        Component.class.getSimpleName());
		templateProperties.put(Component.kind,                           template.getProperty(Component.kind));
		templateProperties.put(AbstractNode.uuid,                        componentId);
		templateProperties.put(AbstractNode.visibleToPublicUsers,        template.getBooleanProperty(AbstractNode.visibleToPublicUsers));
		templateProperties.put(AbstractNode.visibleToAuthenticatedUsers, template.getBooleanProperty(AbstractNode.visibleToAuthenticatedUsers));

		// use parentId from template
		String parentComponentId = template.getComponentId();

		propertySet.remove("pageId");

		if (surroundingComponentId != null) {

			parentComponentId = surroundingComponentId;
		}

		final String finalParentComponentId = parentComponentId;
		final Component newComponent              = (Component) Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				final Component comp = (Component)createNodeCommand.execute(templateProperties);

				RelationshipHelper.copyRelationships(SecurityContext.getSuperUserInstance(), template, comp, RelType.CONTAINS, finalParentComponentId, true);

				// RelationshipHelper.tagOutgoingRelsWithComponentId(comp, comp, comp.getUuid());
				final PropertyMap contentTemplateProperties = new PropertyMap();

				for (final AbstractNode node : template.getContentNodes().values()) {

					// copy content properties
					if (node instanceof Content) {

						final Content contentTemplate = (Content) node;
						final String dataKey          = contentTemplate.getProperty(Content.dataKey);

						// create new content node with content from property set
						contentTemplateProperties.clear();
						
						contentTemplateProperties.put(AbstractNode.type,        Content.class.getSimpleName());
						contentTemplateProperties.put(Content.typeDefinitionId,	contentTemplate.getProperty(Content.typeDefinitionId));
						contentTemplateProperties.put(Content.dataKey,          dataKey);
						
						contentTemplateProperties.put(AbstractNode.visibleToPublicUsers,	contentTemplate.getBooleanProperty(AbstractNode.visibleToPublicUsers));
						contentTemplateProperties.put(AbstractNode.visibleToAuthenticatedUsers,	contentTemplate.getBooleanProperty(AbstractNode.visibleToAuthenticatedUsers));
						
//						contentTemplateProperties.put(Content.UiKey.validationExpression.name(),			contentTemplate.getProperty(Content.UiKey.validationExpression.name()));
//						contentTemplateProperties.put(Content.UiKey.validationErrorMessage.name(),		contentTemplate.getProperty(Content.UiKey.validationErrorMessage.name()));

						final Content newContent = (Content) createNodeCommand.execute(contentTemplateProperties);

						Object dataKeyValue = propertySet.get(dataKey);
						if (dataKeyValue != null) {
							newContent.setProperty(Content.content, dataKeyValue.toString());
						}

						// remove non-local data key from set
						propertySet.remove(dataKey);
						RelationshipHelper.copyRelationships(SecurityContext.getSuperUserInstance(), contentTemplate, newContent, RelType.CONTAINS, componentId, false);

					}
				}

				return comp;

			}

		});

		if (newComponent != null) {
			
			propertySet.remove(AbstractNode.createdDate.dbName());
			propertySet.remove(AbstractNode.lastModifiedDate.dbName());

			for (final String keyName : propertySet.keySet()) {

				PropertyKey key = EntityContext.getPropertyKeyForDatabaseName(Component.class, keyName);
				newComponent.setProperty(key, propertySet.get(keyName));
			}

		}

		return newComponent;

	}

	//~--- get methods ----------------------------------------------------

	// ----- public static methods -----
	public static List<SearchAttribute> getSearchAttributes(final String rawType) {

		// check for dynamic type, use super class otherwise
		final List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

		if (rawType != null) {

			searchAttributes.add(Search.andExactProperty(Component.kind, EntityContext.normalizeEntityName(rawType)));
			searchAttributes.add(Search.andExactType(Component.class.getSimpleName()));

		}

		return searchAttributes;
	}

	/**
	 * Get all active components.
	 *
	 * Active means "has at least one incoming CONTAINS relationship"
	 *
	 * @param securityContext
	 * @param searchAttributes
	 * @return
	 * @throws FrameworkException
	 */
	public static List<Component> getComponents(final SecurityContext securityContext, final List<SearchAttribute> searchAttributes) throws FrameworkException {

		// check for dynamic type, use super class otherwise

		// do search
		final Result<Component> searchResults = (Result) Services.command(securityContext, SearchNodeCommand.class).execute(searchAttributes);
		final List<Component> filteredResults = new LinkedList<Component>();

		for (final Component res : searchResults.getResults()) {

			if (res.hasRelationship(RelType.CONTAINS, Direction.INCOMING)) {
				filteredResults.add(res);
			}
		}

		return filteredResults;
	}

	public static long getMaxPosition(final List<GraphObject> templates, final String pageId) {

		long pos = 0;

		for (final GraphObject template : templates) {

			if (template instanceof Component) {

				final Set<String> paths = (Set<String>) template.getProperty(Component.paths);

				for (final String path : paths) {

					final Long p = Long.parseLong(StringUtils.substringAfterLast(path, "_"));
					pos = Math.max(pos, p == null ? 0 : p);
				}

			}

		}

		return pos;

	}

	@Override
	public boolean isCollectionResource() {

		if (uuidResource != null) {

			return uuidResource.isCollectionResource();
		}

		return true;

	}

}
