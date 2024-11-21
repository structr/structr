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
package org.structr.ldap;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.exception.DeleteInvalidUserException;
import org.structr.core.entity.Group;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.User;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */
public interface LDAPUser extends User {

        void initializeFrom(final Entry entry) throws FrameworkException;

	/*

	static final Logger logger = LoggerFactory.getLogger(LDAPUser.class);

	public static final Property<String> originIdProperty          = new StringProperty("originId").unique().indexed();
	public static final Property<String> distinguishedNameProperty = new StringProperty("distinguishedName").indexed();
	public static final Property<Long> lastLDAPSyncProperty        = new LongProperty("lastLDAPSync");

	public static final View defaultView = new View(LDAPUser.class, PropertyView.Public,
		originIdProperty, distinguishedNameProperty
	);

	public static final View uiView      = new View(LDAPUser.class, PropertyView.Ui,
		originIdProperty, distinguishedNameProperty
	);

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidUniqueProperty(this, LDAPUser.distinguishedNameProperty, errorBuffer);
		valid &= ValidationHelper.isValidUniqueProperty(this, LDAPUser.originIdProperty, errorBuffer);

		return valid;
	}

	public String getOriginId() {
		return getProperty(originIdProperty);
	}

	public String getDistinguishedName() {
		return getProperty(distinguishedNameProperty);
	}

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

				this.setProperty(StructrApp.key(LDAPUser.class, structrName), LDAPUser.getString(entry, ldapName));
			}

			// store DN
			this.setProperty(StructrApp.key(LDAPUser.class, "distinguishedName"), entry.getDn().getNormName());

			// update lastUpdate timestamp
			this.setProperty(StructrApp.key(LDAPUser.class, "lastLDAPSync"), System.currentTimeMillis());


		} catch (final LdapInvalidAttributeValueException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
		}
	}

	@Override
	public boolean isValidPassword(final String password) {

		final LDAPService ldapService = Services.getInstance().getService(LDAPService.class, "default");
		boolean hasLDAPGroups         = false;

		if (ldapService != null) {

			// update user..
			updateUser();

			for (final Group group : this.getGroups()) {

				if (group instanceof LDAPGroup) {

					hasLDAPGroups = true;
					break;
				}
			}

			if (!hasLDAPGroups) {

				final String uuid = this.getUuid();

				logger.warn("LDAPUser {} with UUID {} is not associated with an LDAPGroup, removing.", this.getName(), uuid);

				// this user must be deleted immediately
				throw new DeleteInvalidUserException(uuid);
			}

			return ldapService.canSuccessfullyBind(this.getDistinguishedName(), password);

		} else {

			logger.warn("Unable to reach LDAP server for authentication of {}", this.getDistinguishedName());
		}

		return false;
	}

	public void updateUser() {

		final PropertyKey<Long> lastUpdateKey = StructrApp.key(LDAPUser.class, "lastLDAPSync");

		try {

			final LDAPService service = Services.getInstance().getService(LDAPService.class, "default");
			if (service != null) {

				for (final LDAPGroup group : StructrApp.getInstance().nodeQuery(LDAPGroup.class).getAsList()) {

					service.synchronizeGroup(group);
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
	*/
}
