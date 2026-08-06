/*
 * Copyright (C) 2010-2026 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.process.traits.wrappers;

import org.structr.api.util.Iterables;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.process.entity.BpmnDiDiagram;
import org.structr.process.entity.BpmnDiEdge;
import org.structr.process.entity.BpmnDiShape;
import org.structr.process.traits.definitions.BpmnDiDiagramTraitDefinition;

public class BpmnDiDiagramTraitWrapper extends AbstractNodeTraitWrapper implements BpmnDiDiagram {

	public BpmnDiDiagramTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {

		super(traits, wrappedObject);
	}

	@Override
	public String getDiagramId() {

		return wrappedObject.getProperty(traits.key(BpmnDiDiagramTraitDefinition.DIAGRAM_ID_PROPERTY));
	}

	@Override
	public String getPlaneId() {

		return wrappedObject.getProperty(traits.key(BpmnDiDiagramTraitDefinition.PLANE_ID_PROPERTY));
	}

	@Override
	public String getPlaneElement() {

		return wrappedObject.getProperty(traits.key(BpmnDiDiagramTraitDefinition.PLANE_ELEMENT));
	}

	@Override
	public Iterable<BpmnDiShape> getShapes() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(BpmnDiDiagramTraitDefinition.SHAPES_PROPERTY);

		return Iterables.map(n -> n.as(BpmnDiShape.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<BpmnDiEdge> getEdges() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(BpmnDiDiagramTraitDefinition.EDGES_PROPERTY);

		return Iterables.map(n -> n.as(BpmnDiEdge.class), wrappedObject.getProperty(key));
	}
}
