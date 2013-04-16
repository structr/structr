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

import org.structr.web.common.RelType;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.EntityProperty;

/**
 * Abstract base class for filesystem objects in structr.
 *
 * @author Christian Morgner
 */
public class AbstractFile extends LinkedTreeNode {

	public static final EntityProperty<AbstractFile> previousSibling = new EntityProperty("previousSibling", AbstractFile.class, RelType.CONTAINS_NEXT_SIBLING, Direction.INCOMING, false);
	public static final EntityProperty<AbstractFile> nextSibling     = new EntityProperty("nextSibling", AbstractFile.class, RelType.CONTAINS_NEXT_SIBLING, Direction.OUTGOING, false);
	public static final CollectionProperty<AbstractFile> children    = new CollectionProperty("children", AbstractFile.class, RelType.CONTAINS, Direction.OUTGOING, true);
	public static final EntityProperty<Folder> parent                = new EntityProperty<Folder>("parent", Folder.class, RelType.CONTAINS, Direction.INCOMING, true);

}
