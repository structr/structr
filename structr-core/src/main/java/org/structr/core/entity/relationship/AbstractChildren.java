/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity.relationship;

import java.util.Collections;
import java.util.List;
import org.structr.common.SyncState;
import org.structr.common.Syncable;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.entity.OneToMany;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;

/**
 *
 * @author Christian Morgner
 */
public abstract class AbstractChildren<S extends LinkedTreeNode, T extends LinkedTreeNode> extends OneToMany<S, T> implements Syncable {

	public static final Property<Integer> position = new IntProperty("position").indexed();

	@Override
	public String name() {
		return "CONTAINS";
	}

	// ----- interface Syncable -----
	@Override
	public List<Syncable> getSyncData(final SyncState state) {
		return Collections.emptyList();
	}

	@Override
	public boolean isNode() {
		return false;
	}

	@Override
	public boolean isRelationship() {
		return true;
	}

	@Override
	public NodeInterface getSyncNode() {
		return null;
	}

	@Override
	public RelationshipInterface getSyncRelationship() {
		return this;
	}

	@Override
	public void updateFrom(PropertyMap properties) throws FrameworkException {
	}
}
