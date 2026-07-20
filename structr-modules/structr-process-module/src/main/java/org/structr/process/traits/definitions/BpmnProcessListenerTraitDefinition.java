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
import org.structr.process.entity.BpmnProcessListener;
import org.structr.process.traits.wrappers.BpmnProcessListenerTraitWrapper;

/**
 * Per-process BPMN process listener declaration. Parsed from the {@code <bpmn:process>}
 * element's extensionElements (Structr namespace, with Camunda interop on import):
 *
 * <pre>{@code
 * <bpmn:process id="LeaveRequest" name="Leave Request">
 *   <bpmn:extensionElements>
 *     <structr:processListener event="started"   phase="after" method="leaveRequest_afterStarted" />
 *     <structr:processListener event="completed" phase="on"    method="leaveRequest_onCompleted" />
 *   </bpmn:extensionElements>
 *   ...
 * </bpmn:process>
 * }</pre>
 *
 * <p>One listener per (process, event, phase). The {@code method} relationship points
 * directly at the SchemaMethod the engine invokes with the {@code ProcessInstance} bound
 * as {@code this}: no name resolution at dispatch time. The {@code phase} determines
 * transaction timing, mirroring Structr's on/after lifecycle split:
 * <ul>
 *   <li>{@code on} -- runs pre-commit; a thrown exception rolls back the transition.</li>
 *   <li>{@code after} (default) -- runs post-commit; side-effects only fire if the
 *       transition actually persisted.</li>
 * </ul></p>
 *
 * <p>Lifecycle events:
 * {@code created}, {@code started}, {@code subjectAttached}, {@code completed},
 * {@code terminated}, {@code suspended}, {@code resumed}.</p>
 */
public class BpmnProcessListenerTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String EVENT_PROPERTY    = "event";
	public static final String PHASE_PROPERTY    = "phase";
	public static final String METHOD_PROPERTY   = "method";
	public static final String PROCESS_PROPERTY  = "process";

	// Event constants: match the lifecycle event names exactly.
	public static final String EVENT_CREATED          = "created";
	public static final String EVENT_STARTED          = "started";
	public static final String EVENT_SUBJECT_ATTACHED = "subjectAttached";
	public static final String EVENT_COMPLETED        = "completed";
	public static final String EVENT_TERMINATED       = "terminated";
	public static final String EVENT_SUSPENDED        = "suspended";
	public static final String EVENT_RESUMED          = "resumed";

	// Phase constants -- transaction timing, default 'after'.
	public static final String PHASE_ON    = "on";
	public static final String PHASE_AFTER = "after";

	public BpmnProcessListenerTraitDefinition() {
		super(ProcessTraits.BPMN_PROCESS_LISTENER);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			BpmnProcessListener.class, (traits, node) -> new BpmnProcessListenerTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String> event              = new EnumProperty(EVENT_PROPERTY,
			Set.of(EVENT_CREATED, EVENT_STARTED, EVENT_SUBJECT_ATTACHED, EVENT_COMPLETED,
				   EVENT_TERMINATED, EVENT_SUSPENDED, EVENT_RESUMED)).indexed();
		final Property<String> phase              = new EnumProperty(PHASE_PROPERTY, Set.of(PHASE_ON, PHASE_AFTER)).defaultValue(PHASE_AFTER).indexed();
		final Property<NodeInterface> method      = new EndNode(traitsInstance, METHOD_PROPERTY, ProcessTraits.BPMN_PROCESS_LISTENER_CALLS_METHOD);
		final Property<NodeInterface> process     = new StartNode(traitsInstance, PROCESS_PROPERTY, ProcessTraits.BPMN_PROCESS_HAS_PROCESS_LISTENER);

		return newSet(event, phase, method, process);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(EVENT_PROPERTY, PHASE_PROPERTY, METHOD_PROPERTY),
			PropertyView.Ui,     newSet(EVENT_PROPERTY, PHASE_PROPERTY, METHOD_PROPERTY, BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY, PROCESS_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
