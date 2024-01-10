/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.web.entity;

import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.util.Iterables;
import org.structr.common.ConstantBooleanTrue;
import org.structr.common.ContextAwareEntity;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.PropertyMap;
import org.structr.files.external.DirectoryWatchService;
import org.structr.schema.SchemaService;

import java.net.URI;


public interface Folder extends AbstractFile, ContextAwareEntity {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Folder");
		final JsonObjectType img  = schema.addType("Image");
		final JsonObjectType file = schema.addType("File");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Folder"));
		type.setExtends(URI.create("#/definitions/AbstractFile"));
		type.setCategory("ui");

		type.addBooleanProperty("isFolder", PropertyView.Public, PropertyView.Ui).setReadOnly(true).addTransformer(ConstantBooleanTrue.class.getName());
		type.addStringProperty("mountTarget", PropertyView.Public).setIndexed(true);
		type.addIntegerProperty("position").setIndexed(true);

		type.addStringProperty("enabledChecksums",         PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("mountTarget",              PropertyView.Ui);
		type.addBooleanProperty("mountDoFulltextIndexing", PropertyView.Public, PropertyView.Ui);
		type.addBooleanProperty("mountWatchContents",      PropertyView.Public, PropertyView.Ui);
		type.addIntegerProperty("mountScanInterval",       PropertyView.Public, PropertyView.Ui);
		type.addLongProperty("mountLastScanned",           PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("mountTargetFileType",      PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("mountTargetFolderType",    PropertyView.Public, PropertyView.Ui);

		type.addFunctionProperty("filesCount", "public").setReadFunction("size(this.files)").setTypeHint("int");
		type.addFunctionProperty("foldersCount", "public").setReadFunction("size(this.folders)").setTypeHint("int");

		type.addPropertyGetter("mountTarget", String.class);
		type.addPropertyGetter("mountTargetFileType", String.class);
		type.addPropertyGetter("mountTargetFolderType", String.class);
		type.addPropertyGetter("enabledChecksums", String.class);

		type.addPropertyGetter("children", Iterable.class);

		type.overrideMethod("onCreation",     true, Folder.class.getName() + ".onCreation(this, arg0, arg1);");
		type.overrideMethod("onModification", true, Folder.class.getName() + ".onModification(this, arg0, arg1, arg2);");
		type.overrideMethod("onDeletion",     true, Folder.class.getName() + ".onDeletion(this, arg0, arg1, arg2);");

		type.overrideMethod("getFiles",       false, "return " + Folder.class.getName() + ".getFiles(this);");
		type.overrideMethod("getFolders",     false, "return " + Folder.class.getName() + ".getFolders(this);");
		type.overrideMethod("getImages",      false, "return " + Folder.class.getName() + ".getImages(this);");
		type.overrideMethod("isMounted",      false, "return " + Folder.class.getName() + ".isMounted(this);");

		// ContextAwareEntity
		type.overrideMethod("getEntityContextPath",  false, "return getPath();");

		type.overrideMethod("getPath",             false, "return getProperty(pathProperty);");

		type.relate(type, "CONTAINS", Cardinality.OneToMany, "folderParent", "folders");
		type.relate(file, "CONTAINS", Cardinality.OneToMany, "fileParent",   "files");
		type.relate(img,  "CONTAINS", Cardinality.OneToMany, "imageParent",  "images");

		// view configuration
		type.addViewProperty(PropertyView.Public, "parentId");
		type.addViewProperty(PropertyView.Public, "owner");
		type.addViewProperty(PropertyView.Public, "folders");
		type.addViewProperty(PropertyView.Public, "files");

		type.addViewProperty(PropertyView.Ui, "folders");
		type.addViewProperty(PropertyView.Ui, "files");
		type.addViewProperty(PropertyView.Ui, "images");

	}}

	String getMountTarget();
	String getMountTargetFileType();
	String getMountTargetFolderType();
	String getEnabledChecksums();

	Iterable<AbstractFile> getChildren();
	Iterable<Folder> getFolders();
	Iterable<Image> getImages();
	Iterable<File> getFiles();

	static void onCreation(final Folder thisFolder, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		Folder.setHasParent(thisFolder);

		// only update watch service for root folder of mounted hierarchy
		if (thisFolder.getProperty(StructrApp.key(Folder.class, "mountTarget")) != null || !thisFolder.isMounted()) {

			Folder.updateWatchService(thisFolder, true);
		}
	}

	static void onModification(final Folder thisFolder, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		Folder.setHasParent(thisFolder);

		// only update watch service for root folder of mounted hierarchy
		if (thisFolder.getProperty(StructrApp.key(Folder.class, "mountTarget")) != null || !thisFolder.isMounted()) {

			Folder.updateWatchService(thisFolder, true);
		}
	}

	static void onDeletion(final Folder thisFolder, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

		Folder.updateWatchService(thisFolder, false);
	}


	static Iterable<File> getFiles(final Folder thisFolder) {
		return Iterables.map(s -> (File)s, Iterables.filter((AbstractFile value) -> value instanceof File, thisFolder.getChildren()));
	}

	static Iterable<Folder> getFolders(final Folder thisFolder) {
		return Iterables.map(s -> (Folder)s, Iterables.filter((AbstractFile value) -> value instanceof Folder, thisFolder.getChildren()));
	}

	static Iterable<Image> getImages(final Folder thisFolder) {
		return Iterables.map(s -> (Image)s, Iterables.filter((AbstractFile value) -> value instanceof Image, thisFolder.getChildren()));
	}

	static String removeDuplicateSlashes(final String src) {
		return src.replaceAll("[/]+", "/");
	}

	public static boolean isMounted(final Folder thisFolder) {

		return AbstractFile.isMounted(thisFolder);
	}

	static void setHasParent(final Folder thisFolder) throws FrameworkException {

		synchronized (thisFolder) {

			final SecurityContext ctx = thisFolder.getSecurityContext();

			thisFolder.setSecurityContext(SecurityContext.getSuperUserInstance());
			thisFolder.setProperty(StructrApp.key(AbstractFile.class, "hasParent"), thisFolder.getParent() != null);
			thisFolder.setSecurityContext(ctx);

		}
	}

	static void updateWatchService(final Folder thisFolder, final boolean mount) {

		if (Services.getInstance().isConfigured(DirectoryWatchService.class)) {

			final DirectoryWatchService service = StructrApp.getInstance().getService(DirectoryWatchService.class);
			if (service != null && service.isRunning()) {

				if (mount) {

					service.mountFolder(thisFolder);

				} else {

					service.unmountFolder(thisFolder);
				}
			}
		}
	}
}
