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
package org.structr.process.traits.rels;

import org.structr.core.entity.Relation;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;
import org.structr.process.ProcessTraits;

/**
 * ComponentConfiguration -[BOUND]-> BpmnElement.
 *
 * <p>Set when a ComponentConfiguration's {@code bindingMode} is
 * {@code processBound}: identifies the BPMN UserTask that owns the contract
 * for the component's dataSource. The render path walks this rel at render
 * time and derives the dataSource type from the target task's owning process's
 * {@code subjectType} property (the subject type is process-level -- one subject
 * per instance), so that process-side changes to the subject type propagate
 * automatically into the rendered widget.</p>
 *
 * <p>Many ComponentConfigurations may bind to the same UserTask (a process
 * step can be rendered by multiple widgets / views); each ComponentConfiguration
 * binds at most one UserTask.</p>
 *
 * <p>No cascading delete or permission propagation: deleting the bound
 * UserTask should not also delete the component config (the UI dev sees a
 * warning instead and decides), and a grant on the UserTask should not leak
 * to every widget that happens to render it.</p>
 */
public class ComponentConfigurationBOUNDBpmnElement extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public ComponentConfigurationBOUNDBpmnElement() {

		super(StructrTraits.COMPONENT_CONFIGURATION_BOUND_BPMN_ELEMENT);
	}

	@Override
	public String getSourceType() {

		return StructrTraits.COMPONENT_CONFIGURATION;
	}

	@Override
	public String getTargetType() {

		return ProcessTraits.BPMN_ELEMENT;
	}

	@Override
	public String getRelationshipType() {

		return "BOUND";
	}

	@Override
	public Relation.Multiplicity getSourceMultiplicity() {

		return Relation.Multiplicity.Many;
	}

	@Override
	public Relation.Multiplicity getTargetMultiplicity() {

		return Relation.Multiplicity.One;
	}

	@Override
	public int getCascadingDeleteFlag() {

		return Relation.NONE;
	}

	@Override
	public int getAutocreationFlag() {

		return Relation.NONE;
	}

	@Override
	public boolean isInternal() {

		return false;
	}
}
