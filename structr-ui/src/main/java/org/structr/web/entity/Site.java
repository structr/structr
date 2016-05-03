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
import org.structr.common.View;
import static org.structr.core.GraphObject.id;
import org.structr.core.entity.AbstractNode;
import static org.structr.core.graph.NodeInterface.name;
import org.structr.core.property.EndNodes;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.relation.Pages;
import org.structr.web.property.UiNotion;

/**
 * This class represents a web site, defined by one ore more hostnames and ports
 *
 *
 */


public class Site extends AbstractNode {

//	public static final Property<List<String>>  hostnames = new ArrayProperty("hostnames", String.class).indexedWhenEmpty();
//	public static final Property<List<Integer>> ports     = new ArrayProperty("ports", Integer.class).indexedWhenEmpty();
	public static final Property<String>  hostname = new StringProperty("hostname").cmis().indexedWhenEmpty();
	public static final Property<Integer> port     = new IntProperty("port").cmis().indexedWhenEmpty();

	public static final Property<List<Page>>    pages     = new EndNodes<>("pages", Pages.class, new UiNotion());

	public static final View defaultView = new View(Site.class, PropertyView.Public, id, type, name, hostname, port, pages);

	public static final View uiView = new View(Site.class, PropertyView.Ui,type, name, hostname, port, pages);

}
