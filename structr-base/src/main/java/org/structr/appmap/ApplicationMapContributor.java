/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.appmap;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.graph.NodeInterface;

import java.util.List;

/**
 * Adds one part of the application to the application map. Each module registers its
 * contributors in StructrModule.onLoad, so a module that is not loaded adds nothing.
 *
 * Contributors run in registration order. Nodes are identified by the UUID of the entity
 * they represent, so a contributor can link to nodes of another contributor (an event
 * action mapping to a schema method) without knowing it. Edges to nodes that take no part
 * in the map are dropped when the map is finished.
 */
public interface ApplicationMapContributor {

	void contribute(final ApplicationMap map, final App app) throws FrameworkException;

	/**
	 * The map elements an object of the application may be shown as, from the object itself outwards: an HTML element
	 * lies in the elements above it, up to its page or shared component; a schema property belongs to its type. A client
	 * takes the first one its map knows, e.g. to show where a search result lives. Empty for objects the contributor does
	 * not put on the map.
	 */
	default List<String> locate(final NodeInterface node) throws FrameworkException {

		return List.of();
	}
}
