/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.notion;

import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;

/**
 * Combines an {@link ObjectSerializationStrategy} and an
 * {@link IdDeserializationStrategy} to read/write a
 * {@link org.structr.core.GraphObject}. This is the default notion.
 *
 * @author Christian Morgner
 */
public class ObjectNotion extends Notion {

	public ObjectNotion() {
		this(
			new ObjectSerializationStrategy(),
			new IdDeserializationStrategy()
		);
	}

	public ObjectNotion(SerializationStrategy serializationStrategy, DeserializationStrategy deserializationStrategy) {
		super(serializationStrategy, deserializationStrategy);
	}

	/**
	 * Identity serialization strategy. This class returns the object unmodified.
	 */
	public static class ObjectSerializationStrategy implements SerializationStrategy {
		@Override
		public Object serialize(SecurityContext securityContext, Class type, GraphObject source) {
			return source;
		}
	}
	

	@Override
	public PropertyKey getPrimaryPropertyKey() {
		return AbstractNode.uuid;
	}
}
