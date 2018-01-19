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

import java.net.URI;
import java.util.List;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.structr.cmis.CMISInfo;
import org.structr.cmis.info.CMISFolderInfo;
import org.structr.common.ConstantBooleanTrue;
import org.structr.common.PropertyView;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.files.cmis.config.StructrFolderActions;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;


public interface Folder extends AbstractFile, CMISInfo, CMISFolderInfo {


	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Folder");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Folder"));
		type.setExtends(URI.create("#/definitions/AbstractFile"));

		type.addBooleanProperty("isFolder").setReadOnly(true).addTransformer(ConstantBooleanTrue.class.getName());
		type.addIntegerProperty("position", PropertyView.Public).setIndexed(true);
		type.addStringProperty("mountTarget", PropertyView.Public).setIndexed(true);

		type.addStringProperty("enabledChecksums");

		type.addStringProperty("mountTarget");
		type.addBooleanProperty("mountDoFulltextIndexing");
		type.addBooleanProperty("mountWatchContents");
		type.addIntegerProperty("mountScanInterval");
		type.addLongProperty("mountLastScanned");

		type.addPropertyGetter("mountTarget", String.class);
		type.addPropertyGetter("enabledChecksums", String.class);

		type.addPropertyGetter("children", List.class);

		type.overrideMethod("onCreation",     true, Folder.class.getName() + ".setHasParent(this);");
		type.overrideMethod("onModification", true, Folder.class.getName() + ".setHasParent(this);");

		type.overrideMethod("getFiles",       false, "return " + Folder.class.getName() + ".getFiles(this);");
		type.overrideMethod("getFolders",     false, "return " + Folder.class.getName() + ".getFolders(this);");
		type.overrideMethod("getImages",      false, "return " + Folder.class.getName() + ".getImages(this);");
		type.overrideMethod("isMounted",      false, "return " + Folder.class.getName() + ".isMounted(this);");

		// ----- CMIS support -----
		type.overrideMethod("getCMISInfo",         false, "return this;");
		type.overrideMethod("getBaseTypeId",       false, "return " + BaseTypeId.class.getName() + ".CMIS_FOLDER;");
		type.overrideMethod("getFolderInfo",       false, "return this;");
		type.overrideMethod("getDocumentInfo",     false, "return null;");
		type.overrideMethod("getItemInfo",         false, "return null;");
		type.overrideMethod("getRelationshipInfo", false, "return null;");
		type.overrideMethod("getPolicyInfo",       false, "return null;");
		type.overrideMethod("getSecondaryInfo",    false, "return null;");
		type.overrideMethod("getParentId",         false, "return null;");//return getProperty(parentIdProperty);");
		type.overrideMethod("getPath",             false, "return getProperty(pathProperty);");
		type.overrideMethod("getAllowableActions", false, "return " + StructrFolderActions.class.getName() + ".getInstance();");
		type.overrideMethod("getChangeToken",      false, "return null;");

		type.relate(type, "CONTAINS", Cardinality.OneToMany, "folderParent", "folders");
		type.relate(type, "CONTAINS", Cardinality.OneToMany, "fileParent",   "files");
		type.relate(type, "CONTAINS", Cardinality.OneToMany, "imageParent",  "images");

	}}

	String getMountTarget();
	String getEnabledChecksums();

	Iterable<AbstractFile> getChildren();
	Iterable<Folder> getFolders();
	Iterable<Image> getImages();
	Iterable<File> getFiles();

	/*

	private static final Logger logger                                   = LoggerFactory.getLogger(Folder.class);

	public static final Property<List<Folder>>   folders                 = new EndNodes<>("folders", Folders.class, new PropertySetNotion(id, name));
	public static final Property<List<FileBase>> files                   = new EndNodes<>("files", Files.class, new PropertySetNotion(id, name));
	public static final Property<List<Image>>    images                  = new EndNodes<>("images", Images.class, new PropertySetNotion(id, name));
	public static final Property<Boolean>        isFolder                = new ConstantBooleanProperty("isFolder", true);
	public static final Property<User>           homeFolderOfUser        = new StartNode<>("homeFolderOfUser", UserHomeDir.class);

	public static final Property<String>         mountTarget             = new StringProperty("mountTarget").indexed();
	public static final Property<Boolean>        mountDoFulltextIndexing = new BooleanProperty("mountDoFulltextIndexing");
	public static final Property<Boolean>        mountWatchContents      = new BooleanProperty("mountWatchContents").defaultValue(true);
	public static final Property<Integer>        mountScanInterval       = new IntProperty("mountScanInterval");
	public static final Property<Long>           mountLastScanned        = new LongProperty("mountLastScanned");

	public static final Property<String>         enabledChecksums        = new StringProperty("enabledChecksums").indexed().hint("List of checksum types which are being automatically calculated on file creation.\nSupported values are: crc32, md5, sha1, sha512");

	public static final Property<Integer>        position                = new IntProperty("position").cmis().indexed();

	public static final View publicView = new View(Folder.class, PropertyView.Public,
		id, type, name, owner, isFolder, folders, files, parentId, visibleToPublicUsers, visibleToAuthenticatedUsers,
		mountTarget, mountDoFulltextIndexing, mountScanInterval, mountLastScanned, enabledChecksums
	);

	public static final View uiView = new View(Folder.class, PropertyView.Ui,
		parent, owner, folders, files, images, isFolder, includeInFrontendExport, mountTarget,
		mountDoFulltextIndexing, mountScanInterval, mountLastScanned, enabledChecksums
	);

	// register this type as an overridden builtin type
	static {
		SchemaService.registerBuiltinTypeOverride("Folder", Folder.class.getName());
	}

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (super.onCreation(securityContext, errorBuffer)) {

			setHasParent();

			// only update watch service for root folder of mounted hierarchy
			if (getProperty(mountTarget) != null || !isMounted()) {

				updateWatchService(true);
			}

			return true;
		}

		return false;
	}

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (super.onModification(securityContext, errorBuffer, modificationQueue)) {

			setHasParent();

			// only update watch service for root folder of mounted hierarchy
			if (getProperty(mountTarget) != null || !isMounted()) {

				updateWatchService(true);
			}

			return true;
		}

		return false;
	}

	@Override
	public boolean onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {

		if (super.onDeletion(securityContext, errorBuffer, properties)) {

			// only update watch service for root folder of mounted hierarchy
			if (properties.get(mountTarget) != null) {

				updateWatchService(false);
			}

			return true;
		}

		return false;
	}

	public void deleteRecursively(final boolean deleteRoot) throws FrameworkException {

		final App app = StructrApp.getInstance();

		for (final Folder folder : getProperty(Folder.folders)) {
			folder.deleteRecursively(true);
		}

		for (final FileBase file : getProperty(Folder.files)) {
			app.delete(file);
		}

		if (deleteRoot) {

			// delete post-ordered
			app.delete(this);
		}
	}

	// ----- interface Syncable -----
	@Override
	public List<GraphObject> getSyncData() throws FrameworkException {

		final List<GraphObject> data = super.getSyncData();

		// add full folder structure when resource sync is requested
		//if (state.hasFlag(SyncState.Flag.Images)) {

			data.addAll(getProperty(images));
			data.addAll(Iterables.toList(getOutgoingRelationships(Images.class)));
		//}

		// add full folder structure when resource sync is requested
		//if (state.hasFlag(SyncState.Flag.Files)) {

			data.addAll(getProperty(files));
			data.addAll(Iterables.toList(getOutgoingRelationships(Files.class)));
		//}

		// add full folder structure when resource sync is requested
		//if (state.hasFlag(SyncState.Flag.Folders)) {

			data.addAll(getProperty(folders));
			data.addAll(Iterables.toList(getOutgoingRelationships(Folders.class)));
		//}

		// parent only
		//data.add(getProperty(parent));
		//data.add(getIncomingRelationship(Folders.class));

		return data;
	}

	public java.io.File getFileOnDisk(final FileBase file, final String path, final boolean create) {

		final Folder parentFolder = getProperty(Folder.parent);
		final String _mountTarget = getProperty(Folder.mountTarget);

		if (_mountTarget != null) {

			final String fullPath         = removeDuplicateSlashes(_mountTarget + "/" + path + "/" + file.getProperty(FileBase.name));
			final java.io.File fileOnDisk = new java.io.File(fullPath);

			fileOnDisk.getParentFile().mkdirs();

			if (create && !isExternal()) {

				try {

					fileOnDisk.createNewFile();

				} catch (IOException ioex) {

					logger.error("Unable to create file {}: {}", file, ioex.getMessage());
				}
			}

			return fileOnDisk;

		} else if (parentFolder != null) {

			return parentFolder.getFileOnDisk(file, getProperty(Folder.name) + "/" + path, create);
		}

		// default implementation (store in UUID-indexed tree)
		return defaultGetFileOnDisk(file, create);
	}

	// ----- CMIS support -----
	@Override
	public CMISInfo getCMISInfo() {
		return this;
	}

	@Override
	public BaseTypeId getBaseTypeId() {
		return BaseTypeId.CMIS_FOLDER;
	}

	@Override
	public CMISFolderInfo getFolderInfo() {
		return this;
	}

	@Override
	public CMISDocumentInfo getDocumentInfo() {
		return null;
	}

	@Override
	public CMISItemInfo geItemInfo() {
		return null;
	}

	@Override
	public CMISRelationshipInfo getRelationshipInfo() {
		return null;
	}

	@Override
	public CMISPolicyInfo getPolicyInfo() {
		return null;
	}

	@Override
	public CMISSecondaryInfo getSecondaryInfo() {
		return null;
	}

	@Override
	public String getParentId() {
		return getProperty(FileBase.parentId);
	}

	@Override
	public String getPath() {
		return getProperty(AbstractFile.path);
	}

	@Override
	public AllowableActions getAllowableActions() {
		return StructrFolderActions.getInstance();
	}

	@Override
	public String getChangeToken() {

		// versioning not supported yet.
		return null;
	}

	// ----- private methods -----
	private void setHasParent() throws FrameworkException {

		synchronized (this) {

			// save current security context
			final SecurityContext previousSecurityContext = securityContext;

			// replace with SU context
			this.securityContext = SecurityContext.getSuperUserInstance();

			// set property as super user
			setProperties(this.securityContext, new PropertyMap(hasParent, getProperty(parentId) != null));

			// restore previous security context
			this.securityContext = previousSecurityContext;
		}
	}

	private String removeDuplicateSlashes(final String src) {
		return src.replaceAll("[/]+", "/");
	}

	private void updateWatchService(final boolean mount) {

		final DirectoryWatchService service = StructrApp.getInstance().getService(DirectoryWatchService.class);
		if (service != null && service.isRunning()) {

			if (mount) {

				service.mountFolder(this);

			} else {

				service.unmountFolder(this);
			}
		}
	}
	*/
}
