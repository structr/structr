/*
 *  Copyright (C) 2011 Axel Morgner
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



package org.structr.web.entity;

import org.apache.lucene.queryParser.QueryParser;
import org.neo4j.graphdb.Direction;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.web.entity.html.*;
import org.structr.core.entity.DirectedRelation.Cardinality;
import org.structr.core.node.search.Search;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a content container
 *
 * @author axel
 */
public class Content extends AbstractNode {

	public enum UiKey implements PropertyKey{ name, tag, elements, content, contentType, size; }

	static {
		EntityContext.registerPropertySet(Content.class,	PropertyView.All,	UiKey.values());
		EntityContext.registerPropertySet(Content.class,	PropertyView.Public,	UiKey.values());
		EntityContext.registerPropertySet(Content.class,	PropertyView.Ui,	UiKey.values());

		EntityContext.registerEntityRelation(Content.class,	Element.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		
		EntityContext.registerEntityRelation(Content.class,	org.structr.web.entity.html.Title.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
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

	}


	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "";
	}

	@Override
	public java.lang.Object getPropertyForIndexing(final String key) {

		if (key.equals(Content.UiKey.content.name())) {

			String value = getStringProperty(key);
			return Search.escapeForLucene(value);


		} else {

		return getProperty(key);
		}
	}
}
