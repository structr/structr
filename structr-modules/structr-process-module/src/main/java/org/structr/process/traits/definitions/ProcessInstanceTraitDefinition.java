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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.process.ProcessTraits;
import org.structr.process.engine.ProcessEngine;
import org.structr.schema.action.ActionContext;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * A running instance of a BPMN process definition. Tracks the current execution
 * state through tokens, task instances, and process data.
 *
 * Status lifecycle: running -> completed | suspended | terminated | error
 */
public class ProcessInstanceTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String STATUS_PROPERTY       = "status";
	public static final String START_TIME_PROPERTY   = "startTime";
	public static final String END_TIME_PROPERTY     = "endTime";
	public static final String DEFINITION_PROPERTY   = "definition";
	public static final String TOKENS_PROPERTY            = "tokens";
	public static final String TASKS_PROPERTY             = "tasks";
	public static final String PARAMETER_VALUES_PROPERTY  = "parameterValues";

	// Status constants
	public static final String STATUS_RUNNING     = "running";
	public static final String STATUS_COMPLETED   = "completed";
	public static final String STATUS_SUSPENDED   = "suspended";
	public static final String STATUS_TERMINATED  = "terminated";
	public static final String STATUS_ERROR       = "error";

	public ProcessInstanceTraitDefinition() {
		super(ProcessTraits.PROCESS_INSTANCE);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String> status       = new StringProperty(STATUS_PROPERTY).indexed();
		final Property<Date> startTime      = new DateProperty(START_TIME_PROPERTY);
		final Property<Date> endTime        = new DateProperty(END_TIME_PROPERTY);
		final Property<NodeInterface> def   = new EndNode(traitsInstance, DEFINITION_PROPERTY, ProcessTraits.PROCESS_INSTANCE_OF_DEFINITION);
		final Property<Iterable<NodeInterface>> tokens          = new EndNodes(traitsInstance, TOKENS_PROPERTY, ProcessTraits.PROCESS_INSTANCE_HAS_TOKEN);
		final Property<Iterable<NodeInterface>> tasks            = new StartNodes(traitsInstance, TASKS_PROPERTY, ProcessTraits.TASK_INSTANCE_OF_PROCESS);
		final Property<Iterable<NodeInterface>> parameterValues  = new EndNodes(traitsInstance, PARAMETER_VALUES_PROPERTY, ProcessTraits.PROCESS_INSTANCE_HAS_PARAMETER_VALUE);

		return newSet(status, startTime, endTime, def, tokens, tasks, parameterValues);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(STATUS_PROPERTY, START_TIME_PROPERTY, END_TIME_PROPERTY, DEFINITION_PROPERTY),
			PropertyView.Ui, newSet(STATUS_PROPERTY, START_TIME_PROPERTY, END_TIME_PROPERTY, DEFINITION_PROPERTY, TOKENS_PROPERTY, TASKS_PROPERTY, PARAMETER_VALUES_PROPERTY)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return Set.of(

			new JavaMethod("signalEvent", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {
					final SecurityContext securityContext = actionContext.getSecurityContext();
					final ProcessEngine engine = new ProcessEngine(securityContext);
					final java.util.Map<String, Object> params = arguments.toMap();
					final String eventBpmnId = (String) params.remove("eventBpmnId");
					if (eventBpmnId == null || eventBpmnId.isEmpty()) {
						throw new FrameworkException(422, "Missing required parameter: eventBpmnId");
					}
					engine.signalEvent((NodeInterface) entity, eventBpmnId, params.isEmpty() ? null : params);
					return entity;
				}

				@Override
				public String getDescription() {
					return "Signals an intermediate catch event by bpmnId, resuming the waiting token. Pass eventBpmnId as a required parameter.";
				}
			}
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
