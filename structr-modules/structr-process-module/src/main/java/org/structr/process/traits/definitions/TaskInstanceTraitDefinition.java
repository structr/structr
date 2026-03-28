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
import org.structr.process.engine.ProcessEngine;
import org.structr.process.ProcessTraits;
import org.structr.schema.action.EvaluationHints;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * A task instance is created when a token arrives at a userTask element.
 * It represents a work item that must be completed by a user before the
 * process can advance.
 *
 * Status lifecycle: created -> assigned -> completed | cancelled
 *
 * Exposes a complete method callable via REST:
 *   POST /structr/rest/TaskInstance/{id}/complete
 */
public class TaskInstanceTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String STATUS_PROPERTY           = "status";
	public static final String ASSIGNEE_PROPERTY         = "assignee";
	public static final String CREATED_TIME_PROPERTY     = "createdTime";
	public static final String COMPLETED_TIME_PROPERTY   = "completedTime";
	public static final String PROCESS_INSTANCE_PROPERTY = "processInstance";
	public static final String DEFINED_BY_PROPERTY       = "definedBy";

	// Status constants
	public static final String STATUS_CREATED   = "created";
	public static final String STATUS_ASSIGNED  = "assigned";
	public static final String STATUS_COMPLETED = "completed";
	public static final String STATUS_CANCELLED = "cancelled";

	public TaskInstanceTraitDefinition() {
		super(ProcessTraits.TASK_INSTANCE);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String> status              = new StringProperty(STATUS_PROPERTY).indexed();
		final Property<String> assignee            = new StringProperty(ASSIGNEE_PROPERTY).indexed();
		final Property<Date> createdTime           = new DateProperty(CREATED_TIME_PROPERTY);
		final Property<Date> completedTime         = new DateProperty(COMPLETED_TIME_PROPERTY);
		final Property<NodeInterface> processInst  = new EndNode(traitsInstance, PROCESS_INSTANCE_PROPERTY, ProcessTraits.TASK_INSTANCE_OF_PROCESS);
		final Property<NodeInterface> definedBy    = new EndNode(traitsInstance, DEFINED_BY_PROPERTY, ProcessTraits.TASK_INSTANCE_DEFINED_BY);

		return newSet(status, assignee, createdTime, completedTime, processInst, definedBy);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(STATUS_PROPERTY, ASSIGNEE_PROPERTY, DEFINED_BY_PROPERTY, PROCESS_INSTANCE_PROPERTY),
			PropertyView.Ui, newSet(STATUS_PROPERTY, ASSIGNEE_PROPERTY, CREATED_TIME_PROPERTY, COMPLETED_TIME_PROPERTY, PROCESS_INSTANCE_PROPERTY, DEFINED_BY_PROPERTY)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return Set.of(

			new JavaMethod("complete", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
					final ProcessEngine engine = new ProcessEngine(securityContext);
					// Pass the arguments map as process parameter values
					final java.util.Map<String, Object> params = arguments.toMap();
					engine.completeTask((NodeInterface) entity, params.isEmpty() ? null : params);
					return entity;
				}

				@Override
				public String getDescription() {
					return "Completes this user task and advances the process to the next step.";
				}
			}
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
