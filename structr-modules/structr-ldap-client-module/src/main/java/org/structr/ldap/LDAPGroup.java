/**
 * Copyright (C) 2010-2019 Structr GmbH
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
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import static org.structr.core.GraphObject.logger;
import org.structr.core.Services;
import org.structr.core.entity.Group;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.TransactionCommand;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonMethod;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;

/**
 */
public interface LDAPGroup extends Group {

	static class Impl { static {

		final JsonSchema schema    = SchemaService.getDynamicSchema();
		final JsonObjectType type  = schema.addType("LDAPGroup");

		type.setExtends(schema.getType("Group"));
		type.setImplements(URI.create("https://structr.org/v1.1/definitions/LDAPGroup"));

		type.addStringProperty("distinguishedName", PropertyView.Public, PropertyView.Ui).setUnique(true).setIndexed(true);
		type.addStringProperty("path",              PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("filter",            PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("scope",             PropertyView.Public, PropertyView.Ui);

		type.addPropertyGetter("distinguishedName", String.class);
		type.addPropertySetter("distinguishedName", String.class);
		type.addPropertyGetter("path",              String.class);
		type.addPropertySetter("path",              String.class);
		type.addPropertyGetter("filter",            String.class);
		type.addPropertySetter("filter",            String.class);
		type.addPropertyGetter("scope",             String.class);
		type.addPropertySetter("scope",             String.class);

		final JsonMethod updateMethod = type.addMethod("update");
		updateMethod.setSource(LDAPGroup.class.getName() + ".update(getSecurityContext(), this);");
		updateMethod.setDoExport(true);

		type.overrideMethod("onCreation",     true,  LDAPGroup.class.getName() + ".onCreation(this, arg0, arg1);");
		type.overrideMethod("onModification", true,  LDAPGroup.class.getName() + ".onModification(this, arg0, arg1, arg2);");
	}}

	void setDistinguishedName(final String distinguishedName) throws FrameworkException;
	String getDistinguishedName();
	String getPath();
	String getFilter();
	String getScope();

	// ----- static methods -----
	static void onCreation(final LDAPGroup thisNode, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
		update(securityContext, thisNode);
	}

	static void onModification(final LDAPGroup thisNode, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
		update(securityContext, thisNode);
	}

	static void update(final SecurityContext securityContext, final LDAPGroup thisGroup) {

		final LDAPService ldapService = Services.getInstance().getService(LDAPService.class);
		if (ldapService != null) {

			try {

				ldapService.synchronizeGroup(thisGroup);

			} catch (Throwable t) {

				LoggerFactory.getLogger(LDAPGroup.class).warn("Unable to sync group {}: {}", thisGroup.getName(), t.getMessage());
			}

		} else {

			final String message = "LDAPService not available, is it configured in structr.conf?<br /><a href=\"/structr/config\" target=\"_blank\">Open Structr Configuration</a>";

			TransactionCommand.simpleBroadcastWarning("Service not configured", message, Predicate.only(securityContext.getSessionId()));


			logger.warn(message);
		}
	}
}
