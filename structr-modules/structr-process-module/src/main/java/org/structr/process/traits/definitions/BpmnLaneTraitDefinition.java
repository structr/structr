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
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.process.ProcessTraits;

import java.util.Map;
import java.util.Set;

/**
 * Trait definition for BpmnLane: a {@code <bpmn:lane>} entry inside a
 * process's {@code <bpmn:laneSet>}. Lanes subdivide a participant pool
 * into named horizontal bands and each lane references the flow elements
 * that belong inside it via {@code <bpmn:flowNodeRef>} entries.
 *
 * <p>Lanes are purely a layout / grouping construct: they don't take part
 * in flow execution. The engine ignores them; the editor reads them to
 * paint pool subdivisions.</p>
 */
public class BpmnLaneTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String BPMN_NAME_PROPERTY       = "bpmnName";
	public static final String PROCESS_PROPERTY         = "process";
	public static final String FLOW_NODE_REFS_PROPERTY  = "flowNodeRefs";

	public BpmnLaneTraitDefinition() {
		super(ProcessTraits.BPMN_LANE);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String>                   bpmnName     = new StringProperty(BPMN_NAME_PROPERTY).indexed();
		final Property<NodeInterface>            process      = new StartNode(traitsInstance, PROCESS_PROPERTY,        ProcessTraits.BPMN_PROCESS_HAS_LANE);
		final Property<Iterable<NodeInterface>>  flowNodeRefs = new EndNodes(traitsInstance,  FLOW_NODE_REFS_PROPERTY, ProcessTraits.BPMN_LANE_HAS_FLOW_NODE);

		return newSet(bpmnName, process, flowNodeRefs);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(
				BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY,
				BPMN_NAME_PROPERTY, FLOW_NODE_REFS_PROPERTY),
			PropertyView.Ui, newSet(
				BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY,
				BPMN_NAME_PROPERTY, PROCESS_PROPERTY, FLOW_NODE_REFS_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
