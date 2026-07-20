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
import org.structr.process.entity.BpmnParticipant;
import org.structr.process.traits.wrappers.BpmnParticipantTraitWrapper;

/**
 * Trait definition for BpmnParticipant -- a {@code <bpmn:participant>} entry
 * inside a collaboration that pools a single process. Carries the
 * participant's display name (used as the pool label) and a reference to
 * the process it represents.
 */
public class BpmnParticipantTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String BPMN_NAME_PROPERTY      = "bpmnName";
	public static final String COLLABORATION_PROPERTY  = "collaboration";
	public static final String PROCESS_PROPERTY        = "process";

	public BpmnParticipantTraitDefinition() {
		super(ProcessTraits.BPMN_PARTICIPANT);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			BpmnParticipant.class, (traits, node) -> new BpmnParticipantTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String>        bpmnName      = new StringProperty(BPMN_NAME_PROPERTY).indexed();
		final Property<NodeInterface> collaboration = new StartNode(traitsInstance, COLLABORATION_PROPERTY, ProcessTraits.BPMN_COLLABORATION_HAS_PARTICIPANT);
		final Property<NodeInterface> process       = new EndNode(traitsInstance,   PROCESS_PROPERTY,       ProcessTraits.BPMN_PARTICIPANT_OF_PROCESS);

		return newSet(bpmnName, collaboration, process);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(
				BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY,
				BPMN_NAME_PROPERTY, PROCESS_PROPERTY),
			PropertyView.Ui, newSet(
				BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY,
				BPMN_NAME_PROPERTY, COLLABORATION_PROPERTY, PROCESS_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
