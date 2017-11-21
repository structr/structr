/**
 * Copyright (C) 2010-2017 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import static org.structr.core.GraphObject.visibleToAuthenticatedUsers;
import static org.structr.core.GraphObject.visibleToPublicUsers;
import org.structr.core.function.LocalizeFunction;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonType;

public interface Localization extends NodeInterface {

	static class Impl { static {

		final JsonType type = SchemaService.getDynamicSchema().addType("Localizaton");

		type.addStringProperty("localizedName", PropertyView.Public).setIndexed(true);
		type.addStringProperty("description",   PropertyView.Public);
		type.addStringProperty("domain",        PropertyView.Public).setIndexed(true);
		type.addStringProperty("locale",        PropertyView.Public).setRequired(true).setIndexed(true);
		type.addBooleanProperty("imported",     PropertyView.Public).setIndexed(true);


		type.addMethod("boolean", "onCreation", "final SecurityContext securityContext, final ErrorBuffer errorBuffer",

	}}

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (super.onCreation(securityContext, errorBuffer)) {

			setProperty(visibleToPublicUsers, true);
			setProperty(visibleToAuthenticatedUsers, true);

			LocalizeFunction.invalidateCache();

			return true;
		}

		return false;
	}

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (super.onModification(securityContext, errorBuffer, modificationQueue)) {

			LocalizeFunction.invalidateCache();
			return true;
		}

		return false;
	}

	/*
	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidStringNotBlank(this, Localization.name, errorBuffer);
		valid &= ValidationHelper.isValidStringNotBlank(this, Localization.locale, errorBuffer);

		return valid;
	}
	*/
}
