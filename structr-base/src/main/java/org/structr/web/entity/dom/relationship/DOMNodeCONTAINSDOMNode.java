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
package org.structr.web.entity.dom.relationship;

import org.structr.api.graph.Direction;
import org.structr.api.graph.PropertyContainer;
import org.structr.core.entity.ManyEndpoint;
import org.structr.core.entity.OneStartpoint;
import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.OneToManyTrait;
import org.structr.core.traits.Trait;
import org.structr.web.entity.dom.DOMNode;

public interface DOMNodeCONTAINSDOMNode extends OneToMany<DOMNode, DOMNode> {

	public static final Property<Integer> position  = new IntProperty("position");

	class Impl extends OneToManyTrait<DOMNode, DOMNode> implements DOMNodeCONTAINSDOMNode {

		public Impl(final PropertyContainer propertyContainer) {
			super(propertyContainer);
		}

		@Override
		public Trait<DOMNode> getSourceType() {
			return Trait.of(DOMNode.class);
		}

		@Override
		public Trait<DOMNode> getTargetType() {
			return Trait.of(DOMNode.class);
		}

		@Override
		public Trait<Relation<DOMNode, DOMNode, OneStartpoint<DOMNode>, ManyEndpoint<DOMNode>>> getTrait() {
			return Trait.of(DOMNodeCONTAINSDOMNode.class);
		}

		@Override
		public Direction getDirectionForType(final Trait<?> type) {
			return null;
		}

		@Override
		public Property<String> getSourceIdProperty() {
			return null;
		}

		@Override
		public Property<String> getTargetIdProperty() {
			return null;
		}

		@Override
		public void setSourceProperty(PropertyKey source) {

		}

		@Override
		public void setTargetProperty(PropertyKey target) {

		}

		@Override
		public PropertyKey getSourceProperty() {
			return null;
		}

		@Override
		public PropertyKey getTargetProperty() {
			return null;
		}

		@Override
		public String name() {
			return "CONTAINS";
		}

		@Override
		public String getName() {
			return "";
		}
	}
}
