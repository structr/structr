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
package org.structr.files.cmis.config;

import java.util.ArrayList;
import org.structr.cmis.common.CMISExtensionsData;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.chemistry.opencmis.commons.data.AclCapabilities;
import org.apache.chemistry.opencmis.commons.data.ExtensionFeature;
import org.apache.chemistry.opencmis.commons.data.PermissionMapping;
import org.apache.chemistry.opencmis.commons.data.RepositoryCapabilities;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.PermissionDefinition;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.enums.SupportedPermissions;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PermissionMappingDataImpl;
import org.structr.cmis.CMISInfo;
import org.structr.common.Permission;
import org.structr.core.entity.Principal;
import org.structr.files.cmis.repository.StructrPermissionDefinition;

/**
 *
 *
 */
public class StructrRepositoryInfo extends CMISExtensionsData implements RepositoryInfo, AclCapabilities {

	private static final Logger logger = Logger.getLogger(StructrRepositoryInfo.class.getName());

	@Override
	public String getId() {
		return "default";
	}

	@Override
	public String getName() {
		return "Structr CMIS Repository";
	}

	@Override
	public String getDescription() {
		return getName();
	}

	@Override
	public String getVendorName() {
		return "Structr";
	}

	@Override
	public String getProductName() {
		return "Structr CMIS";
	}

	@Override
	public String getProductVersion() {
		return "1.1";
	}

	@Override
	public String getRootFolderId() {
		return CMISInfo.ROOT_FOLDER_ID;
	}

	@Override
	public RepositoryCapabilities getCapabilities() {
		return new StructrRepositoryCapabilities();
	}

	@Override
	public AclCapabilities getAclCapabilities() {
		return this;
	}

	@Override
	public String getLatestChangeLogToken() {
		return null;
	}

	@Override
	public String getCmisVersionSupported() {
		return "1.1";
	}

	@Override
	public CmisVersion getCmisVersion() {
		return CmisVersion.CMIS_1_1;
	}

	@Override
	public String getThinClientUri() {
		return "http://localhost:8082/structr/";
	}

	@Override
	public Boolean getChangesIncomplete() {
		return false;
	}

	@Override
	public List<BaseTypeId> getChangesOnType() {
		return Collections.emptyList();
	}

	@Override
	public String getPrincipalIdAnonymous() {
		return Principal.ANONYMOUS;
	}

	@Override
	public String getPrincipalIdAnyone() {
		return Principal.ANYONE;
	}

	@Override
	public List<ExtensionFeature> getExtensionFeatures() {
		return Collections.emptyList();
	}

	// ----- interface AclCapabilities -----
	@Override
	public SupportedPermissions getSupportedPermissions() {
		return SupportedPermissions.REPOSITORY;
	}

	@Override
	public AclPropagation getAclPropagation() {
		return AclPropagation.OBJECTONLY;
	}

	@Override
	public List<PermissionDefinition> getPermissions() {

		final List<PermissionDefinition> permissions = new LinkedList<>();

		permissions.add(new StructrPermissionDefinition(Permission.read,          "Read access"));
		permissions.add(new StructrPermissionDefinition(Permission.write,         "Write access"));
		permissions.add(new StructrPermissionDefinition(Permission.delete,        "Delete access"));
		permissions.add(new StructrPermissionDefinition(Permission.accessControl, "Access control"));

		return permissions;
	}

