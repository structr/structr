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
 *   $.process.<name>     -- process parameter values (read)
 *
 * This object is set as a constant on the ActionContext before script
 * evaluation and removed afterward.
 */
public class ProcessContext implements ProxyObject {

	private final NodeInterface instance;
	private final NodeInterface element;
	private final NodeInterface definition;
	private final Map<String, Object> parameterValues;

	public ProcessContext(final NodeInterface instance, final NodeInterface element,
						  final NodeInterface definition, final Map<String, Object> parameterValues) {

		this.instance        = instance;
		this.element         = element;
		this.definition      = definition;
		this.parameterValues = parameterValues;
	}

	@Override
	public Object getMember(String key) {

		return switch (key) {
			case "instance"   -> instance;
			case "element"    -> element;
			case "definition" -> definition;
			default           -> parameterValues != null ? parameterValues.get(key) : null;
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
			default -> parameterValues != null && parameterValues.containsKey(key);
		};
	}

	@Override
	public void putMember(String key, Value value) {
		// Process parameter writes are not yet supported through this interface.
		// Parameters are set via task.complete({...}) or signalEvent({...}).
	}
}
