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
package org.structr.process.entity;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;

import java.util.List;

/**
 * Typed view of a {@code BpmnProcess} node. Obtain via
 * {@code node.as(BpmnProcess.class)}.
 */
public interface BpmnProcess extends NodeInterface {

	String getProcessName();
	boolean isDefaultAssigneeFromInitiator();

	List<BpmnElement> getElements();
	List<NodeInterface> getProcessListeners();

	/** The (single) top-level element with the given bpmnId, or {@code null}. */
	BpmnElement getElementByBpmnId(String bpmnId);

	/**
	 * The single top-level start event that starts this process, or {@code null}
	 * if there is none. Throws if the process declares more than one top-level
	 * start event (ambiguous entry point).
	 */
	BpmnElement getStartEvent() throws FrameworkException;
}
