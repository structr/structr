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
package org.structr.web.traits.wrappers;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.Linkable;
import org.structr.web.traits.definitions.LinkSourceTraitDefinition;

/**
 * This class represents elements which can have an outgoing link to a resource.
 */
public class LinkSourceTraitWrapper extends AbstractNodeTraitWrapper implements LinkSource {

	public LinkSourceTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getName() {
		return wrappedObject.getName();
	}

	public Linkable getLinkable() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(LinkSourceTraitDefinition.LINKABLE_PROPERTY));
		if (node != null) {

			return node.as(Linkable.class);
		}

		return null;
	}

	public Object setLinkable(final Linkable linkable) throws FrameworkException {
		return wrappedObject.setProperty(traits.key(LinkSourceTraitDefinition.LINKABLE_PROPERTY), linkable);
	}
}