/**
 * Copyright (C) 2010-2018 Structr GmbH
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
import java.util.List;
import org.structr.api.config.Settings;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UniqueToken;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.core.property.PropertyKey;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonReferenceType;
import org.structr.schema.json.JsonSchema;
import org.structr.web.common.FileHelper;
import org.structr.web.property.PathProperty;

/**
 * Base class for filesystem objects in Structr.
 */
public interface AbstractFile extends LinkedTreeNode<AbstractFile> {

	static class Impl { static {

		final JsonSchema schema     = SchemaService.getDynamicSchema();
		final JsonObjectType folder = (JsonObjectType)schema.addType("Folder");
		final JsonObjectType type   = schema.addType("AbstractFile");

		type.setIsAbstract();
		type.setImplements(URI.create("https://structr.org/v1.1/definitions/AbstractFile"));
		type.setExtends(URI.create("https://structr.org/v1.1/definitions/LinkedTreeNodeImpl?typeParameters=org.structr.web.entity.AbstractFile"));

		type.addStringProperty("name", PropertyView.Public).setRequired(true).setFormat("[^\\\\/\\\\x00]+");

		type.addBooleanProperty("includeInFrontendExport", PropertyView.Public).setIndexed(true);
		type.addBooleanProperty("isExternal", PropertyView.Public).setIndexed(true);
		type.addBooleanProperty("hasParent", PropertyView.Public).setIndexed(true);
		type.addCustomProperty("path", PathProperty.class.getName(), PropertyView.Public).setIndexed(true);

		type.addPropertyGetter("hasParent", Boolean.TYPE);
		type.addPropertyGetter("parent", Folder.class);
		type.addPropertyGetter("path", String.class);

		type.overrideMethod("getPositionProperty",         false, "return FolderCONTAINSAbstractFile.positionProperty;");

		type.overrideMethod("onCreation",                  true, "if (org.structr.api.config.Settings.UniquePaths.getValue()) { validateAndRenameFileOnce(arg0, arg1); }");
		type.overrideMethod("onModification",              true, "if (org.structr.api.config.Settings.UniquePaths.getValue()) { validatePath(arg0, arg1); }");
		type.overrideMethod("getSiblingLinkType",          false, "return AbstractFileCONTAINS_NEXT_SIBLINGAbstractFile.class;");
		type.overrideMethod("getChildLinkType",            false, "return FolderCONTAINSAbstractFile.class;");
		type.overrideMethod("isExternal",                  false, "return getProperty(isExternalProperty);");
		type.overrideMethod("validatePath",                false, "return " + AbstractFile.class.getName() + ".validatePath(this, arg0, arg1);");
		type.overrideMethod("validateAndRenameFileOnce",   false, "return " + AbstractFile.class.getName() + ".validateAndRenameFileOnce(this, arg0, arg1);");
		type.overrideMethod("includeInFrontendExport",     false, "return " + AbstractFile.class.getName() + ".includeInFrontendExport(this);");

		type.addMethod("setParent")
			.setSource("setProperty(parentProperty, (Folder)parent);")
			.addException(FrameworkException.class.getName())
			.addParameter("parent", "org.structr.web.entity.Folder");

		final JsonReferenceType parentRel  = folder.relate(type, "CONTAINS", Relation.Cardinality.OneToMany, "parent", "children");
		final JsonReferenceType siblingRel = type.relate(type, "CONTAINS_NEXT_SIBLING", Cardinality.OneToOne,  "previousSibling", "nextSibling");

		type.addIdReferenceProperty("parentId",      parentRel.getSourceProperty());
		type.addIdReferenceProperty("nextSiblingId", siblingRel.getTargetProperty());

		// sort position of children in page
		parentRel.addIntegerProperty("position");
	}}

	String getPath();

	Folder getParent();
	boolean getHasParent();
	void setParent(final Folder folder) throws FrameworkException;

