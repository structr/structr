/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.ldap.traits.wrappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.GroupTraitWrapper;
import org.structr.ldap.LDAPGroup;
import org.structr.ldap.LDAPService;

/**
 */
public class LDAPGroupTraitWrapper extends GroupTraitWrapper implements LDAPGroup {

	private static final Logger logger = LoggerFactory.getLogger(LDAPGroup.class);

	public LDAPGroupTraitWrapper(final Traits traits, final NodeInterface nodeInterface) {
		super(traits, nodeInterface);
	}

	public void setDistinguishedName(final String distinguishedName) throws FrameworkException {
		wrappedObject.setProperty(traits.key("distinguishedName"), distinguishedName);
	}

	public String getDistinguishedName() {
		return wrappedObject.getProperty(traits.key("distinguishedName"));
	}

	public String getPath() {
		return wrappedObject.getProperty(traits.key("path"));
	}

	public String getFilter() {
		return wrappedObject.getProperty(traits.key("filter"));
	}

	public String getScope() {
		return wrappedObject.getProperty(traits.key("scope"));
	}

	@Override
	public void update(final SecurityContext securityContext) {

		final LDAPService ldapService = Services.getInstance().getService(LDAPService.class, "default");
		if (ldapService != null) {

			try {

				ldapService.synchronizeGroup(this);

			} catch (Throwable t) {

				logger.warn("Unable to sync group " + this.getName(), t.getMessage());
			}

		} else {

			final String message = "LDAPService not available, is it configured in structr.conf?<br /><a href=\"/structr/config\" target=\"_blank\">Open Structr Configuration</a>";

			TransactionCommand.simpleBroadcastWarning("Service not configured", message, Predicate.only(securityContext.getSessionId()));

			logger.warn(message);
		}
	}
}
