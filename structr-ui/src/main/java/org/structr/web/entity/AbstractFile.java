/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
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
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.CollectionIdProperty;
import org.structr.core.property.EndNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.EntityIdProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.web.entity.relation.FileChildren;
import org.structr.web.entity.relation.FileSiblings;
import org.structr.web.entity.relation.FolderChildren;
import org.structr.web.property.PathProperty;

/**
 * Base class for filesystem objects in structr.
 *
 * @author Christian Morgner
 */
public class AbstractFile extends LinkedTreeNode<FileChildren, FileSiblings, AbstractFile> {

	public static final Property<Folder> parent                = new StartNode<>("parent", FolderChildren.class);
	public static final Property<List<AbstractFile>> children  = new EndNodes<>("children", FileChildren.class);
	public static final Property<AbstractFile> previousSibling = new StartNode<>("previousSibling", FileSiblings.class);
	public static final Property<AbstractFile> nextSibling     = new EndNode<>("nextSibling", FileSiblings.class);
	public static final Property<List<String>> childrenIds     = new CollectionIdProperty("childrenIds", children);
	public static final Property<String> nextSiblingId         = new EntityIdProperty("nextSiblingId", nextSibling);
	public static final Property<String> path                  = new PathProperty("path").indexed().readOnly();
	public static final Property<String> parentId              = new EntityIdProperty("parentId", parent);
	public static final Property<Boolean> hasParent            = new BooleanProperty("hasParent").indexed();

	public static final View defaultView = new View(AbstractFile.class, PropertyView.Public, path);
	public static final View uiView      = new View(AbstractFile.class, PropertyView.Ui, path);

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {
		return (super.isValid(errorBuffer) && !ValidationHelper.checkStringNotBlank(this, name, errorBuffer));
	}

	@Override
	public Class<FileChildren> getChildLinkType() {
		return FileChildren.class;
	}

	@Override
	public Class<FileSiblings> getSiblingLinkType() {
		return FileSiblings.class;
	}
}
