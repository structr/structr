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


package org.structr.web.entity;

import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyView;
import org.structr.web.common.RelType;
import org.structr.common.View;
import org.structr.core.EntityContext;
import static org.structr.core.GraphObject.type;
import static org.structr.core.GraphObject.uuid;
import org.structr.core.graph.NodeService.NodeIndex;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.IntProperty;

//~--- classes ----------------------------------------------------------------

/**
 * The Folder entity.
 *
 * @author Axel Morgner
 *
 */
public class Folder extends AbstractFile {

	public static final CollectionProperty<Folder> folders      = new CollectionProperty<Folder>("folders", Folder.class, RelType.CONTAINS, Direction.OUTGOING, new PropertySetNotion(uuid, name), true);
	public static final CollectionProperty<File>   files        = new CollectionProperty<File>("files", File.class, RelType.CONTAINS, Direction.OUTGOING, new PropertySetNotion(uuid, name), true);
	public static final CollectionProperty<Image>  images       = new CollectionProperty<Image>("images", Image.class, RelType.CONTAINS, Direction.OUTGOING, new PropertySetNotion(uuid, name), true);
	
	public static final IntProperty                position     = new IntProperty("position");

	public static final View defaultView = new View(Folder.class, PropertyView.Public, uuid, type, name);
	
	public static final View uiView = new View(Folder.class, PropertyView.Ui,
		parent, folders, files, images
	);

	static {
		EntityContext.registerSearchablePropertySet(Image.class, NodeIndex.keyword.name(), uuid, type, name);
		EntityContext.registerSearchablePropertySet(Image.class, NodeIndex.fulltext.name(), uuid, type, name);
	}
}
