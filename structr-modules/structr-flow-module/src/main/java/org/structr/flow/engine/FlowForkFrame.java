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
import org.structr.common.trace.ContinuationFrame;

/**
 * The body of a flow fork, running after the transaction of the flow that started it.
 */
public class FlowForkFrame extends ContinuationFrame {

	public FlowForkFrame(final CallFrame flow, final String forkId) {
		super(flow, forkId);
	}

	@Override
	public String kind() {
		return "FLOW_FORK";
	}

	@Override
	public String describe() {
		return "flow fork " + getName();
	}

	@Override
	public boolean isLogContext() {
		return true;
	}
}
