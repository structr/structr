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
package org.structr.files.cmis.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.cmis.CMISInfo;
import org.structr.cmis.common.CMISExtensionsData;
import org.structr.common.Permission;
import org.structr.core.entity.Principal;
import org.structr.files.cmis.repository.StructrAccessControlPermissionMapping;
import org.structr.files.cmis.repository.StructrDeletePermissionMapping;
import org.structr.files.cmis.repository.StructrPermissionDefinition;
import org.structr.files.cmis.repository.StructrReadPermissionMapping;
import org.structr.files.cmis.repository.StructrWritePermissionMapping;

/**
 *
 *
 */
public class StructrRepositoryInfo extends CMISExtensionsData implements RepositoryInfo, AclCapabilities {

	private static final Logger logger = LoggerFactory.getLogger(StructrRepositoryInfo.class.getName());

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
		permissions.add(new StructrPermissionDefinition(Permission.delete,        "Write access"));
		permissions.add(new StructrPermissionDefinition(Permission.accessControl, "Access control"));

		return permissions;
	}

	@Override
	public Map<String, PermissionMapping> getPermissionMapping() {

		final Map<String, PermissionMapping> mapping = new LinkedHashMap<>();

		mapping.put(Permission.read.name(),          new StructrReadPermissionMapping());
		mapping.put(Permission.write.name(),         new StructrWritePermissionMapping());
		mapping.put(Permission.delete.name(),        new StructrDeletePermissionMapping());
		mapping.put(Permission.accessControl.name(), new StructrAccessControlPermissionMapping());

		return mapping;
	}
}
