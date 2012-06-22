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

import org.apache.commons.collections.ListUtils;

import org.neo4j.graphdb.Direction;

import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.node.*;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
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

import java.util.*;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class DynamicTypeResource extends TypeResource {

	public static final Logger logger = Logger.getLogger(DynamicTypeResource.class.getName());

	//~--- fields ---------------------------------------------------------

	private List<DynamicTypeResource> nestedResources = new ArrayList<DynamicTypeResource>();
	private UuidResource uuidResource                 = null;
	private boolean parentResults                     = false;

	//~--- methods --------------------------------------------------------

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

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
	public List<GraphObject> doGet() throws FrameworkException {

		List<GraphObject> uuidResults = null;

		// REST path contained uuid, return result of UuidResource
		if (uuidResource != null) {

			uuidResults = (List<GraphObject>) uuidResource.doGet();
		}

		// check for dynamic type, use super class otherwise
		List<SearchAttribute> searchAttributes = getSearchAttributes(rawType);

		hasSearchableAttributesForNodes(rawType, request, searchAttributes);

		// do search
		List<GraphObject> results = getComponents(securityContext, searchAttributes);

		if (!results.isEmpty()) {

			// intersect results with uuid result
			if (uuidResults != null) {

				results = ListUtils.intersection(results, uuidResults);
			}

			// check if nested DynamicTypeResources have valid results
			for (DynamicTypeResource res : nestedResources) {

				if (res.doGet().isEmpty()) {

					throw new NotFoundException();
				}

			}

			return results;
		}

		parentResults = true;

		return super.doGet();

	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

		// REST path contained uuid, POST not allowed here
		if (uuidResource != null) {

			throw new IllegalPathException();
		}

		List<GraphObject> templates = doGet();

		if (parentResults) {

			return super.doPost(propertySet);
		} else if (!templates.isEmpty()) {

			// try to find ID if surrounding component
			String surroundingComponentId = null;

			if ((wrappedResource != null) && (wrappedResource instanceof UuidResource)) {

				surroundingComponentId = ((UuidResource) wrappedResource).getUuid();
			} else if (!nestedResources.isEmpty()) {

				DynamicTypeResource nested = nestedResources.get(nestedResources.size() - 1);

				if (nested.uuidResource != null) {

					surroundingComponentId = nested.uuidResource.getUuid();
				}

			}

			Component newComponent  = duplicateComponent(securityContext, propertySet, rawType, surroundingComponentId);
			RestMethodResult result = new RestMethodResult(201);

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

			return uuidResource.doPut(propertySet);
		}

		throw new IllegalPathException();

	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		int x = 0;

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

		final List<GraphObject> templates            = getComponents(securityContext, getSearchAttributes(rawType));
		final Command createNodeCommand              = Services.command(securityContext, CreateNodeCommand.class);
		final Map<String, Object> templateProperties = new LinkedHashMap<String, Object>();
		final String componentId                     = UUID.randomUUID().toString().replaceAll("[\\-]+", "");
		final Component template                     = (Component) templates.get(0);

		// copy properties to map
		templateProperties.put(AbstractNode.Key.type.name(), Component.class.getSimpleName());
		templateProperties.put(Component.UiKey.kind.name(), template.getStringProperty(Component.UiKey.kind.name()));
		templateProperties.put(AbstractNode.Key.uuid.name(), componentId);

		// use parentId from template
		String parentComponentId = template.getComponentId();
		String parentResourceId  = template.getPageId();
		final long position      = getMaxPosition(templates, parentResourceId) + 1;

		if (surroundingComponentId != null) {

			parentComponentId = surroundingComponentId;
		}

		final String finalParentComponentId = parentComponentId;
		final String finalParentResourceId  = parentResourceId;
		Component newComponent              = (Component) Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				Component comp = (Component) createNodeCommand.execute(templateProperties);

				RelationshipHelper.copyRelationships(securityContext, template, comp, RelType.CONTAINS, finalParentResourceId, finalParentComponentId, position);

				// RelationshipHelper.tagOutgoingRelsWithComponentId(comp, comp, comp.getUuid());
				Map<String, Object> contentTemplateProperties = new LinkedHashMap<String, Object>();

				for (AbstractNode node : template.getContentNodes().values()) {

					// copy content properties
					if (node instanceof Content) {

						Content contentTemplate = (Content) node;
						String dataKey          = contentTemplate.getStringProperty("data-key");

						// create new content node with content from property set
						contentTemplateProperties.clear();
						contentTemplateProperties.put(AbstractNode.Key.type.name(), "Content");
						contentTemplateProperties.put("data-key", dataKey);
						contentTemplateProperties.put("content", propertySet.get(dataKey));

						Content newContent = (Content) createNodeCommand.execute(contentTemplateProperties);

						// remove non-local data key from set
						propertySet.remove(dataKey);
						RelationshipHelper.copyRelationships(securityContext, contentTemplate, newContent, RelType.CONTAINS, finalParentResourceId, componentId, position);

					}
				}

				return comp;

			}

		});

		if (newComponent != null) {

			for (String key : propertySet.keySet()) {

				newComponent.setProperty(key, propertySet.get(key));
			}

		}

		/**
		 * not used, was "link components with DATA relationships together"
		 *
		 * // make new component collect content properties right now
		 * newComponent.onNodeInstantiation();
		 *
		 * // create linked component
		 * for(AbstractRelationship rel : template.getRelationships(RelType.DATA, Direction.INCOMING)) {
		 *
		 *       AbstractNode dataNode = rel.getStartNode();
		 *       if(dataNode instanceof Component) {
		 *
		 *               Component linkedComponent = (Component)dataNode;
		 *
		 *               // prepare property set for linked POST
		 *               Map<String, Object> postPropertySet = new LinkedHashMap<String, Object>();
		 *               String rawNames = rel.getStringProperty("names");
		 *               String[] names = rawNames.split("[, ]+");
		 *
		 *               // copy properties from new component according to property keys on relationship
		 *               for(String key : names) {
		 *
		 *                       Object value = newComponent.getProperty(key);
		 *                       if(value != null) {
		 *                               postPropertySet.put(key, value);
		 *                       }
		 *               }
		 *
		 *               String linkedRawType = linkedComponent.getStringProperty(Component.UiKey.structrclass);
		 *
		 *               Component newLinkedComponent = duplicateComponent(securityContext, postPropertySet, linkedRawType, null);
		 *
		 *               // create new DATA relationship from linked component to new component
		 *               Map<String, Object> newRelProperties = new LinkedHashMap<String, Object>();
		 *               newRelProperties.put(ReferenceRelationship.Key.names.name(), rel.getProperty(ReferenceRelationship.Key.names));
		 *
		 *               Services.command(securityContext, CreateRelationshipCommand.class).execute(newLinkedComponent, newComponent, RelType.DATA, newRelProperties);
		 *       }
		 *
		 * }
		 */
		return newComponent;

	}

	//~--- get methods ----------------------------------------------------

	// ----- public static methods -----
	public static List<SearchAttribute> getSearchAttributes(String rawType) {

		// check for dynamic type, use super class otherwise
		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

		if (rawType != null) {

			searchAttributes.add(Search.andExactProperty(Component.UiKey.kind.name(), EntityContext.normalizeEntityName(rawType)));
			searchAttributes.add(Search.andExactType(Component.class.getSimpleName()));

		}

		return searchAttributes;
	}

	public static List<GraphObject> getComponents(final SecurityContext securityContext, List<SearchAttribute> searchAttributes) throws FrameworkException {

		// check for dynamic type, use super class otherwise
		AbstractNode topNode            = null;
		boolean includeDeletedAndHidden = false;
		boolean publicOnly              = false;

		// do search
		return (List<GraphObject>) Services.command(securityContext, SearchNodeCommand.class).execute(topNode, includeDeletedAndHidden, publicOnly, searchAttributes);
	}

	public static long getMaxPosition(final List<GraphObject> templates, final String pageId) {

		long pos = 0;

		for (GraphObject template : templates) {

			if (template instanceof Component) {

				Component component             = (Component) template;
				List<AbstractRelationship> rels = component.getRelationships(RelType.CONTAINS, Direction.INCOMING);

				for (AbstractRelationship rel : rels) {

					pos = Math.max(pos, rel.getLongProperty(pageId));
				}

			}

		}

		return pos;

	}

	@Override
	public boolean hasSearchableAttributes(List<SearchAttribute> attributes) throws FrameworkException {

		// TODO: search for data-key attributes
		return false;
	}

	@Override
	public boolean isCollectionResource() {

		if (uuidResource != null) {

			return uuidResource.isCollectionResource();
		}

		return true;

	}

}
