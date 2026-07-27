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
package org.structr.process.engine;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;

import java.util.Map;

/**
 * Runtime process context exposed as $.process in scripts and expressions.
 *
 * Provides access to:
 *   $.process.instance   -- the ProcessInstance node
 *   $.process.element    -- the current BpmnElement node
 *   $.process.definition -- the BpmnDefinitions node
 *   $.process.<name>     -- process parameter values (read AND write)
 *
 * Writing ($.process.x = y, or a transpiled execution.setVariable(...)) persists
 * a process variable via the supplied {@link VariableSink} and updates the live
 * in-memory view so later reads in the same evaluation see it.
 *
 * This object is set as a constant on the ActionContext before script
 * evaluation and removed afterward.
 */
public class ProcessContext implements ProxyObject {

	/** Persists a process variable write (name/value) -- backed by the engine's storeParameterValues. */
	@FunctionalInterface
	public interface VariableSink {
		void set(String name, Object value) throws FrameworkException;
	}

	private final NodeInterface instance;
	private final NodeInterface element;
	private final NodeInterface definition;
	private final Map<String, Object> parameterValues;
	private final VariableSink sink;

	// Optional activity-local variable scope (Camunda inputOutput). When present:
	//   - reads resolve local-first, then process variables (locals shadow, not overwrite);
	//   - writes ($.process.x = y, execution.setVariable) go to the local scope and are
	//     NOT persisted -- they are discarded when the activity ends unless promoted by
	//     an output mapping.
	// When null, writes persist to process scope via the sink (normal task behaviour).
	private final Map<String, Object> localScope;

	public ProcessContext(final NodeInterface instance, final NodeInterface element,
						  final NodeInterface definition, final Map<String, Object> parameterValues) {
		this(instance, element, definition, parameterValues, null, null);
	}

	public ProcessContext(final NodeInterface instance, final NodeInterface element,
						  final NodeInterface definition, final Map<String, Object> parameterValues,
						  final VariableSink sink) {
		this(instance, element, definition, parameterValues, sink, null);
	}

	public ProcessContext(final NodeInterface instance, final NodeInterface element,
						  final NodeInterface definition, final Map<String, Object> parameterValues,
						  final VariableSink sink, final Map<String, Object> localScope) {

		this.instance        = instance;
		this.element         = element;
		this.definition      = definition;
		this.parameterValues = parameterValues;
		this.sink            = sink;
		this.localScope      = localScope;
	}

	@Override
	public Object getMember(String key) {

		return switch (key) {

			case "instance"   -> instance;
			case "element"    -> element;
			case "definition" -> definition;
			// Local scope shadows process variables during the activity.
			default           -> (localScope != null && localScope.containsKey(key))
									? localScope.get(key)
									: (parameterValues != null ? parameterValues.get(key) : null);
		};
	}

	@Override
	public Object getMemberKeys() {
		return new String[] { "instance", "element", "definition" };
	}

	@Override
	public boolean hasMember(String key) {

		return switch (key) {

			case "instance", "element", "definition" -> true;
			default -> (localScope != null && localScope.containsKey(key))
						|| (parameterValues != null && parameterValues.containsKey(key));
		};
	}

	@Override
	public void putMember(String key, Value value) {

		if ("instance".equals(key) || "element".equals(key) || "definition".equals(key)) {

			throw new IllegalArgumentException("$.process." + key + " is read-only");
		}

		final Object javaValue = toJavaValue(value);

		// Inside an activity-local scope (io mappings), writes stay local and are NOT
		// persisted -- they vanish with the activity unless an output mapping promotes
		// them. Elsewhere, writes persist to process scope via the sink.
		if (localScope != null) {

			localScope.put(key, javaValue);
			return;
		}

		// Update the live view so subsequent reads in the same evaluation see the write.
		if (parameterValues != null) {
			parameterValues.put(key, javaValue);
		}

		// Persist as a ProcessParameterValue (or onto the subject) via the engine.
		if (sink != null) {

			try {

				sink.set(key, javaValue);

			} catch (final FrameworkException fex) {

				// putMember can't throw checked exceptions; surface as unchecked so the
				// script evaluation fails visibly rather than silently losing the write.
				throw new RuntimeException("Failed to persist process variable '" + key + "': " + fex.getMessage(), fex);
			}
		}
	}

	/** Convert a Graal polyglot value to a plain Java scalar for persistence. */
	private static Object toJavaValue(final Value value) {

		if (value == null || value.isNull()) {
			return null;
		}
		if (value.isBoolean()) {
			return value.asBoolean();
		}
		if (value.isNumber()) {
			return value.fitsInLong() ? value.asLong() : value.asDouble();
		}
		if (value.isString()) {
			return value.asString();
		}
		return value.toString();
	}
}
