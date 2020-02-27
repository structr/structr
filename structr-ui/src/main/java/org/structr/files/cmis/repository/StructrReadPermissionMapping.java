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
package org.structr.files.cmis.repository;

import java.util.LinkedList;
import java.util.List;
import org.apache.chemistry.opencmis.commons.data.PermissionMapping;
import org.structr.cmis.common.CMISExtensionsData;
import org.structr.common.Permission;

/**
 *
 *
 */
public class StructrReadPermissionMapping extends CMISExtensionsData implements PermissionMapping {

	@Override
	public String getKey() {
		return Permission.read.name();
	}

	@Override
	public List<String> getPermissions() {

		final List<String> permissions = new LinkedList<>();

		// this is the full list, comment in/out what fits
		permissions.add(PermissionMapping.CAN_GET_DESCENDENTS_FOLDER);
		permissions.add(PermissionMapping.CAN_GET_CHILDREN_FOLDER);
		permissions.add(PermissionMapping.CAN_GET_PARENTS_FOLDER);
		permissions.add(PermissionMapping.CAN_GET_FOLDER_PARENT_OBJECT);
		//permissions.add(PermissionMapping.CAN_CREATE_DOCUMENT_FOLDER);
		//permissions.add(PermissionMapping.CAN_CREATE_FOLDER_FOLDER);
		//permissions.add(PermissionMapping.CAN_CREATE_POLICY_FOLDER);
		//permissions.add(PermissionMapping.CAN_CREATE_RELATIONSHIP_SOURCE);
		//permissions.add(PermissionMapping.CAN_CREATE_RELATIONSHIP_TARGET);
		permissions.add(PermissionMapping.CAN_GET_PROPERTIES_OBJECT);
		permissions.add(PermissionMapping.CAN_VIEW_CONTENT_OBJECT);
		//permissions.add(PermissionMapping.CAN_UPDATE_PROPERTIES_OBJECT);
		//permissions.add(PermissionMapping.CAN_MOVE_OBJECT);
		//permissions.add(PermissionMapping.CAN_MOVE_TARGET);
		//permissions.add(PermissionMapping.CAN_MOVE_SOURCE);
		//permissions.add(PermissionMapping.CAN_DELETE_OBJECT);
		//permissions.add(PermissionMapping.CAN_DELETE_TREE_FOLDER);
		//permissions.add(PermissionMapping.CAN_SET_CONTENT_DOCUMENT);
		//permissions.add(PermissionMapping.CAN_DELETE_CONTENT_DOCUMENT);
		//permissions.add(PermissionMapping.CAN_ADD_TO_FOLDER_OBJECT);
		//permissions.add(PermissionMapping.CAN_ADD_TO_FOLDER_FOLDER);
		//permissions.add(PermissionMapping.CAN_REMOVE_FROM_FOLDER_OBJECT);
		//permissions.add(PermissionMapping.CAN_REMOVE_FROM_FOLDER_FOLDER);
		permissions.add(PermissionMapping.CAN_CHECKOUT_DOCUMENT);
		permissions.add(PermissionMapping.CAN_CANCEL_CHECKOUT_DOCUMENT);
		//permissions.add(PermissionMapping.CAN_CHECKIN_DOCUMENT);
		//permissions.add(PermissionMapping.CAN_GET_ALL_VERSIONS_VERSION_SERIES);
		permissions.add(PermissionMapping.CAN_GET_OBJECT_RELATIONSHIPS_OBJECT);
		//permissions.add(PermissionMapping.CAN_ADD_POLICY_OBJECT);
		//permissions.add(PermissionMapping.CAN_ADD_POLICY_POLICY);
		//permissions.add(PermissionMapping.CAN_REMOVE_POLICY_OBJECT);
		//permissions.add(PermissionMapping.CAN_REMOVE_POLICY_POLICY);
		//permissions.add(PermissionMapping.CAN_GET_APPLIED_POLICIES_OBJECT);
		permissions.add(PermissionMapping.CAN_GET_ACL_OBJECT);
		//permissions.add(PermissionMapping.CAN_APPLY_ACL_OBJECT);



		return permissions;
	}
}
