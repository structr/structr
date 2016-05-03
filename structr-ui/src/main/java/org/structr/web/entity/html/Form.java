/**
 * Copyright (C) 2010-2016 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.html;

import org.structr.web.entity.dom.DOMElement;
import org.apache.commons.lang3.ArrayUtils;
import org.structr.core.property.Property;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.web.common.HtmlProperty;

//~--- classes ----------------------------------------------------------------

/**
 *
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
	
//	public static final EndNodes<Div>      divParents = new EndNodes<Div>("divParents", Div.class, RelType.CONTAINS, Direction.INCOMING, false);
//	public static final EndNodes<P>        pParents   = new EndNodes<P>("pParents", P.class, RelType.CONTAINS, Direction.INCOMING, false);
//	public static final EndNodes<Content>  contents   = new EndNodes<Content>("contents", Content.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Div>      divs       = new EndNodes<Div>("divs", Div.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Input>    inputs     = new EndNodes<Input>("inputs", Input.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Button>   buttons    = new EndNodes<Button>("buttons", Button.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Select>   selects    = new EndNodes<Select>("selects", Select.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Label>    labels     = new EndNodes<Label>("labels", Label.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Textarea> textareas  = new EndNodes<Textarea>("textareas", Textarea.class, RelType.CONTAINS, Direction.OUTGOING, false);

	public static final View htmlView = new View(Form.class, PropertyView.Html,
	    _acceptCharset, _action, _autocomplete, _enctype, _method, _name, _novalidate, _target
	);

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}
}
