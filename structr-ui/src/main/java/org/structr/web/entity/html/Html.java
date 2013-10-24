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
import org.structr.common.SecurityContext;
import org.structr.web.common.RelType;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.Forward;
import org.structr.core.property.StringProperty;
import org.structr.web.common.HtmlProperty;
import org.structr.web.common.RenderContext;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Html extends DOMElement {

	public static final Property<String> _manifest = new HtmlProperty("manifest");
	
	/** If set, the custom opening tag is rendered instead of just <html> to allow things like IE conditional comments:
	 * 
	 * <!--[if lt IE 7]>      <html class="no-js ie8 ie7 ie6"> <![endif]-->
	 * <!--[if IE 7]>         <html class="no-js ie8 ie7"> <![endif]-->
	 * <!--[if IE 8]>         <html class="no-js ie8"> <![endif]-->
	 * <!--[if gt IE 8]><!--> <html class="no-js ie9"> <!--<![endif]-->
	 *
	*/
	public static final Property<String> _customOpeningTag = new StringProperty("customOpeningTag");
	
	public static final Forward<Head> head = new Forward("head", Head.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Forward<Body> body = new Forward("body", Body.class, RelType.CONTAINS, Direction.OUTGOING, false);

	public static final View htmlView = new View(Html.class, PropertyView.Html,
		_manifest
	);

	public static final View uiView = new View(Html.class, PropertyView.Ui,
		_manifest, _customOpeningTag
	);

	@Override
	public void openingTag(final SecurityContext securityContext, final StringBuilder buffer, final String tag, final RenderContext.EditMode editMode, final RenderContext renderContext, final int depth) throws FrameworkException {
		
		String custTag = getProperty(_customOpeningTag);
		
		if (custTag != null) {
			
			buffer.append(custTag);
			
		} else {
			
			super.openingTag(securityContext, buffer, tag, editMode, renderContext, depth);
			
		}
		
	}
	
	//~--- get methods ----------------------------------------------------

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}
}
