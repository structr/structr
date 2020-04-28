/**
 * Copyright (C) 2010-2020 Structr GmbH
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


import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.structr.api.config.Settings;
import org.structr.api.graph.Cardinality;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UniqueToken;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.PropertyKey;
import org.structr.schema.SchemaService;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonReferenceType;
import org.structr.api.schema.JsonSchema;
import org.structr.web.common.FileHelper;
import org.structr.web.property.MethodProperty;
import org.structr.web.property.PathProperty;

/**
 * Base class for filesystem objects in structr.
 */
public interface AbstractFile extends LinkedTreeNode<AbstractFile> {

	static class Impl { static {

		final JsonSchema schema     = SchemaService.getDynamicSchema();
		final JsonObjectType folder = (JsonObjectType)schema.addType("Folder");
		final JsonObjectType type   = schema.addType("AbstractFile");

		type.setIsAbstract();
		type.setImplements(URI.create("https://structr.org/v1.1/definitions/AbstractFile"));
		type.setExtends(URI.create("https://structr.org/v1.1/definitions/LinkedTreeNodeImpl?typeParameters=org.structr.web.entity.AbstractFile"));
		type.setCategory("ui");

		type.addStringProperty("name", PropertyView.Public).setIndexed(true).setRequired(true).setFormat("[^\\\\/\\\\x00]+");

		type.addCustomProperty("isMounted", MethodProperty.class.getName(), PropertyView.Public, PropertyView.Ui).setTypeHint("Boolean").setFormat(AbstractFile.class.getName() + ", isMounted");
		type.addBooleanProperty("includeInFrontendExport",                  PropertyView.Ui).setIndexed(true);
		type.addBooleanProperty("isExternal",                               PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addLongProperty("lastSeenMounted",                             PropertyView.Public, PropertyView.Ui);

		type.addBooleanProperty("hasParent").setIndexed(true);

		type.addCustomProperty("path", PathProperty.class.getName(), PropertyView.Public, PropertyView.Ui).setTypeHint("String").setIndexed(true);

		type.addPropertyGetter("hasParent", Boolean.TYPE);
		type.addPropertyGetter("parent", Folder.class);
		type.addPropertyGetter("path", String.class);

		type.addPropertySetter("hasParent", Boolean.TYPE);

		type.overrideMethod("getPositionProperty",         false, "return FolderCONTAINSAbstractFile.positionProperty;");

		type.overrideMethod("onCreation",                  true,  AbstractFile.class.getName() + ".onCreation(this, arg0, arg1);");
		type.overrideMethod("onModification",              true,  AbstractFile.class.getName() + ".onModification(this, arg0, arg1, arg2);");
		type.overrideMethod("getSiblingLinkType",          false, "return AbstractFileCONTAINS_NEXT_SIBLINGAbstractFile.class;");
		type.overrideMethod("getChildLinkType",            false, "return FolderCONTAINSAbstractFile.class;");
		type.overrideMethod("isExternal",                  false, "return getProperty(isExternalProperty);");
		type.overrideMethod("isBinaryDataAccessible",      false, "return !isExternal() || isMounted();")
//			.addParameter("ctx", SecurityContext.class.getName())
			.setDoExport(true);
		type.overrideMethod("isMounted",                   false, "return " + AbstractFile.class.getName() + ".isMounted(this);");
		type.overrideMethod("getFolderPath",               false, "return " + AbstractFile.class.getName() + ".getFolderPath(this);");
		type.overrideMethod("includeInFrontendExport",     false, "return " + AbstractFile.class.getName() + ".includeInFrontendExport(this);");

		type.addMethod("setParent")
			.setSource("setProperty(parentProperty, (Folder)parent);")
			.addException(FrameworkException.class.getName())
			.addParameter("parent", "org.structr.web.entity.Folder");

		final JsonReferenceType parentRel  = folder.relate(type, "CONTAINS", Cardinality.OneToMany, "parent", "children");
		final JsonReferenceType siblingRel = type.relate(type, "CONTAINS_NEXT_SIBLING", Cardinality.OneToOne,  "previousSibling", "nextSibling");

		type.addIdReferenceProperty("parentId",      parentRel.getSourceProperty());
		type.addIdReferenceProperty("nextSiblingId", siblingRel.getTargetProperty());

		// sort position of children in page
		parentRel.addIntegerProperty("position");

		// view configuration
		type.addViewProperty(PropertyView.Public, "visibleToAuthenticatedUsers");
		type.addViewProperty(PropertyView.Public, "visibleToPublicUsers");

		type.addViewProperty(PropertyView.Ui, "parent");
	}}

	void setParent(final Folder parent) throws FrameworkException;
	void setHasParent(final boolean hasParent) throws FrameworkException;
	Folder getParent();

