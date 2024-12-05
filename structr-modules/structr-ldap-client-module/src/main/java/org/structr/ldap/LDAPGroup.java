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

import org.structr.core.traits.definitions.GroupTraitDefinition;

/**
 */
public interface LDAPGroup extends GroupTraitDefinition {

	String getDistinguishedName();
	String getPath();
	String getFilter();
	String getScope();

	/*

	static final Logger logger = LoggerFactory.getLogger(LDAPGroup.class);

	public static final Property<String> distinguishedNameProperty = new StringProperty("distinguishedName").unique().indexed();
	public static final Property<String> pathProperty              = new StringProperty("path");
	public static final Property<String> filterProperty            = new StringProperty("filter");
	public static final Property<String> scopeProperty             = new StringProperty("scope");

	public static final View defaultView = new View(LDAPGroup.class, PropertyView.Public,
		distinguishedNameProperty, pathProperty, filterProperty, scopeProperty
	);

	public static final View uiView      = new View(LDAPGroup.class, PropertyView.Ui,
		distinguishedNameProperty, pathProperty, filterProperty, scopeProperty
	);

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidUniqueProperty(this, LDAPGroup.distinguishedNameProperty, errorBuffer);

		return valid;
	}

	public void setDistinguishedName(final String distinguishedName) throws FrameworkException {
		setProperty(distinguishedNameProperty, distinguishedName);
	}

	public String getDistinguishedName() {
		return getProperty(distinguishedNameProperty);
	}

	public String getPath() {
		return getProperty(pathProperty);
	}

	public String getFilter() {
		return getProperty(filterProperty);
	}

	public String getScope() {
		return getProperty(scopeProperty);
	}

	@Override
	public void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);
		update(securityContext);
	}

	@Override
	public void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue);
		update(securityContext);
	}

	@Export
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
	*/
}
