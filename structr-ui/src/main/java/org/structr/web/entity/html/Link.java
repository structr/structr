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

import org.apache.commons.lang3.ArrayUtils;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.EndNode;
import org.structr.core.property.EntityIdProperty;
import org.structr.core.property.Property;
import org.structr.web.common.HtmlProperty;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.Linkable;
import org.structr.web.entity.html.relation.ResourceLink;

//~--- classes ----------------------------------------------------------------

/**
 *
 */
public class Link extends LinkSource {

	public static final Property<String> _href     = new HtmlProperty("href");
	public static final Property<String> _rel      = new HtmlProperty("rel");
	public static final Property<String> _media    = new HtmlProperty("media");
	public static final Property<String> _hreflang = new HtmlProperty("hreflang");
	public static final Property<String> _type     = new HtmlProperty("type");
	public static final Property<String> _sizes    = new HtmlProperty("sizes");
	
//	public static final EndNodes<Head> heads      = new EndNodes<Head>("heads", Head.class, RelType.CONTAINS, Direction.INCOMING, false);
	
	public static final Property<Linkable> linkable   = new EndNode<>("linkable", ResourceLink.class, new PropertyNotion(AbstractNode.name));
	public static final Property<String>   linkableId = new EntityIdProperty("linkableId", linkable);

	public static final View uiView = new View(Link.class, PropertyView.Ui,
		linkableId, linkable
	);
	
	public static final View htmlView = new View(Link.class, PropertyView.Html,
		_href, _rel, _media, _hreflang, _type, _sizes
	);

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}

	@Override
	public boolean isVoidElement() {

		return true;

	}
	
//	@Override
//	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
//		
//		Linkable target = getProperty(linkable);
//		
//		if (target instanceof File) {
//			
//			File file = (File) target;
//			
//			String contentType = file.getProperty(File.contentType);
//			
//			if (contentType != null) {
//				
//				setProperty(_type, contentType);
//				
//				if ("text/css".equals(contentType)) {
//				
//					setProperty(_rel, "stylesheet");
//					setProperty(_media, "screen");
//					
//				}
//				
//			}
//			
//			if (getProperty(_href) == null) {
//				
//				setProperty(_href, "${link.path}");
//				
//			}
//			
//		}
//		
//		return true;
//		
//	}
	
}
