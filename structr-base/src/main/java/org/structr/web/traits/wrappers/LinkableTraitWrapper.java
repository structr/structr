/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.web.entity.Linkable;
import org.structr.web.traits.definitions.LinkableTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMElementTraitDefinition;

public class LinkableTraitWrapper extends AbstractNodeTraitWrapper implements Linkable {

	public LinkableTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public boolean getEnableBasicAuth() {
		return wrappedObject.getProperty(traits.key(LinkableTraitDefinition.ENABLE_BASIC_AUTH_PROPERTY));
	}

	@Override
	public String getBasicAuthRealm() {
		return wrappedObject.getProperty(traits.key(LinkableTraitDefinition.BASIC_AUTH_REALM_PROPERTY));
	}

	@Override
	public String getPath() {
		return wrappedObject.getProperty(traits.key(DOMElementTraitDefinition.PATH_PROPERTY));
	}
}
