/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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



package org.structr.core.entity;

import java.util.List;
import org.neo4j.graphdb.Direction;

import org.structr.common.property.Property;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.View;
import org.structr.core.EntityContext;
import org.structr.core.entity.RelationClass.Cardinality;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class Folder extends AbstractNode {

	public static final Property<Folder>       parentFolder = new Property<Folder>("parentFolder");
	public static final Property<List<Folder>> folders      = new Property<List<Folder>>("folders");
	public static final Property<List<File>>   files        = new Property<List<File>>("files");
	public static final Property<List<Image>>  images       = new Property<List<Image>>("images");

	public static final View uiView = new View(Folder.class, PropertyView.Ui,
		parentFolder, folders, files, images
	);
	
	static {

//		EntityContext.registerPropertySet(Folder.class, PropertyView.All, Key.values());
//		EntityContext.registerPropertySet(Folder.class, PropertyView.Ui, Key.values());
		
		EntityContext.registerPropertyRelation(Folder.class, parentFolder, Folder.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToOne);

		EntityContext.registerEntityRelation(Folder.class, Folder.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.OneToMany);
		EntityContext.registerEntityRelation(Folder.class, File.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.OneToMany);
		EntityContext.registerEntityRelation(Folder.class, Image.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.OneToMany);
		

	}

	//~--- constant enums -------------------------------------------------
}
