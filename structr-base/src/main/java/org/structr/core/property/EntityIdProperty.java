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
package org.structr.core.property;

import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.traits.Traits;

/**
 * A property that wraps a PropertyNotion with the entity's UUID around an EntityProperty.
 *
 *
 * @param <S>
 */
public class EntityIdProperty extends EntityNotionProperty<NodeInterface, String> {
	
	public EntityIdProperty(final String name, final Property<NodeInterface> base) {
		this(name, base, false);
	}
	
	public EntityIdProperty(final String name, final Property<NodeInterface> base, final boolean createIfNotExisting) {
		
		super(name, base, new PropertyNotion(Traits.idProperty(), createIfNotExisting));
	}
}
