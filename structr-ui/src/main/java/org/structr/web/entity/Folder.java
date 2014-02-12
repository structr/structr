/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity;

import java.util.List;
import org.structr.common.View;
import org.structr.common.PropertyView;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.EndNodes;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.web.entity.relation.Files;
import org.structr.web.entity.relation.Folders;
import org.structr.web.entity.relation.Images;


//~--- classes ----------------------------------------------------------------

/**
 * The Folder entity.
 *
 * @author Axel Morgner
 *
 */
public class Folder extends AbstractFile {

	public static final Property<List<Folder>> folders = new EndNodes<>("folders", Folders.class, new PropertySetNotion(id, name));
	public static final Property<List<File>>   files   = new EndNodes<>("files", Files.class, new PropertySetNotion(id, name));
	public static final Property<List<Image>>  images  = new EndNodes<>("images", Images.class, new PropertySetNotion(id, name));
	
	public static final Property<Integer>		position     = new IntProperty("position").indexed();

	public static final View defaultView = new View(Folder.class, PropertyView.Public, id, type, name);
	
	public static final View uiView = new View(Folder.class, PropertyView.Ui,
		parent, folders, files, images
	);
	
	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {
		
		boolean valid = true;
		
		valid &= nonEmpty(AbstractNode.name, errorBuffer);
		valid &= super.isValid(errorBuffer);
		
		return valid;
	}
}
