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

import java.util.Date;

/**
 * Typed view of a {@code ProcessInstance} node. Obtain via
 * {@code node.as(ProcessInstance.class)}.
 */
public interface ProcessInstance extends NodeInterface {

	String getStatus();
	void setStatus(String status) throws FrameworkException;

	boolean isRunning();
	boolean isCompleted();
	boolean isSuspended();
	boolean isTerminated();

	BpmnProcess getProcess();
	void setProcess(NodeInterface process) throws FrameworkException;

	NodeInterface getSubject();
	void setSubject(NodeInterface subject) throws FrameworkException;

	NodeInterface getInitiator();
	void setInitiator(NodeInterface initiator) throws FrameworkException;

	Iterable<ProcessToken> getTokens();
	Iterable<TaskInstance> getTasks();
	Iterable<ProcessParameterValue> getParameterValues();

	void setStartTime(Date startTime) throws FrameworkException;
	Date getEndTime();
	void setEndTime(Date endTime) throws FrameworkException;
}
