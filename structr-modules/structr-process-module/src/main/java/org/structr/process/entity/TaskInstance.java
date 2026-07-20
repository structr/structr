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
import java.util.List;

/**
 * Typed view of a {@code TaskInstance} node. Obtain via
 * {@code node.as(TaskInstance.class)}.
 */
public interface TaskInstance extends NodeInterface {

	String getStatus();
	void setStatus(String status) throws FrameworkException;

	boolean isCreated();
	boolean isAvailable();
	boolean isReserved();
	boolean isCompleted();
	boolean isCancelled();
	boolean isTerminal();

	NodeInterface getAssignee();
	void setAssignee(NodeInterface assignee) throws FrameworkException;

	String getAssigneeSetBy();
	void setAssigneeSetBy(String setBy) throws FrameworkException;

	List<NodeInterface> getCandidateAssignees();
	void setCandidateAssignees(List<NodeInterface> candidates) throws FrameworkException;

	List<NodeInterface> getDeclinedBy();
	void setDeclinedBy(List<NodeInterface> declinedBy) throws FrameworkException;

	ProcessInstance getProcessInstance();
	void setProcessInstance(NodeInterface instance) throws FrameworkException;

	BpmnElement getDefinedBy();
	void setDefinedBy(NodeInterface element) throws FrameworkException;

	void setCreatedTime(Date time) throws FrameworkException;
	void setClaimedTime(Date time) throws FrameworkException;
	Date getCompletedTime();
	void setCompletedTime(Date time) throws FrameworkException;
	Date getCancelledTime();
	void setCancelledTime(Date time) throws FrameworkException;
}
