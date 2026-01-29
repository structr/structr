/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.SessionDataNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SessionDataNodeTraitDefinition;

public class SessionDataNodeTraitWrapper extends AbstractNodeTraitWrapper implements SessionDataNode {

	public SessionDataNodeTraitWrapper(final Traits traits, final NodeInterface node) {
		super(traits, node);
	}

	@Override
	public void incrementVersion() throws FrameworkException {

		final PropertyKey<Long> versionProperty = traits.key(SessionDataNodeTraitDefinition.VERSION_PROPERTY);

		// increment version on each change
		final Long version = wrappedObject.getProperty(versionProperty);
		if (version == null) {

			wrappedObject.setProperty(versionProperty, 1L);

		} else {

			wrappedObject.setProperty(versionProperty,  version + 1);
		}
	}
}
