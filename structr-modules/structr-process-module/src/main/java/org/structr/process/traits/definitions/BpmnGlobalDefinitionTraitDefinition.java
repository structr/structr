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

/**
 * Trait definition for BpmnGlobalDefinition -- a top-level definition element
 * in the BPMN definitions scope (sibling of bpmn:process). Covers message,
 * signal, error, escalation, and other global definitions.
 *
 * The definitionType property discriminates the BPMN element kind.
 */
public class BpmnGlobalDefinitionTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DEFINITION_TYPE_PROPERTY  = "definitionType";
	public static final String BPMN_NAME_PROPERTY        = "bpmnName";
	public static final String ERROR_CODE_PROPERTY       = "errorCode";
	public static final String STRUCTURE_REF_PROPERTY    = "structureRef";
	public static final String DEFINITION_PROPERTY       = "definition";

	public BpmnGlobalDefinitionTraitDefinition() {
		super(ProcessTraits.BPMN_GLOBAL_DEFINITION);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String> definitionType = new StringProperty(DEFINITION_TYPE_PROPERTY).indexed();
		final Property<String> bpmnName       = new StringProperty(BPMN_NAME_PROPERTY).indexed();
		final Property<String> errorCode      = new StringProperty(ERROR_CODE_PROPERTY);
		final Property<String> structureRef   = new StringProperty(STRUCTURE_REF_PROPERTY);
		final Property<NodeInterface> def     = new StartNode(traitsInstance, DEFINITION_PROPERTY, ProcessTraits.BPMN_DEFINITIONS_HAS_GLOBAL_DEFINITION);

		return newSet(definitionType, bpmnName, errorCode, structureRef, def);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY, DEFINITION_TYPE_PROPERTY, BPMN_NAME_PROPERTY),
			PropertyView.Ui, newSet(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY, DEFINITION_TYPE_PROPERTY, BPMN_NAME_PROPERTY, ERROR_CODE_PROPERTY, STRUCTURE_REF_PROPERTY, DEFINITION_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
