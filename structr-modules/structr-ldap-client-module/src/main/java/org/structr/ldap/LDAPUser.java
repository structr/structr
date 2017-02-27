/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.Services;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.User;

/**
 *
 */
public class LDAPUser extends User {

	private static final Logger logger = LoggerFactory.getLogger(LDAPUser.class);
	
	public static final Property<String> distinguishedName = new StringProperty("distinguishedName").unique().indexed();
	public static final Property<String> description       = new StringProperty("description").indexed();
	public static final Property<String> commonName        = new StringProperty("commonName").indexed();
	public static final Property<String> entryUuid         = new StringProperty("entryUuid").unique().indexed();


	public static final org.structr.common.View uiView = new org.structr.common.View(LDAPUser.class, PropertyView.Ui,
		distinguishedName, entryUuid, commonName, description
	);

	public static final org.structr.common.View publicView = new org.structr.common.View(LDAPUser.class, PropertyView.Public,
		distinguishedName, entryUuid, commonName, description
	);

	public void initializeFrom(final Entry entry) throws FrameworkException, LdapInvalidAttributeValueException {

		setProperty(LDAPUser.description, getString(entry, "description"));
		setProperty(LDAPUser.entryUuid,   getString(entry, "entryUUID"));
		setProperty(LDAPUser.name,        getString(entry, "uid"));
		setProperty(LDAPUser.commonName,  getString(entry, "cn"));
		setProperty(LDAPUser.eMail,       getString(entry, "mail"));
	}

	@Override
	public boolean isValidPassword(final String password) {

		final LDAPService ldapService = Services.getInstance().getService(LDAPService.class);
		final String dn               = getProperty(distinguishedName);

		if (ldapService != null) {

			return ldapService.canSuccessfullyBind(dn, password);

		} else {

			logger.warn("Unable to reach LDAP server for authentication of {}", dn);
		}

		return false;
	}

	@Export
	public void printDebug() {

		final LDAPService ldapService = Services.getInstance().getService(LDAPService.class);
		final String dn               = getProperty(distinguishedName);

		if (ldapService != null) {

			System.out.println(ldapService.fetchObjectInfo(dn));

		} else {

			logger.warn("Unable to reach LDAP server for user information of {}", dn);
		}
	}



	// ----- private methods -----
	private String getString(final Entry entry, final String key) throws LdapInvalidAttributeValueException {

		final Attribute attribute = entry.get(key);
		if (attribute != null) {

			return attribute.getString();
		}

		return null;
	}
}
