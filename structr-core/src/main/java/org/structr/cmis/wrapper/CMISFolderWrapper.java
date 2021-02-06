/*
 * Copyright (C) 2010-2021 Structr GmbH
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
package org.structr.cmis.wrapper;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.structr.cmis.CMISInfo;
import org.structr.cmis.info.CMISFolderInfo;
import org.structr.common.error.FrameworkException;

/**
 *
 *
 */
public class CMISFolderWrapper extends CMISObjectWrapper<CMISFolderInfo> {

	private String changeToken = null;
	private String parentId    = null;
	private String path        = null;

	public CMISFolderWrapper(final String propertyFilter, final Boolean includeAllowableActions) {
		super(BaseTypeId.CMIS_FOLDER, propertyFilter, includeAllowableActions);
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setChangeToken(String changeToken) {
		this.changeToken = changeToken;
	}

	@Override
	public void createProperties(final BindingsObjectFactory factory, final FilteredPropertyList properties) {

		properties.add(factory.createPropertyStringData(PropertyIds.PARENT_ID, parentId == null && !isRootFolder() ? CMISInfo.ROOT_FOLDER_ID : parentId));
		properties.add(factory.createPropertyStringData(PropertyIds.PATH, path));
		properties.add(factory.createPropertyStringData(PropertyIds.CHANGE_TOKEN, changeToken));

		// added for specification compliance
		properties.add(factory.createPropertyIdData(PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS, (String)null));
		properties.add(factory.createPropertyIdData(PropertyIds.SECONDARY_OBJECT_TYPE_IDS, (String)null));
	}

	@Override
	public void initializeFrom(final CMISFolderInfo info) throws FrameworkException {

		super.initializeFrom(info);

		this.changeToken = info.getChangeToken();
		this.parentId    = info.getParentId();
		this.path        = info.getPath();
	}

	// ----- protected methods -----
	protected boolean isRootFolder() {
		return false;
	}
}
