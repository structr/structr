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
package org.structr.web.entity;

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.html.relation.ResourceLink;

//~--- interfaces -------------------------------------------------------------

/**
 *
 *
 */
public interface Linkable extends NodeInterface {

	public static final Property<List<LinkSource>> linkingElements = new StartNodes<>("linkingElements", ResourceLink.class, new PropertyNotion(GraphObject.id));
	public static final Property<Boolean> enableBasicAuth          = new BooleanProperty("enableBasicAuth").defaultValue(false).indexed();
	public static final Property<String> basicAuthRealm            = new StringProperty("basicAuthRealm");

	public static final org.structr.common.View uiView = new org.structr.common.View(Linkable.class, PropertyView.Ui, linkingElements, enableBasicAuth, basicAuthRealm);

	public String getPath();
}
