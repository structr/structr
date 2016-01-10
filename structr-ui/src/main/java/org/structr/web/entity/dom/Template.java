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
package org.structr.web.entity.dom;

import org.structr.common.PropertyView;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

/**
 * Content element that can act as an outer template.
 *
 *
 */


public class Template extends Content {

	public static final Property<String> configuration                                   = new StringProperty("configuration").indexed();

	public static final org.structr.common.View uiView                                   = new org.structr.common.View(Content.class, PropertyView.Ui,
		children, childrenIds, content, contentType, parent, pageId, hideOnDetail, hideOnIndex, sharedComponent, syncedNodes, dataKey, restQuery, cypherQuery, xpathQuery, functionQuery,
		showForLocales, hideForLocales, showConditions, hideConditions, isContent, configuration
	);

	public static final org.structr.common.View publicView                               = new org.structr.common.View(Content.class, PropertyView.Public,
		children, childrenIds, content, contentType, parent, pageId, hideOnDetail, hideOnIndex, sharedComponent, syncedNodes, dataKey, restQuery, cypherQuery, xpathQuery, functionQuery,
		showForLocales, hideForLocales, showConditions, hideConditions, isContent, configuration
	);

}
