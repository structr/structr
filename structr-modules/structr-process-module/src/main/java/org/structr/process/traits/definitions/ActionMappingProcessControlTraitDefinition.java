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
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.web.traits.definitions.ActionMappingTraitDefinition;

import java.util.Set;

/**
 * Sibling trait that adds process-control properties to the {@code ActionMapping}
 * type. Lives in the process module so the EndNode declarations resolve their
 * relationship targets ({@code ActionMappingCONTROLSBpmnDefinitions},
 * {@code ActionMappingTARGETSBpmnElement}) from a module that has already loaded
 * those types.
 *
 * <p>Why a sibling trait instead of inlining on {@code ActionMappingTraitDefinition}
 * (in structr-base): the EndNode constructor calls
 * {@code traitsInstance.getTraits(relationshipTypeName)} eagerly. If the
 * relationship type has not been registered yet (as is the case during
 * structr-base load, before the process module loads), the call returns null
 * and the trait registration fails, cascading to break the entire registry.
 * Splitting the property declarations into a process-module trait ensures
 * relationship-type registration precedes property declaration.</p>
 *
 * <p>The properties this trait adds:
 * <ul>
 *   <li>{@code controlsProcess} (EndNode -&gt; BpmnDefinitions): the process
 *       definition the action operates on. Required for the {@code start}
 *       operation.</li>
 *   <li>{@code targetsElement} (EndNode -&gt; BpmnElement): the BPMN element the
 *       action targets. Required for the {@code signal} operation; optional
 *       static binding for task operations.</li>
 *   <li>{@code processOperation} (String): closed-vocabulary enum naming the
 *       engine operation (start, claim, complete, signal, terminate, ...).</li>
 * </ul>
 *
 * <p>Constants are shared with {@link ActionMappingTraitDefinition}: this trait
 * does not own the property names, only their declarations on the process side.</p>
 */
public class ActionMappingProcessControlTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String TRAIT_NAME = "ActionMappingProcessControl";

	public ActionMappingProcessControlTraitDefinition() {
		super(TRAIT_NAME);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<NodeInterface> controlsProcess = new EndNode(traitsInstance,
			ActionMappingTraitDefinition.CONTROLS_PROCESS_PROPERTY,
			StructrTraits.ACTION_MAPPING_CONTROLS_BPMN_DEFINITIONS);

		final Property<NodeInterface> targetsElement = new EndNode(traitsInstance,
			ActionMappingTraitDefinition.TARGETS_ELEMENT_PROPERTY,
			StructrTraits.ACTION_MAPPING_TARGETS_BPMN_ELEMENT);

		final Property<String> processOperation = new StringProperty(
			ActionMappingTraitDefinition.PROCESS_OPERATION_PROPERTY)
				.description("Process operation: start | claim | release | decline | delegate | complete | cancel | makeAvailable | assignTask | signal | terminate | suspend | resume");

		// Denormalized backups for the rels above. Editor writes them on save
		// alongside controlsProcess/targetsElement; importer refreshes them on
		// re-import rewire.
		final Property<String> controlsProcessId = new StringProperty(
			ActionMappingTraitDefinition.CONTROLS_PROCESS_ID_PROPERTY).indexed();

		final Property<String> targetsElementBpmnId = new StringProperty(
			ActionMappingTraitDefinition.TARGETS_ELEMENT_BPMN_ID_PROPERTY).indexed();

		// Dynamic alternative to the static controlsProcess relationship.
		// Structr scripting expression (e.g. ${current.id}) that resolves to
		// a BpmnDefinitions UUID at page render time. When set and resolvable,
		// the dispatcher uses it as the start-process target in place of the
		// static rel.
		final Property<String> controlsProcessIdExpression = new StringProperty(
			ActionMappingTraitDefinition.CONTROLS_PROCESS_ID_EXPRESSION_PROPERTY)
				.description("Structr scripting expression that resolves to a BpmnDefinitions UUID at render time. Alternative to selecting a Process manually; useful on process-catalog pages where the target definition is data-driven (e.g. ${current.id}). Only honoured for the 'start' operation.");

		return newSet(controlsProcess, targetsElement, processOperation,
			controlsProcessId, targetsElementBpmnId, controlsProcessIdExpression);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
