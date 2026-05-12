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
 * Trait definition for BpmnMessageFlow -- a {@code <bpmn:messageFlow>} entry
 * inside a collaboration. Connects a source element in one participant's
 * process to a target element in another participant's process. Engine
 * runtime semantics are NOT yet supported; this is currently
 * import / render / export plumbing only.
 *
 * <p>{@code sourceRefId} and {@code targetRefId} hold the BPMN ids (string)
 * of the referenced elements. The actual graph edges (sourceElement,
 * targetElement) are resolved after import once all elements are in place,
 * mirroring the BpmnSequenceFlow pattern.</p>
 */
public class BpmnMessageFlowTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String BPMN_NAME_PROPERTY        = "bpmnName";
	public static final String COLLABORATION_PROPERTY    = "collaboration";
	public static final String SOURCE_REF_ID_PROPERTY    = "sourceRefId";
	public static final String TARGET_REF_ID_PROPERTY    = "targetRefId";
	public static final String SOURCE_ELEMENT_PROPERTY   = "sourceElement";
	public static final String TARGET_ELEMENT_PROPERTY   = "targetElement";

	public BpmnMessageFlowTraitDefinition() {
		super(ProcessTraits.BPMN_MESSAGE_FLOW);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String>        bpmnName       = new StringProperty(BPMN_NAME_PROPERTY).indexed();
		final Property<String>        sourceRefId    = new StringProperty(SOURCE_REF_ID_PROPERTY).indexed();
		final Property<String>        targetRefId    = new StringProperty(TARGET_REF_ID_PROPERTY).indexed();
		final Property<NodeInterface> collaboration  = new StartNode(traitsInstance, COLLABORATION_PROPERTY,  ProcessTraits.BPMN_COLLABORATION_HAS_MESSAGE_FLOW);
		final Property<NodeInterface> sourceElement  = new EndNode(traitsInstance,   SOURCE_ELEMENT_PROPERTY, ProcessTraits.BPMN_MESSAGE_FLOW_FROM);
		final Property<NodeInterface> targetElement  = new EndNode(traitsInstance,   TARGET_ELEMENT_PROPERTY, ProcessTraits.BPMN_MESSAGE_FLOW_TO);

		return newSet(bpmnName, sourceRefId, targetRefId, collaboration, sourceElement, targetElement);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(
				BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY,
				BPMN_NAME_PROPERTY, SOURCE_REF_ID_PROPERTY, TARGET_REF_ID_PROPERTY,
				SOURCE_ELEMENT_PROPERTY, TARGET_ELEMENT_PROPERTY),
			PropertyView.Ui, newSet(
				BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY,
				BPMN_NAME_PROPERTY, COLLABORATION_PROPERTY, SOURCE_REF_ID_PROPERTY, TARGET_REF_ID_PROPERTY,
				SOURCE_ELEMENT_PROPERTY, TARGET_ELEMENT_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
