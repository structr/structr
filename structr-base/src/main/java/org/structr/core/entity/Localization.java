/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.function.LocalizeFunction;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;

public class Localization extends AbstractNode {

	public static final Property<String> localizedNameProperty = new StringProperty("localizedName").indexed().partOfBuiltInSchema();
	public static final Property<String> domainProperty        = new StringProperty("domain").indexed().partOfBuiltInSchema();
	public static final Property<String> localeProperty        = new StringProperty("locale").notNull().indexed().partOfBuiltInSchema();
	public static final Property<Boolean> importedProperty     = new BooleanProperty("imported").partOfBuiltInSchema();

	public static final View defaultView = new View(Localization.class, PropertyView.Public,
		localizedNameProperty, domainProperty, localeProperty, importedProperty
	);

	public static final View uiView = new View(Localization.class, PropertyView.Ui,
		localizedNameProperty, domainProperty, localeProperty, importedProperty
	);

	@Override

	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidPropertyNotNull(this, Localization.localeProperty, errorBuffer);

		return valid;
	}

	@Override
	public void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);

		setProperty(visibleToPublicUsers, true);
		setProperty(visibleToAuthenticatedUsers, true);

		LocalizeFunction.invalidateCache();
	}

	@Override
	public void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue);
		LocalizeFunction.invalidateCache();
	}

	@Override
	public void onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {

		super.onDeletion(securityContext, errorBuffer, properties);
		LocalizeFunction.invalidateCache();
	}
}
