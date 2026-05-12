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

import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.process.ProcessTraits;

import java.util.Set;

/**
 * Sibling trait composed into {@code SchemaMethod} at process-module load time.
 * Declares the inverse {@code StartNode} properties for the two BPMN-side
 * "owns method" relationships: {@code BpmnProcess HAS_METHOD SchemaMethod}
 * and {@code BpmnElement HAS_METHOD SchemaMethod}.
 *
 * <p>Why this is required: {@code OneToMany.ensureCardinality()} (called every
 * time a relationship of this type is created) does
 * {@code targetNode.getIncomingRelationshipAsSuperUser(relType)} to check whether
 * the target already has a source for that relationship. That call resolves
 * the relation's {@code getSource()} via the target type's trait definition,
 * which requires a {@code StartNode} property pointing at this rel type. Without
 * it, the cardinality check throws "Invalid schema setup: missing StartNode(s)
 * property" and any attempt to attach a method via {@code element.methods = […]}
 * or {@code process.methods = […]} fails.</p>
 *
 * <p>Why this trait lives in process-module rather than on
 * {@code SchemaMethodTraitDefinition} in structr-base: the {@code StartNode}
 * constructor eagerly resolves its rel type via {@code traits.getRelation()},
 * and the BPMN_*_HAS_METHOD rel types are registered later, in
 * {@link org.structr.process.ProcessModule}. Putting the StartNodes here means
 * by the time this trait's properties are constructed (at process-module load),
 * the rel types are already registered.</p>
 *
 * <p>Composed into {@code SCHEMA_METHOD} via re-registration in
 * {@link org.structr.process.ProcessModule}, mirroring how
 * {@code ActionMappingProcessControlTraitDefinition} extends {@code ACTION_MAPPING}.</p>
 */
public class BpmnSchemaMethodTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String TRAIT_NAME = "BpmnSchemaMethod";

	public static final String BPMN_PROCESS_PROPERTY = "bpmnProcess";
	public static final String BPMN_ELEMENT_PROPERTY = "bpmnElement";

	public BpmnSchemaMethodTraitDefinition() {
		super(TRAIT_NAME);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<NodeInterface> bpmnProcess = new StartNode(traitsInstance,
			BPMN_PROCESS_PROPERTY,
			ProcessTraits.BPMN_PROCESS_HAS_METHOD);

		final Property<NodeInterface> bpmnElement = new StartNode(traitsInstance,
			BPMN_ELEMENT_PROPERTY,
			ProcessTraits.BPMN_ELEMENT_HAS_METHOD);

		return newSet(bpmnProcess, bpmnElement);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
