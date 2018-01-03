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

import java.net.URI;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;
import org.structr.web.entity.User;

/**
 *
 */
public interface LDAPUser extends User {

	static class Impl { static {

		final JsonSchema schema    = SchemaService.getDynamicSchema();
		final JsonObjectType type  = schema.addType("LDAPUser");

		type.setExtends(schema.getType("User"));
		type.setImplements(URI.create("https://structr.org/v1.1/definitions/LDAPUser"));

		type.addStringProperty("distinguishedName", PropertyView.Public).setUnique(true).setIndexed(true);
		type.addStringProperty("description",       PropertyView.Public).setIndexed(true);
		type.addStringProperty("commonName",        PropertyView.Public).setIndexed(true);
		type.addStringProperty("entryUuid",         PropertyView.Public).setUnique(true).setIndexed(true);

		type.addPropertyGetter("distinguishedName", String.class);
		type.addPropertyGetter("description",       String.class);
		type.addPropertyGetter("commonName",        String.class);
		type.addPropertyGetter("entryUuid",         String.class);

		type.addPropertySetter("distinguishedName", String.class);
		type.addPropertySetter("description",       String.class);
		type.addPropertySetter("commonName",        String.class);
		type.addPropertySetter("entryUuid",         String.class);

		type.overrideMethod("initializeFrom",  false, LDAPUser.class.getName() + ".initializeFrom(this, arg0);");
		type.overrideMethod("printDebug",      false, LDAPUser.class.getName() + ".printDebug(this);").setDoExport(true);
		type.overrideMethod("isValidPassword", false, "return " + LDAPUser.class.getName() + ".isValidPassword(this, arg0);");
	}}

	String getDistinguishedName();
	String getDescription();
	String getCommonName();
	String getEntryUuid();

	void initializeFrom(final Entry entry) throws FrameworkException, LdapInvalidAttributeValueException;
	void setDistinguishedName(final String distinguishedName) throws FrameworkException;
	void setDescription(final String description) throws FrameworkException;
	void setCommonName(final String commonName) throws FrameworkException;
	void setEntryUuid(final String uuid) throws FrameworkException;

	static void initializeFrom(final LDAPUser thisUser, final Entry entry) throws FrameworkException, LdapInvalidAttributeValueException {

		thisUser.setProperty(StructrApp.key(LDAPUser.class, "description"), LDAPUser.getString(entry, "description"));
		thisUser.setProperty(StructrApp.key(LDAPUser.class, "entryUuid"),   LDAPUser.getString(entry, "entryUUID"));
		thisUser.setProperty(StructrApp.key(LDAPUser.class, "name"),        LDAPUser.getString(entry, "uid"));
		thisUser.setProperty(StructrApp.key(LDAPUser.class, "commonName"),  LDAPUser.getString(entry, "cn"));
		thisUser.setProperty(StructrApp.key(LDAPUser.class, "eMail"),       LDAPUser.getString(entry, "mail"));
	}

	static boolean isValidPassword(final LDAPUser thisUser, final String password) {

		final LDAPService ldapService = Services.getInstance().getService(LDAPService.class);
		final String dn               = thisUser.getDistinguishedName();

		if (ldapService != null) {

			return ldapService.canSuccessfullyBind(dn, password);

		} else {

			logger.warn("Unable to reach LDAP server for authentication of {}", dn);
		}

		return false;
	}

	static void printDebug(final LDAPUser thisUser) {

		final LDAPService ldapService = Services.getInstance().getService(LDAPService.class);
		final String dn               = thisUser.getDistinguishedName();

		if (ldapService != null) {

			System.out.println(ldapService.fetchObjectInfo(dn));

		} else {

			logger.warn("Unable to reach LDAP server for user information of {}", dn);
		}
	}

	static String getString(final Entry entry, final String key) throws LdapInvalidAttributeValueException {

		final Attribute attribute = entry.get(key);
		if (attribute != null) {

			return attribute.getString();
		}

		return null;
	}
}