	String getPath();
	String getFolderPath();

	boolean isMounted();
	boolean isExternal();
	boolean getHasParent();
	boolean isBinaryDataAccessible(final SecurityContext securityContext);
	boolean includeInFrontendExport();

	static void onCreation(final AbstractFile thisFile, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		thisFile.setHasParent(thisFile.getParent() != null);

		if (org.structr.api.config.Settings.UniquePaths.getValue()) {
			AbstractFile.validateAndRenameFileOnce(thisFile, securityContext, errorBuffer);
		}
	}

	static void onModification(final AbstractFile thisFile, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		thisFile.setHasParent(thisFile.getParent() != null);

		if (org.structr.api.config.Settings.UniquePaths.getValue()) {
			AbstractFile.validateAndRenameFileOnce(thisFile, securityContext, errorBuffer);
		}
	}

	static boolean validatePath(final AbstractFile thisFile, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		final PropertyKey<String> pathKey = StructrApp.key(AbstractFile.class, "path");
		final String filePath             = thisFile.getProperty(pathKey);

		if (filePath != null) {

			final List<AbstractFile> files = StructrApp.getInstance().nodeQuery(AbstractFile.class).and(pathKey, filePath).getAsList();
			for (final AbstractFile file : files) {

				if (!file.getUuid().equals(thisFile.getUuid())) {

					if (errorBuffer != null) {

						final UniqueToken token = new UniqueToken(AbstractFile.class.getSimpleName(), pathKey, file.getUuid());
						token.setValue(filePath);

						errorBuffer.add(token);
					}

					return false;
				}
			}
		}

		return true;
	}

	static boolean validateAndRenameFileOnce(final AbstractFile thisFile, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		final PropertyKey<String> pathKey = StructrApp.key(AbstractFile.class, "path");
		boolean valid                     = AbstractFile.validatePath(thisFile, securityContext, null);

		if (!valid) {

			final String originalPath = thisFile.getProperty(pathKey);
			final String newName      = thisFile.getProperty(AbstractFile.name).concat("_").concat(FileHelper.getDateString());

			thisFile.setProperty(AbstractNode.name, newName);

			valid = AbstractFile.validatePath(thisFile, securityContext, errorBuffer);

			if (valid) {
				logger.warn("File {} already exists, renaming to {}", new Object[] { originalPath, newName });
			} else {
				logger.warn("File {} already existed. Tried renaming to {} and failed. Aborting.", new Object[] { originalPath, newName });
			}

		}

		return valid;

	}

	static String getFolderPath(final AbstractFile thisFile) {

		Folder parentFolder = thisFile.getParent();
		String folderPath   = thisFile.getProperty(AbstractFile.name);

		if (folderPath == null) {
			folderPath = thisFile.getUuid();
		}

		while (parentFolder != null) {

			folderPath   = parentFolder.getName().concat("/").concat(folderPath);
			parentFolder = parentFolder.getParent();
		}

		return "/".concat(folderPath);
	}

	static boolean includeInFrontendExport(final AbstractFile thisFile) {

		if (thisFile.getProperty(StructrApp.key(File.class, "includeInFrontendExport"))) {

			return true;
		}

		final Folder _parent = thisFile.getParent();
		if (_parent != null) {

			// recurse
			return _parent.includeInFrontendExport();
		}

		return false;
	}

	static boolean isMounted(final AbstractFile thisFile) {

		if (thisFile.getProperty(StructrApp.key(Folder.class, "mountTarget")) != null) {
			return true;
		}

		final Folder parent = thisFile.getParent();
		if (parent != null) {

			return parent.isMounted();
		}

		return false;
	}

	// ----- protected methods -----
	static java.io.File defaultGetFileOnDisk(final File fileBase, final boolean create) {

		final String uuid       = fileBase.getUuid();
		final String filePath   = Settings.FilesPath.getValue();
		final String uuidPath   = AbstractFile.getDirectoryPath(uuid);
		final String finalPath  = filePath + "/" + uuidPath + "/" + uuid;
		final Path path         = Paths.get(URI.create("file://" + finalPath));
		final java.io.File file = path.toFile();

		// create parent directory tree
		file.getParentFile().mkdirs();

		// create file only if requested
		if (!file.exists() && create && !fileBase.isExternal()) {

			try {

				file.createNewFile();

			} catch (IOException ioex) {

				logger.error("Unable to create file {}: {}", file, ioex.getMessage());
			}
		}

		return file;
	}

	static String getDirectoryPath(final String uuid) {

		return (uuid != null)
			? uuid.substring(0, 1) + "/" + uuid.substring(1, 2) + "/" + uuid.substring(2, 3) + "/" + uuid.substring(3, 4)
			: null;

	}
	// ----- nested classes -----
}
