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

import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.RelationClass;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Input extends HtmlElement {

	private static final String[] htmlAttributes = new String[] {

		"accept", "alt", "autocomplete", "autofocus", "checked", "dirname", "disabled", "form", "formaction", "formenctype", "formmethod", "formnovalidate", "formtarget", "height", "list",
		"max", "maxlength", "min", "multiple", "name", "pattern", "placeholder", "readonly", "required", "size", "src", "step", "type", "value", "width"

	};

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(Input.class, PropertyView.All, HtmlElement.UiKey.values());
		EntityContext.registerPropertySet(Input.class, PropertyView.Public, HtmlElement.UiKey.values());
		EntityContext.registerPropertySet(Input.class, PropertyView.Html, PropertyView.Html, htmlAttributes);
		EntityContext.registerEntityRelation(Input.class, Form.class, RelType.CONTAINS, Direction.INCOMING, RelationClass.Cardinality.ManyToMany);

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public boolean isVoidElement() {

		return true;

	}

}
