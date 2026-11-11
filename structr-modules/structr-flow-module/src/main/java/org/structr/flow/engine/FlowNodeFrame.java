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
package org.structr.flow.engine;

import org.structr.common.trace.CallFrame;

import java.util.Map;

/**
 * A node of a flow being executed. It stays open while a failure of the node is routed to an
 * exception handler, so that a handled failure is settled before the frame is left.
 */
public class FlowNodeFrame extends CallFrame {

	private final String nodeType;
	private final String nodeId;

	public FlowNodeFrame(final String nodeType, final String nodeId) {

		super(nodeType, null, false);

		this.nodeType = nodeType;
		this.nodeId   = nodeId;
	}

	@Override
	public String kind() {
		return "FLOW";
	}

	@Override
	public String describe() {
		return nodeType + "[" + nodeId + "]";
	}

	@Override
	public boolean isLogContext() {
		return true;
	}

	@Override
	public Map<String, Object> toMap() {

		final Map<String, Object> map = super.toMap();

		map.put("id", nodeId);

		return map;
	}
}
