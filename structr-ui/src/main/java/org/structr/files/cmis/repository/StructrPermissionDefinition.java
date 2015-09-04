package org.structr.files.cmis.repository;

import org.structr.cmis.common.CMISExtensionsData;
import org.apache.chemistry.opencmis.commons.definitions.PermissionDefinition;
import org.structr.common.Permission;

/**
 *
 * @author Christian Morgner
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
