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
package org.structr.core.traits.wrappers;

import org.structr.core.entity.Localization;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.LocalizationTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;

public class LocalizationTraitWrapper extends AbstractNodeTraitWrapper implements Localization {

	public LocalizationTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getLocalizedName() {
		return wrappedObject.getProperty(traits.key(LocalizationTraitDefinition.LOCALIZED_NAME_PROPERTY));
	}

	@Override
	public String getLocale() {
		return wrappedObject.getProperty(traits.key(LocalizationTraitDefinition.LOCALE_PROPERTY));
	}

	@Override
	public String getDomain() {
		return wrappedObject.getProperty(traits.key(LocalizationTraitDefinition.DOMAIN_PROPERTY));
	}

	@Override
	public boolean isImported() {
		return wrappedObject.getProperty(traits.key(LocalizationTraitDefinition.IMPORTED_PROPERTY));
	}
}
