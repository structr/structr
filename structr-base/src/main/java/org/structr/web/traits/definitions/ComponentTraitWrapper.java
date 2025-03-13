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
package org.structr.web.traits.definitions;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.web.entity.Component;
import org.structr.web.traits.wrappers.dom.DOMElementTraitWrapper;

/**
 * Represents a component. A component is an assembly of elements
 */
public class ComponentTraitWrapper extends DOMElementTraitWrapper implements Component {

	public ComponentTraitWrapper(final Traits traits, final NodeInterface node) {
		super(traits, node);
	}
}
