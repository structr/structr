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
 * Per-userTask BPMN task listener declaration. Parsed from the BPMN element's
 * extensionElements (Structr namespace, with Camunda interop on import):
 *
 * <pre>{@code
 * <bpmn:userTask id="...">
 *   <bpmn:extensionElements>
 *     <structr:taskListener event="assigned" method="notifyAssignee" />
 *     <structr:taskListener event="completed" method="archiveDecision" sync="true" />
 *   </bpmn:extensionElements>
 * </bpmn:userTask>
 * }</pre>
 *
 * <p>Each listener fires when its event is emitted by the engine. The
 * {@code method} attribute names a schema method (typically on the
 * {@code TaskInstance} type) that the engine invokes with the task as
 * {@code this}. {@code sync="true"} makes listener exceptions abort the
 * engine transaction; default ({@code sync="false"}) catches and logs
 * errors so the state transition stands regardless.</p>
 *
 * <p>Lifecycle events:
 * {@code created}, {@code assigned}, {@code claimed}, {@code available},
 * {@code declined}, {@code completed}, {@code cancelled}.</p>
 */
public class BpmnTaskListenerTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String EVENT_PROPERTY    = "event";
	public static final String METHOD_PROPERTY   = "method";
	public static final String SYNC_PROPERTY     = "sync";
	public static final String ELEMENT_PROPERTY  = "element";

	// Event constants -- match the lifecycle event names exactly.
	public static final String EVENT_CREATED   = "created";
	public static final String EVENT_ASSIGNED  = "assigned";
	public static final String EVENT_CLAIMED   = "claimed";
	public static final String EVENT_AVAILABLE = "available";
	public static final String EVENT_DECLINED  = "declined";
	public static final String EVENT_COMPLETED = "completed";
	public static final String EVENT_CANCELLED = "cancelled";

	public BpmnTaskListenerTraitDefinition() {
		super(ProcessTraits.BPMN_TASK_LISTENER);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String> event              = new EnumProperty(EVENT_PROPERTY,
			Set.of(EVENT_CREATED, EVENT_ASSIGNED, EVENT_CLAIMED, EVENT_AVAILABLE,
				   EVENT_DECLINED, EVENT_COMPLETED, EVENT_CANCELLED)).indexed();
		final Property<String> method             = new StringProperty(METHOD_PROPERTY);
		final Property<Boolean> sync              = new BooleanProperty(SYNC_PROPERTY).defaultValue(false);
		final Property<NodeInterface> element     = new StartNode(traitsInstance, ELEMENT_PROPERTY, ProcessTraits.BPMN_ELEMENT_HAS_TASK_LISTENER);

		return newSet(event, method, sync, element);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(EVENT_PROPERTY, METHOD_PROPERTY, SYNC_PROPERTY),
			PropertyView.Ui,     newSet(EVENT_PROPERTY, METHOD_PROPERTY, SYNC_PROPERTY, BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY, ELEMENT_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
