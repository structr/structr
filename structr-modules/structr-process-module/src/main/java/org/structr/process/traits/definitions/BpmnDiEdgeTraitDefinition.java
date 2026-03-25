/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.process.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;

import java.util.Map;
import java.util.Set;

/**
 * Trait definition for BpmnDiEdge -- DI edge data for a BPMN sequence flow.
 * Maps to &lt;bpmndi:BPMNEdge&gt; in BPMN 2.0.2 XML.
 * Stores waypoints as a JSON array and optional label bounds.
 */
public class BpmnDiEdgeTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String EDGE_ID_PROPERTY          = "edgeId";
	public static final String BPMN_ELEMENT_REF_PROPERTY = "bpmnElementRef";
	public static final String WAYPOINTS_PROPERTY        = "waypoints";
	public static final String LABEL_BOUNDS_PROPERTY     = "labelBounds";
	public static final String DI_ATTRIBUTES_PROPERTY    = "diAttributes";
	public static final String DIAGRAM_PROPERTY          = "diagram";
	public static final String REFERENCES_FLOW_PROPERTY  = "referencesFlow";

	public BpmnDiEdgeTraitDefinition() {
		super(StructrTraits.BPMN_DI_EDGE);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String> edgeId          = new StringProperty(EDGE_ID_PROPERTY);
		final Property<String> bpmnElementRef  = new StringProperty(BPMN_ELEMENT_REF_PROPERTY).indexed();
		final Property<String> waypoints       = new StringProperty(WAYPOINTS_PROPERTY);
		final Property<String> labelBounds     = new StringProperty(LABEL_BOUNDS_PROPERTY);
		final Property<String> diAttributes    = new StringProperty(DI_ATTRIBUTES_PROPERTY);
		final Property<NodeInterface> diagram  = new StartNode(traitsInstance, DIAGRAM_PROPERTY, StructrTraits.BPMN_DI_DIAGRAM_HAS_EDGE);
		final Property<NodeInterface> refFlow  = new EndNode(traitsInstance, REFERENCES_FLOW_PROPERTY, StructrTraits.BPMN_DI_EDGE_REFERENCES_FLOW);

		return newSet(edgeId, bpmnElementRef, waypoints, labelBounds, diAttributes, diagram, refFlow);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(EDGE_ID_PROPERTY, BPMN_ELEMENT_REF_PROPERTY, WAYPOINTS_PROPERTY),
			PropertyView.Ui, newSet(EDGE_ID_PROPERTY, BPMN_ELEMENT_REF_PROPERTY, WAYPOINTS_PROPERTY, LABEL_BOUNDS_PROPERTY, DI_ATTRIBUTES_PROPERTY, DIAGRAM_PROPERTY, REFERENCES_FLOW_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
