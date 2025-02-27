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

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.UserTraitWrapper;
import org.structr.ldap.LDAPGroup;
import org.structr.ldap.LDAPService;
import org.structr.ldap.LDAPUser;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */
public class LDAPUserTraitWrapper extends UserTraitWrapper implements LDAPUser {

        private static final Logger logger = LoggerFactory.getLogger(LDAPUserTraitWrapper.class);

        public LDAPUserTraitWrapper(final Traits traits, final NodeInterface node) {
                super(traits, node);
        }

        @Override
	public String getOriginId() {
		return wrappedObject.getProperty(traits.key("originId"));
	}

        @Override
        public String getDistinguishedName() {
                return wrappedObject.getProperty(traits.key("distinguishedName"));
        }

        @Override
        public void setDistinguishedName(final String name) throws FrameworkException {
                wrappedObject.setProperty(traits.key("distinguishedName"), name);
        }

        @Override
	public void setLastLDAPSync(final Long time) throws FrameworkException {
		wrappedObject.setProperty(traits.key("lastLDAPSync"), time);
	}

        @Override
	public void initializeFrom(final Entry entry) throws FrameworkException {

		final LDAPService ldapService      = Services.getInstance().getService(LDAPService.class, "default");
		final Map<String, String> mappings = new LinkedHashMap<>();

		if (ldapService != null) {

			mappings.putAll(ldapService.getPropertyMapping());
		}

		try {

			// apply mappings
			for (final String key : mappings.keySet()) {

				final String structrName = mappings.get(key);
				final String ldapName    = key;

				wrappedObject.setProperty(traits.key(structrName), LDAPUserTraitWrapper.getString(entry, ldapName));
			}

			// store DN
			setDistinguishedName(entry.getDn().getNormName());

			// update lastUpdate timestamp
			this.setProperty(traits.key("lastLDAPSync"), System.currentTimeMillis());


		} catch (final LdapInvalidAttributeValueException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
		}
	}

        @Override
	public void update() {

		final PropertyKey<Long> lastUpdateKey = traits.key("lastLDAPSync");

		try {

			final LDAPService service = Services.getInstance().getService(LDAPService.class, "default");
			if (service != null) {

				for (final NodeInterface group : StructrApp.getInstance().nodeQuery("LDAPGroup").getAsList()) {

					service.synchronizeGroup(group.as(LDAPGroup.class));
				}
			}

			this.setProperty(lastUpdateKey, System.currentTimeMillis());

		} catch (CursorException | LdapException | IOException | FrameworkException fex) {
			logger.warn("Unable to update LDAP information for user {}: {}", this.getName(), fex.getMessage());
		}
	}

	public static String getString(final Entry entry, final String key) throws LdapInvalidAttributeValueException {

		final Attribute attribute = entry.get(key);
		if (attribute != null) {

			return attribute.getString();
		}

		return null;
	}
}
