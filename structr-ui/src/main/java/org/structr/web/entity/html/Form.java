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


package org.structr.web.entity.html;

import org.structr.web.entity.dom.DOMElement;
import org.apache.commons.lang.ArrayUtils;
import org.neo4j.graphdb.Direction;
import org.structr.core.property.Property;

import org.structr.common.PropertyView;
import org.structr.web.common.RelType;
import org.structr.common.View;
import org.structr.core.property.CollectionProperty;
import org.structr.web.common.HtmlProperty;
import org.structr.web.entity.dom.Content;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Form extends DOMElement {

	public static final Property<String> _acceptCharset = new HtmlProperty("accept-charset");
	public static final Property<String> _action        = new HtmlProperty("action");
	public static final Property<String> _autocomplete  = new HtmlProperty("autocomplete");
	public static final Property<String> _enctype       = new HtmlProperty("enctype");
	public static final Property<String> _method        = new HtmlProperty("method");
	public static final Property<String> _name          = new HtmlProperty("name");
	public static final Property<String> _novalidate    = new HtmlProperty("novalidate");
	public static final Property<String> _target        = new HtmlProperty("target");
	
	public static final CollectionProperty<Div>      divParents = new CollectionProperty<Div>("divParents", Div.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<P>        pParents   = new CollectionProperty<P>("pParents", P.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Content>  contents   = new CollectionProperty<Content>("contents", Content.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Div>      divs       = new CollectionProperty<Div>("divs", Div.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Input>    inputs     = new CollectionProperty<Input>("inputs", Input.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Button>   buttons    = new CollectionProperty<Button>("buttons", Button.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Select>   selects    = new CollectionProperty<Select>("selects", Select.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Label>    labels     = new CollectionProperty<Label>("labels", Label.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Textarea> textareas  = new CollectionProperty<Textarea>("textareas", Textarea.class, RelType.CONTAINS, Direction.OUTGOING, false);

	public static final View htmlView = new View(Form.class, PropertyView.Html,
	    _acceptCharset, _action, _autocomplete, _enctype, _method, _name, _novalidate, _target
	);

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}
}
