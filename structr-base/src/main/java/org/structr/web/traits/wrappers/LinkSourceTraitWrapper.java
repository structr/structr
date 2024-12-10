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
import org.structr.core.traits.wrappers.AbstractTraitWrapper;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.Linkable;

/**
 * This class represents elements which can have an outgoing link to a resource.
 */
public class LinkSourceTraitWrapper extends AbstractTraitWrapper<NodeInterface> implements LinkSource {

	public LinkSourceTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getName() {
		return wrappedObject.getName();
	}

	public NodeInterface getLinkable() {
		return wrappedObject.getProperty(traits.key("linkable"));
	}

	public Object setLinkable(final Linkable linkable) throws FrameworkException {
		return wrappedObject.setProperty(traits.key("linkable"), linkable);
	}
}