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

package org.structr.web.entity.html;

import org.structr.common.PropertyView;
import org.structr.core.EntityContext;

/**
 * @author Axel Morgner
 */
public class Cite extends HtmlElement {

	static {
		EntityContext.registerPropertySet(Cite.class, PropertyView.All,		HtmlElement.UiKey.values());
		EntityContext.registerPropertySet(Cite.class, PropertyView.Public,	HtmlElement.UiKey.values());
		EntityContext.registerPropertySet(Cite.class, PropertyView.Html, PropertyView.Html, htmlAttributes);
	}
}
