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

import java.util.Date;
import java.util.Map;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.process.entity.ProcessTimer;
import org.structr.process.traits.wrappers.ProcessTimerTraitWrapper;
import java.util.Set;

/**
 * Persistent timer record for BPMN timer events. The Process Timer Service
 * polls for timers whose {@code fireAt} has elapsed and dispatches them via
 * {@code ProcessEngine.fireTimer(...)}.
 *
 * <h3>Timer flavours</h3>
 * <ul>
 *   <li><b>intermediateTimer</b> -- a token has reached an
 *       {@code <intermediateCatchEvent><timerEventDefinition>} and is waiting.
 *       Firing advances the token past the event.</li>
 *   <li><b>boundaryTimer</b> -- attached to an activity (typically a userTask).
 *       Firing either cancels the parent activity (interrupting) or spawns a
 *       parallel token via the boundary event's outgoing flow (non-interrupting).
 *       Cancelled when the parent activity completes / cancels.</li>
 *   <li><b>timerStart</b> -- recurring class-level trigger. Firing creates a new
 *       ProcessInstance from the definition. (Implemented in a follow-up round.)</li>
 * </ul>
 *
 * <h3>Status lifecycle</h3>
 * <pre>
 *   pending  -- waiting for fireAt
 *   fired    -- terminal: timer has fired, listener handled it
 *   cancelled-- terminal: cancelled before firing (e.g. parent activity completed)
 *   error    -- terminal: firing threw an exception (logged)
 * </pre>
 */
public class ProcessTimerTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String FIRE_AT_PROPERTY            = "fireAt";
	public static final String TIMER_TYPE_PROPERTY         = "timerType";
	public static final String TIMER_EXPRESSION_PROPERTY   = "timerExpression";
	public static final String STATUS_PROPERTY             = "status";
	public static final String CANCEL_ACTIVITY_PROPERTY    = "cancelActivity";
	public static final String FIRED_AT_PROPERTY           = "firedAt";
	public static final String ERROR_MESSAGE_PROPERTY      = "errorMessage";

	// Relationship properties
	public static final String INSTANCE_PROPERTY           = "instance";
	public static final String TOKEN_PROPERTY              = "token";
	public static final String ELEMENT_PROPERTY            = "element";

	// timerType constants
	public static final String TIMER_INTERMEDIATE = "intermediateTimer";
	public static final String TIMER_BOUNDARY     = "boundaryTimer";
	public static final String TIMER_START        = "timerStart";

	// status constants
	public static final String STATUS_PENDING   = "pending";
	public static final String STATUS_FIRED     = "fired";
	public static final String STATUS_CANCELLED = "cancelled";
	public static final String STATUS_ERROR     = "error";

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			ProcessTimer.class, (traits, node) -> new ProcessTimerTraitWrapper(traits, node)
		);
	}

	public ProcessTimerTraitDefinition() {
		super(ProcessTraits.PROCESS_TIMER);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<Date> fireAt              = new DateProperty(FIRE_AT_PROPERTY).indexed();
		final Property<String> timerType         = new EnumProperty(TIMER_TYPE_PROPERTY,
			Set.of(TIMER_INTERMEDIATE, TIMER_BOUNDARY, TIMER_START)).indexed();
		final Property<String> timerExpression   = new StringProperty(TIMER_EXPRESSION_PROPERTY);
		final Property<String> status            = new EnumProperty(STATUS_PROPERTY,
			Set.of(STATUS_PENDING, STATUS_FIRED, STATUS_CANCELLED, STATUS_ERROR)).indexed();
		final Property<Boolean> cancelActivity   = new BooleanProperty(CANCEL_ACTIVITY_PROPERTY).defaultValue(true);
		final Property<Date> firedAt             = new DateProperty(FIRED_AT_PROPERTY);
		final Property<String> errorMessage      = new StringProperty(ERROR_MESSAGE_PROPERTY);

		final Property<NodeInterface> instance   = new EndNode(traitsInstance, INSTANCE_PROPERTY, ProcessTraits.PROCESS_TIMER_OF_INSTANCE);
		final Property<NodeInterface> token      = new EndNode(traitsInstance, TOKEN_PROPERTY, ProcessTraits.PROCESS_TIMER_FOR_TOKEN);
		final Property<NodeInterface> element    = new EndNode(traitsInstance, ELEMENT_PROPERTY, ProcessTraits.PROCESS_TIMER_AT_ELEMENT);

		return newSet(fireAt, timerType, timerExpression, status, cancelActivity, firedAt, errorMessage,
			instance, token, element);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(FIRE_AT_PROPERTY, TIMER_TYPE_PROPERTY, STATUS_PROPERTY, ELEMENT_PROPERTY, INSTANCE_PROPERTY),
			PropertyView.Ui,     newSet(FIRE_AT_PROPERTY, TIMER_TYPE_PROPERTY, TIMER_EXPRESSION_PROPERTY, STATUS_PROPERTY,
				CANCEL_ACTIVITY_PROPERTY, FIRED_AT_PROPERTY, ERROR_MESSAGE_PROPERTY,
				INSTANCE_PROPERTY, TOKEN_PROPERTY, ELEMENT_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
