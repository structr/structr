/**
 * Copyright (C) 2010-2017 Structr GmbH
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
import java.util.List;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.cmis.CMISInfo;
import org.structr.cmis.info.CMISDocumentInfo;
import org.structr.cmis.info.CMISFolderInfo;
import org.structr.cmis.info.CMISItemInfo;
import org.structr.cmis.info.CMISPolicyInfo;
import org.structr.cmis.info.CMISRelationshipInfo;
import org.structr.cmis.info.CMISSecondaryInfo;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.ConstantBooleanProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.files.cmis.config.StructrFolderActions;
import org.structr.files.external.DirectoryWatchService;
import org.structr.schema.SchemaService;
import org.structr.web.entity.relation.Files;
import org.structr.web.entity.relation.Folders;
import org.structr.web.entity.relation.Images;
import org.structr.web.entity.relation.UserHomeDir;


public class Folder extends AbstractFile implements CMISInfo, CMISFolderInfo {

	private static final Logger logger                                 = LoggerFactory.getLogger(Folder.class);

	public static final Property<List<Folder>>   folders               = new EndNodes<>("folders", Folders.class, new PropertySetNotion(id, name));
	public static final Property<List<FileBase>> files                 = new EndNodes<>("files", Files.class, new PropertySetNotion(id, name));
	public static final Property<List<Image>>    images                = new EndNodes<>("images", Images.class, new PropertySetNotion(id, name));
	public static final Property<Boolean>        isFolder              = new ConstantBooleanProperty("isFolder", true);
	public static final Property<User>           homeFolderOfUser      = new StartNode<>("homeFolderOfUser", UserHomeDir.class);

	public static final Property<String>         mountTarget           = new StringProperty("mountTarget").indexed();
	public static final Property<Boolean>        mountFulltextIndexing = new BooleanProperty("mountFulltextIndexing");
	public static final Property<Integer>        mountScanInterval     = new IntProperty("mountScanInterval");
	public static final Property<Long>           lastScanned           = new LongProperty("lastSeenMounted");

	public static final Property<Integer>        position              = new IntProperty("position").cmis().indexed();

	public static final View publicView = new View(Folder.class, PropertyView.Public,
		id, type, name, owner, isFolder, folders, files, parentId, visibleToPublicUsers, visibleToAuthenticatedUsers,
		mountTarget, mountFulltextIndexing, mountScanInterval, lastScanned, isExternal
	);

	public static final View uiView = new View(Folder.class, PropertyView.Ui,
		parent, owner, folders, files, images, isFolder, includeInFrontendExport, mountTarget, isExternal,
		mountFulltextIndexing, mountScanInterval, lastScanned
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
		if (parentFolder != null) {

			return parentFolder.getFileOnDisk(file, getProperty(Folder.name) + "/" + path, create);
		}

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
}
