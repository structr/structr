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
package org.structr.process.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.process.ProcessTraits;

import java.util.Map;
import java.util.Set;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.process.entity.BpmnDiDiagram;
import org.structr.process.traits.wrappers.BpmnDiDiagramTraitWrapper;

/**
 * Trait definition for BpmnDiDiagram -- the BPMN DI diagram container.
 * Maps to &lt;bpmndi:BPMNDiagram&gt; and its &lt;bpmndi:BPMNPlane&gt; child.
 */
public class BpmnDiDiagramTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DIAGRAM_ID_PROPERTY  = "diagramId";
	public static final String PLANE_ID_PROPERTY    = "planeId";
	public static final String PLANE_ELEMENT        = "planeElement";
	public static final String DEFINITION_PROPERTY  = "definition";
	public static final String SHAPES_PROPERTY      = "shapes";
	public static final String EDGES_PROPERTY       = "edges";

	public BpmnDiDiagramTraitDefinition() {

		super(ProcessTraits.BPMN_DI_DIAGRAM);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(BpmnDiDiagram.class, (traits, node) -> new BpmnDiDiagramTraitWrapper(traits, node));
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String> diagramId                       = new StringProperty(DIAGRAM_ID_PROPERTY);
		final Property<String> planeId                         = new StringProperty(PLANE_ID_PROPERTY);
		final Property<String> planeElement                    = new StringProperty(PLANE_ELEMENT);
		final Property<NodeInterface> def                      = new StartNode(traitsInstance, DEFINITION_PROPERTY, ProcessTraits.BPMN_DEFINITIONS_HAS_DIAGRAM);
		final Property<Iterable<NodeInterface>> shapes         = new EndNodes(traitsInstance, SHAPES_PROPERTY, ProcessTraits.BPMN_DI_DIAGRAM_HAS_SHAPE);
		final Property<Iterable<NodeInterface>> edges          = new EndNodes(traitsInstance, EDGES_PROPERTY, ProcessTraits.BPMN_DI_DIAGRAM_HAS_EDGE);

		return newSet(diagramId, planeId, planeElement, def, shapes, edges);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(DIAGRAM_ID_PROPERTY, PLANE_ID_PROPERTY, SHAPES_PROPERTY, EDGES_PROPERTY),
			PropertyView.Ui, newSet(DIAGRAM_ID_PROPERTY, PLANE_ID_PROPERTY, PLANE_ELEMENT, DEFINITION_PROPERTY, SHAPES_PROPERTY, EDGES_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {

		return null;
	}
}