	boolean isExternal();
	boolean isMounted();
	boolean includeInFrontendExport();
	boolean validatePath(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException;
	boolean validateAndRenameFileOnce(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException;

	public static String getFolderPath(final AbstractFile thisFile) {

		Folder parentFolder = thisFile.getParent();
		String folderPath   = thisFile.getName();

		if (folderPath == null) {
			folderPath = thisFile.getUuid();
		}

		while (parentFolder != null) {

			folderPath   = parentFolder.getName().concat("/").concat(folderPath);
			parentFolder = parentFolder.getParent();
		}

		return "/".concat(folderPath);
	}

	public static String getDirectoryPath(final String uuid) {

		return (uuid != null)
			? uuid.substring(0, 1) + "/" + uuid.substring(1, 2) + "/" + uuid.substring(2, 3) + "/" + uuid.substring(3, 4)
			: null;
	}

	public static java.io.File defaultGetFileOnDisk(final AbstractFile fileBase, final boolean create) {

		final String uuid       = fileBase.getUuid();
		final String filePath   = Settings.FilesPath.getValue();
		final String uuidPath   = AbstractFile.getDirectoryPath(uuid);
		final java.io.File file = new java.io.File(filePath + "/" + uuidPath + "/" + uuid);

		// create parent directory tree
		file.getParentFile().mkdirs();

		// create file only if requested
		if (create && !file.exists() && !fileBase.isExternal()) {

			try {

				file.createNewFile();

			} catch (IOException ioex) {

				logger.error("Unable to create file {}: {}", file, ioex.getMessage());
			}
		}

		return file;
	}

	/*
	//public static final Property<Folder> parent                    = new StartNode<>("parent", FolderChildren.class);
	//public static final Property<List<AbstractFile>> children      = new EndNodes<>("children", FileChildren.class);
	//public static final Property<AbstractFile> previousSibling     = new StartNode<>("previousSibling", FileSiblings.class);
	//public static final Property<AbstractFile> nextSibling         = new EndNode<>("nextSibling", FileSiblings.class);
	//public static final Property<List<String>> childrenIds         = new CollectionIdProperty("childrenIds", children);
	//public static final Property<String> nextSiblingId             = new EntityIdProperty("nextSiblingId", nextSibling);
	//public static final Property<String> path                      = new PathProperty("path").indexed().readOnly();
	//public static final Property<String> parentId                  = new EntityIdProperty("parentId", parent);
	//public static final Property<Boolean> hasParent                = new BooleanProperty("hasParent").indexed();
	//public static final Property<Boolean>  includeInFrontendExport = new BooleanProperty("includeInFrontendExport").cmis().indexed();

	public static final View defaultView = new View(AbstractFile.class, PropertyView.Public,
		path, isExternal, isMounted, lastSeenMounted
	);

	public static final View uiView = new View(AbstractFile.class, PropertyView.Ui,
		path, isExternal, isMounted, lastSeenMounted
	);

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		boolean valid = true;

		if (Settings.UniquePaths.getValue()) {
			valid = validateAndRenameFileOnce(securityContext, errorBuffer);
		}

		return valid && super.onCreation(securityContext, errorBuffer);
	}

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		boolean valid = true;

		if (Settings.UniquePaths.getValue()) {
			valid = validateAndRenameFileOnce(securityContext, errorBuffer);
		}

		return valid && super.onModification(securityContext, errorBuffer, modificationQueue);
	}
	*/

	public static boolean validatePath(final AbstractFile thisFile, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		final PropertyKey<String> pathKey = StructrApp.key(File.class, "path");
		final String filePath             = thisFile.getPath();

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

	public static boolean validateAndRenameFileOnce(final AbstractFile thisFile, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		boolean valid = thisFile.validatePath(securityContext, null);

		if (!valid) {

			final String originalPath = thisFile.getPath();
			final String newName      = thisFile.getProperty(AbstractFile.name).concat("_").concat(FileHelper.getDateString());

			thisFile.setProperty(AbstractFile.name, newName);

			valid = thisFile.validatePath(securityContext, errorBuffer);

			if (valid) {
				logger.warn("File {} already exists, renaming to {}", new Object[] { originalPath, newName });
			} else {
				logger.warn("File {} already existed. Tried renaming to {} and failed. Aborting.", new Object[] { originalPath, newName });
			}

		}

		return valid;
	}

	/*

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
	*/

	public static boolean includeInFrontendExport(final AbstractFile thisFile) {

		if (thisFile.getProperty(StructrApp.key(AbstractFile.class, "includeInFrontendExport"))) {

			return true;
		}

		final Folder _parent = thisFile.getParent();
		if (_parent != null) {

			// recurse
			return _parent.includeInFrontendExport();
		}

		return false;
	}
}
