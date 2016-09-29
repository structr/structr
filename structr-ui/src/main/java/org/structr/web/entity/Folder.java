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
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
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
import org.structr.core.graph.ModificationQueue;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.ConstantBooleanProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.files.cmis.config.StructrFolderActions;
import org.structr.schema.SchemaService;
import org.structr.web.entity.relation.Files;
import org.structr.web.entity.relation.Folders;
import org.structr.web.entity.relation.Images;
import org.structr.web.entity.relation.UserHomeDir;


//~--- classes ----------------------------------------------------------------

/**
 * The Folder entity.
 *
 *
 *
 */
public class Folder extends AbstractFile implements CMISInfo, CMISFolderInfo {

	public static final Property<List<Folder>>   folders                 = new EndNodes<>("folders", Folders.class, new PropertySetNotion(id, name));
	public static final Property<List<FileBase>> files                   = new EndNodes<>("files", Files.class, new PropertySetNotion(id, name));
	public static final Property<List<Image>>    images                  = new EndNodes<>("images", Images.class, new PropertySetNotion(id, name));
	public static final Property<Boolean>        isFolder                = new ConstantBooleanProperty("isFolder", true);
	public static final Property<Boolean>        includeInFrontendExport = new BooleanProperty("includeInFrontendExport").cmis().indexed();
	public static final Property<User>           homeFolderOfUser        = new StartNode<>("homeFolderOfUser", UserHomeDir.class);

	public static final Property<Integer>        position                = new IntProperty("position").cmis().indexed();

	public static final View publicView = new View(Folder.class, PropertyView.Public, id, type, name, owner, isFolder, folders, files, parentId);
	public static final View uiView     = new View(Folder.class, PropertyView.Ui, parent, owner, folders, files, images, isFolder, includeInFrontendExport);

	// register this type as an overridden builtin type
	static {
		SchemaService.registerBuiltinTypeOverride("Folder", Folder.class.getName());
	}

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (super.onCreation(securityContext, errorBuffer)) {
			setProperty(hasParent, getProperty(parentId) != null);
			return true;
		}

		return false;
	}

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (super.onModification(securityContext, errorBuffer, modificationQueue)) {

			synchronized (this) {

				// save current security context
				final SecurityContext previousSecurityContext = securityContext;

				// replace with SU context
				this.securityContext = SecurityContext.getSuperUserInstance();

				// set property as super user
				setProperty(hasParent, getProperty(parentId) != null);

				// restore previous security context
				this.securityContext = previousSecurityContext;
			}

			return true;
		}

		return false;
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
}
