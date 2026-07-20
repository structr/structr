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
import org.structr.process.entity.BpmnCollaboration;
import org.structr.process.traits.wrappers.BpmnCollaborationTraitWrapper;

/**
 * Trait definition for BpmnCollaboration -- the {@code <bpmn:collaboration>}
 * container that groups multiple participants and their cross-process
 * message flows. Optional: single-process BPMN files have no collaboration.
 */
public class BpmnCollaborationTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DEFINITION_PROPERTY    = "definition";
	public static final String PARTICIPANTS_PROPERTY  = "participants";
	public static final String MESSAGE_FLOWS_PROPERTY = "messageFlows";

	public BpmnCollaborationTraitDefinition() {
		super(ProcessTraits.BPMN_COLLABORATION);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			BpmnCollaboration.class, (traits, node) -> new BpmnCollaborationTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<NodeInterface>           definition    = new StartNode(traitsInstance, DEFINITION_PROPERTY,    ProcessTraits.BPMN_DEFINITIONS_HAS_COLLABORATION);
		final Property<Iterable<NodeInterface>> participants  = new EndNodes(traitsInstance,  PARTICIPANTS_PROPERTY,  ProcessTraits.BPMN_COLLABORATION_HAS_PARTICIPANT);
		final Property<Iterable<NodeInterface>> messageFlows  = new EndNodes(traitsInstance,  MESSAGE_FLOWS_PROPERTY, ProcessTraits.BPMN_COLLABORATION_HAS_MESSAGE_FLOW);

		return newSet(definition, participants, messageFlows);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(
				BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY,
				PARTICIPANTS_PROPERTY, MESSAGE_FLOWS_PROPERTY),
			PropertyView.Ui, newSet(
				BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY,
				DEFINITION_PROPERTY, PARTICIPANTS_PROPERTY, MESSAGE_FLOWS_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
