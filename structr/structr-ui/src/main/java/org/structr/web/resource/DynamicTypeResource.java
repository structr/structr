/*
 *  Copyright (C) 2012 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.resource;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.resource.TypeResource;
import org.structr.web.entity.html.HtmlElement;

/**
 *
 * @author Christian Morgner
 */
public class DynamicTypeResource extends TypeResource {

	private static final Logger logger = Logger.getLogger(DynamicTypeResource.class.getName());
	
	@Override
	public List<GraphObject> doGet() throws FrameworkException {

		// check for dynamic type, use super class otherwise
		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		AbstractNode topNode                   = null;
		boolean includeDeleted                 = false;
		boolean publicOnly                     = false;

		if (rawType != null) {

			searchAttributes.add(Search.andExactProperty(HtmlElement.UiKey.structrclass.name(), EntityContext.normalizeEntityName(rawType)));
			
			// searchable attributes from EntityContext
			hasSearchableAttributes(rawType, request, searchAttributes);

			// do search
			List<GraphObject> results = (List<GraphObject>) Services.command(securityContext, SearchNodeCommand.class).execute(topNode, includeDeleted, publicOnly, searchAttributes);
			if (!results.isEmpty()) {
			
				
				/*
				TODO: if template is found, collect different content elements!
				we need to synthesize elements here if thereis more than one!
				*/
				
				return results;

			}
		}

			
		return super.doGet();
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {
		
		
		// TODO: implement POSTing of dynamic types

		/**
		 * - rawType contains the desired data-class to duplicate
		 * - we can not create entities without a "template, so there
		 *   must be at least one object of the desired type already in the database
		 * - we need to load a template instance
		 * - there will be only a single template node in the whole graph!
		 * - this template can contain many different content elements
		 */
		List<GraphObject> templates = doGet();
		if(!templates.isEmpty()) {
			
//			final Element template                       = (Element)templates.get(0);   
//			final Map<String, AbstractNode> contentNodes = template.getContentNodes();
//			final Command createNodeCommand              = Services.command(securityContext, CreateNodeCommand.class);
//			final Command createRelCommand               = Services.command(securityContext, CreateRelationshipCommand.class);
//			
//			// TODO: find resourceId for template and modify it
//			
//			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
//
//				@Override
//				public Object execute() throws FrameworkException {
//					
//					if(!propertySet.isEmpty()) {
//						
//						for(String key : propertySet.keySet()) {
//
//							AbstractNode node = contentNodes.get(key);
//							if(node != null && node instanceof Content) {
//								
//								Content content = (Content)node;
//								AbstractNode newNode = (AbstractNode)createNodeCommand.execute(new NodeAttribute(AbstractNode.Key.type.name(), content.getType()));
//								for(Entry<String, Object> entry : propertySet.entrySet()) {
//									newNode.setProperty(entry.getKey(), entry.getValue());
//								}
//
//								// get parent & rel to parent
//								AbstractRelationship parentRel = content.getRelToParent();
//								Element parent = content.getParent();
//								
//								// duplicate relationship to parent & properties
//								AbstractRelationship newRel = (AbstractRelationship)createRelCommand.execute(parent, content, RelType.CONTAINS);
//								for(Entry<String, Object> entry : parentRel.getProperties().entrySet()) {
//									newRel.setProperty(entry.getKey(), entry.getValue());
//								}
//								
//								newRel.setProperty(template.getStringProperty(AbstractNode.Key.uuid), 0);
//							}
//						}
//					}
//					
//					return null;
//				}
//				
//			});
//			
			RestMethodResult result = new RestMethodResult(201);
			return result;
			
		} else {
			
			return super.doPost(propertySet);
		}
	}
}












