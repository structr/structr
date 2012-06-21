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



package org.structr.web.entity;

import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.neo4j.graphdb.Direction;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.web.entity.html.*;
import org.structr.core.node.NodeService;
import org.structr.core.node.search.Search;
import org.structr.core.entity.RelationClass.Cardinality;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a content container
 *
 * @author axel
 */
public class Content extends AbstractNode {

	private static final Logger logger = Logger.getLogger(Content.class.getName());

	public enum UiKey implements PropertyKey{ name, tag, content, contentType, size, type }
	
	protected static final String[] attributes = new String[] {
		
		UiKey.name.name(),
		UiKey.tag.name(),
		UiKey.content.name(),
		UiKey.contentType.name(),
		UiKey.size.name(),
		UiKey.type.name(),
		
		// support for microformats
		"data-key"
	};


	static {
		EntityContext.registerPropertySet(Content.class,	PropertyView.All,	attributes);
		EntityContext.registerPropertySet(Content.class,	PropertyView.Public,	attributes);
		EntityContext.registerPropertySet(Content.class,	PropertyView.Ui,	attributes);

		EntityContext.registerEntityRelation(Content.class,	Element.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		
		EntityContext.registerEntityRelation(Content.class,	Title.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Body.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Style.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Script.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	P.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Div.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	H1.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	H2.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	H3.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	H4.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	H5.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	H6.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	A.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Em.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Strong.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Small.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	S.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Cite.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	G.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Dfn.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Abbr.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Time.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Code.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Var.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Samp.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Kbd.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Sub.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Sup.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	I.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	B.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	U.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Mark.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Ruby.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Rt.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Rp.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Bdi.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Bdo.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class,	Span.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);

		EntityContext.registerSearchablePropertySet(Content.class, NodeService.NodeIndex.fulltext.name(), UiKey.values());
		EntityContext.registerSearchablePropertySet(Content.class, NodeService.NodeIndex.keyword.name(), UiKey.values());

	}
	
	//~--- get methods ----------------------------------------------------

	@Override
	public java.lang.Object getPropertyForIndexing(final String key) {

		if (key.equals(Content.UiKey.content.name())) {

			String value = getStringProperty(key);
			return Search.escapeForLucene(value);


		} else {

                    return getProperty(key);
		}
	}
	
	public Element getParent() {
		// FIXME: this is an ugly hack :)
		return (Element)getRelToParent().getStartNode();
	}
	
	public AbstractRelationship getRelToParent() {
		// FIXME: this is an ugly hack :)
		return getRelationships(RelType.CONTAINS, Direction.INCOMING).get(0);
	}	
	
	public String getPropertyWithVariableReplacement(HttpServletRequest request, AbstractNode page, String pageId, String componentId, AbstractNode viewComponent, String key) {
		if (securityContext.getRequest() == null) {
			securityContext.setRequest(request);
		}
		return HtmlElement.replaceVariables(securityContext, page, this, pageId, componentId, viewComponent, super.getStringProperty(key));
	}
}
