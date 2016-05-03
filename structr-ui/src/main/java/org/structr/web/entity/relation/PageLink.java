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
package org.structr.web.entity.relation;

import org.structr.core.property.Property;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.ManyToOne;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

//~--- classes ----------------------------------------------------------------

/**
 *
 *
 */
public class PageLink extends ManyToOne<DOMNode, Page> {

	public static final Property<String> linkType     = new StringProperty("linkType");

	public static final View uiView = new View(PageLink.class, PropertyView.Ui,
		 linkType
	);

	@Override
	public Class<DOMNode> getSourceType() {
		return DOMNode.class;
	}

	@Override
	public Class<Page> getTargetType() {
		return Page.class;
	}

	@Override
	public String name() {
		return "PAGE";
	}

	@Override
	public boolean isInternal() {
		return true;
	}
}
