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
package org.structr.ldap;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.api.service.Command;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.rest.resource.MaintenanceParameterResource;

/**
 *
 */
public class LDAPCommand extends Command implements MaintenanceCommand {

	static {

		MaintenanceParameterResource.registerMaintenanceCommand("updateLDAP", LDAPCommand.class);
	}

	@Override
	public Class getServiceClass() {
		return LDAPService.class;
	}

	@Override
	public void execute(Map<String, Object> attributes) throws FrameworkException {

		final LDAPService ldapService = Services.getInstance().getService(LDAPService.class);
		if (ldapService != null) {

			try {

				ldapService.doUpdate();

			} catch (Throwable t) {
				Logger.getLogger(LDAPCommand.class.getName()).log(Level.WARNING, "Unable to update LDAP information.", t);
			}
		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}
}
