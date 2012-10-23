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



package org.structr.web.entity.html;

import org.apache.commons.lang.ArrayUtils;
import org.neo4j.graphdb.Direction;
import org.structr.common.Property;

import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.View;
import org.structr.core.EntityContext;
import org.structr.core.entity.RelationClass;
import org.structr.web.common.HtmlProperty;
import org.structr.web.entity.Content;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Form extends HtmlElement {

	public static final Property<String> _acceptCharset = new HtmlProperty("accept-charset");
	public static final Property<String> _action        = new HtmlProperty("action");
	public static final Property<String> _autocomplete  = new HtmlProperty("autocomplete");
	public static final Property<String> _enctype       = new HtmlProperty("enctype");
	public static final Property<String> _method        = new HtmlProperty("method");
	public static final Property<String> _name          = new HtmlProperty("name");
	public static final Property<String> _novalidate    = new HtmlProperty("novalidate");
	public static final Property<String> _target        = new HtmlProperty("target");

	public static final View htmlView = new View(Form.class, PropertyView.Html,
	    _acceptCharset, _action, _autocomplete, _enctype, _method, _name, _novalidate, _target
	);
	
	//~--- static initializers --------------------------------------------

	static {

//		EntityContext.registerPropertySet(Form.class, PropertyView.All, HtmlElement.UiKey.values());
//		EntityContext.registerPropertySet(Form.class, PropertyView.Public, HtmlElement.UiKey.values());
//		EntityContext.registerPropertySet(Form.class, PropertyView.Html, PropertyView.Html, htmlAttributes);
		
		EntityContext.registerEntityRelation(Form.class, Div.class, RelType.CONTAINS, Direction.INCOMING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Form.class, P.class, RelType.CONTAINS, Direction.INCOMING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Form.class, Content.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Form.class, Div.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Form.class, Input.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Form.class, Button.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Form.class, Select.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Form.class, Label.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Form.class, Textarea.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);

	}

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}
}
