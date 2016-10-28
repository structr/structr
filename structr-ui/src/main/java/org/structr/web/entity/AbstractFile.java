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
import org.structr.common.SecurityContext;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UniqueToken;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.graph.ModificationQueue;
import static org.structr.core.graph.NodeInterface.name;
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
 *
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

	private static boolean validatePathUniqueness = false;

	static {

		try { validatePathUniqueness = Boolean.valueOf(StructrApp.getConfigurationValue(Services.APPLICATION_FILESYSTEM_UNIQUE_PATHS, "true")); } catch (Throwable t) {}
	}

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		final boolean valid = validatePath(securityContext, errorBuffer);
		return valid && super.onCreation(securityContext, errorBuffer);
	}

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		final boolean valid = validatePath(securityContext, errorBuffer);
		return valid && super.onModification(securityContext, errorBuffer, modificationQueue);
	}

	public boolean validatePath(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (validatePathUniqueness) {

			final String filePath = getProperty(path);
			if (filePath != null) {

				final List<AbstractFile> files = StructrApp.getInstance().nodeQuery(AbstractFile.class).and(path, filePath).getAsList();
				for (final AbstractFile file : files) {

					if (!file.getUuid().equals(getUuid())) {

						if (errorBuffer != null) {

							final UniqueToken token = new UniqueToken(AbstractFile.class.getSimpleName(), path, file.getUuid());
							token.setValue(filePath);

							errorBuffer.add(token);
						}

						return false;
					}
				}
			}
		}

		return true;
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= nonEmpty(AbstractFile.name, errorBuffer);
		valid &= ValidationHelper.isValidStringMatchingRegex(this, name, "[^\\/\\x00]+", errorBuffer);

		return valid;
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
