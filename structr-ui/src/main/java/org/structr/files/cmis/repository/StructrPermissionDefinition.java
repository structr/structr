/*
 * Copyright (C) 2010-2021 Structr GmbH
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

import org.apache.chemistry.opencmis.commons.definitions.PermissionDefinition;
import org.structr.cmis.common.CMISExtensionsData;
import org.structr.common.Permission;

/**
 *
 *
 */
public class StructrPermissionDefinition extends CMISExtensionsData implements PermissionDefinition {

	private Permission permission = null;
	private String description    = null;

	public StructrPermissionDefinition(final Permission permission, final String description) {

		this.permission  = permission;
		this.description = description;
	}

	@Override
	public String getId() {
		return permission.name();
	}

	@Override
	public String getDescription() {
		return description;
	}
}