	@Override
	public Map<String, PermissionMapping> getPermissionMapping() {

		List<PermissionMapping> list = new ArrayList<>();

		list.add(createMapping(PermissionMapping.CAN_GET_DESCENDENTS_FOLDER, Permission.read.name()));
		list.add(createMapping(PermissionMapping.CAN_GET_CHILDREN_FOLDER, Permission.read.name()));
		list.add(createMapping(PermissionMapping.CAN_GET_PARENTS_FOLDER, Permission.read.name()));
		list.add(createMapping(PermissionMapping.CAN_GET_FOLDER_PARENT_OBJECT, Permission.read.name()));
		list.add(createMapping(PermissionMapping.CAN_CREATE_DOCUMENT_FOLDER, Permission.read.name()));
		list.add(createMapping(PermissionMapping.CAN_CREATE_FOLDER_FOLDER, Permission.read.name()));
	//	list.add(createMapping(PermissionMapping.CAN_CREATE_RELATIONSHIP_SOURCE, CMIS_READ));
	//	list.add(createMapping(PermissionMapping.CAN_CREATE_RELATIONSHIP_TARGET, CMIS_READ));
		list.add(createMapping(PermissionMapping.CAN_GET_PROPERTIES_OBJECT, Permission.read.name()));
		list.add(createMapping(PermissionMapping.CAN_VIEW_CONTENT_OBJECT, Permission.read.name()));
		list.add(createMapping(PermissionMapping.CAN_UPDATE_PROPERTIES_OBJECT, Permission.write.name()));
		list.add(createMapping(PermissionMapping.CAN_MOVE_OBJECT, Permission.write.name()));
		list.add(createMapping(PermissionMapping.CAN_MOVE_TARGET, Permission.write.name()));
		list.add(createMapping(PermissionMapping.CAN_MOVE_SOURCE, Permission.write.name()));
		list.add(createMapping(PermissionMapping.CAN_DELETE_OBJECT, Permission.delete.name()));

		list.add(createMapping(PermissionMapping.CAN_DELETE_TREE_FOLDER, Permission.delete.name()));
		list.add(createMapping(PermissionMapping.CAN_SET_CONTENT_DOCUMENT, Permission.write.name()));
		list.add(createMapping(PermissionMapping.CAN_DELETE_CONTENT_DOCUMENT, Permission.delete.name()));
	//	list.add(createMapping(PermissionMapping.CAN_ADD_TO_FOLDER_OBJECT, CMIS_WRITE));
	//	list.add(createMapping(PermissionMapping.CAN_REMOVE_FROM_FOLDER_OBJECT, CMIS_WRITE));
	//	list.add(createMapping(PermissionMapping.CAN_CHECKOUT_DOCUMENT, CMIS_WRITE));
	//	list.add(createMapping(PermissionMapping.CAN_CANCEL_CHECKOUT_DOCUMENT, CMIS_WRITE));

	//	list.add(createMapping(PermissionMapping.CAN_CHECKIN_DOCUMENT, CMIS_WRITE));
	//	list.add(createMapping(PermissionMapping.CAN_GET_ALL_VERSIONS_VERSION_SERIES, CMIS_READ));
	//	list.add(createMapping(PermissionMapping.CAN_GET_OBJECT_RELATIONSHIPS_OBJECT, CMIS_READ));
	//	list.add(createMapping(PermissionMapping.CAN_ADD_POLICY_OBJECT, CMIS_WRITE));
	//	list.add(createMapping(PermissionMapping.CAN_REMOVE_POLICY_OBJECT, CMIS_WRITE));

	//	list.add(createMapping(PermissionMapping.CAN_GET_APPLIED_POLICIES_OBJECT, CMIS_READ));
		list.add(createMapping(PermissionMapping.CAN_GET_ACL_OBJECT, Permission.accessControl.name()));
		list.add(createMapping(PermissionMapping.CAN_APPLY_ACL_OBJECT, Permission.accessControl.name()));

		Map<String, PermissionMapping> map = new LinkedHashMap<>();

		for (PermissionMapping pm : list) {
			map.put(pm.getKey(), pm);
		}

		return map;
	}

	//---private methods---
	private PermissionMapping createMapping(String key, String permission) {

		PermissionMappingDataImpl pm = new PermissionMappingDataImpl();
		pm.setKey(key);
		pm.setPermissions(Collections.singletonList(permission));

		return pm;
	}
}
