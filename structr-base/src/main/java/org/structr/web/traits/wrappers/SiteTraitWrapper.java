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

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.web.entity.Site;
import org.structr.web.traits.definitions.SiteTraitDefinition;

public class SiteTraitWrapper extends AbstractNodeTraitWrapper implements Site {

	public SiteTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getName() {
		return wrappedObject.getName();
	}

	@Override
	public String getHostname() {
		return wrappedObject.getProperty(traits.key(SiteTraitDefinition.HOSTNAME_PROPERTY));
	}

	@Override
	public Integer getPort() {
		return wrappedObject.getProperty(traits.key(SiteTraitDefinition.PORT_PROPERTY));
	}

	@Override
	public Iterable<NodeInterface> getPages() {
		return wrappedObject.getProperty(traits.key(SiteTraitDefinition.PAGES_PROPERTY));
	}
}
