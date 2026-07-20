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
package org.structr.process.traits.wrappers;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.process.entity.BpmnElement;
import org.structr.process.entity.ProcessInstance;
import org.structr.process.entity.TaskInstance;
import org.structr.process.traits.definitions.TaskInstanceTraitDefinition;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TaskInstanceTraitWrapper extends AbstractNodeTraitWrapper implements TaskInstance {

	public TaskInstanceTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getStatus() {
		return wrappedObject.getProperty(traits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY));
	}

	@Override
	public void setStatus(final String status) throws FrameworkException {
		wrappedObject.setProperty(traits.key(TaskInstanceTraitDefinition.STATUS_PROPERTY), status);
	}

	@Override
	public boolean isCreated() {
		return TaskInstanceTraitDefinition.STATUS_CREATED.equals(getStatus());
	}

	@Override
	public boolean isAvailable() {
		return TaskInstanceTraitDefinition.STATUS_AVAILABLE.equals(getStatus());
	}

	@Override
	public boolean isReserved() {
		return TaskInstanceTraitDefinition.STATUS_RESERVED.equals(getStatus());
	}

	@Override
	public boolean isCompleted() {
		return TaskInstanceTraitDefinition.STATUS_COMPLETED.equals(getStatus());
	}

	@Override
	public boolean isCancelled() {
		return TaskInstanceTraitDefinition.STATUS_CANCELLED.equals(getStatus());
	}

	@Override
	public boolean isTerminal() {
		return isCompleted() || isCancelled();
	}

	@Override
	public NodeInterface getAssignee() {
		return wrappedObject.getProperty(traits.key(TaskInstanceTraitDefinition.ASSIGNEE_PROPERTY));
	}

	@Override
	public void setAssignee(final NodeInterface assignee) throws FrameworkException {
		wrappedObject.setProperty(traits.key(TaskInstanceTraitDefinition.ASSIGNEE_PROPERTY), assignee);
	}

	@Override
	public String getAssigneeSetBy() {
		return wrappedObject.getProperty(traits.key(TaskInstanceTraitDefinition.ASSIGNEE_SET_BY_PROPERTY));
	}

	@Override
	public void setAssigneeSetBy(final String setBy) throws FrameworkException {
		wrappedObject.setProperty(traits.key(TaskInstanceTraitDefinition.ASSIGNEE_SET_BY_PROPERTY), setBy);
	}

	@Override
	public List<NodeInterface> getCandidateAssignees() {
		return toList(wrappedObject.getProperty(traits.key(TaskInstanceTraitDefinition.CANDIDATE_ASSIGNEES_PROPERTY)));
	}

	@Override
	public void setCandidateAssignees(final List<NodeInterface> candidates) throws FrameworkException {
		wrappedObject.setProperty(traits.key(TaskInstanceTraitDefinition.CANDIDATE_ASSIGNEES_PROPERTY), candidates);
	}

	@Override
	public List<NodeInterface> getDeclinedBy() {
		return toList(wrappedObject.getProperty(traits.key(TaskInstanceTraitDefinition.DECLINED_BY_PROPERTY)));
	}

	@Override
	public void setDeclinedBy(final List<NodeInterface> declinedBy) throws FrameworkException {
		wrappedObject.setProperty(traits.key(TaskInstanceTraitDefinition.DECLINED_BY_PROPERTY), declinedBy);
	}

	@Override
	public ProcessInstance getProcessInstance() {
		final NodeInterface instance = wrappedObject.getProperty(traits.key(TaskInstanceTraitDefinition.PROCESS_INSTANCE_PROPERTY));
		return instance != null ? instance.as(ProcessInstance.class) : null;
	}

	@Override
	public void setProcessInstance(final NodeInterface instance) throws FrameworkException {
		wrappedObject.setProperty(traits.key(TaskInstanceTraitDefinition.PROCESS_INSTANCE_PROPERTY), instance);
	}

	@Override
	public BpmnElement getDefinedBy() {
		final NodeInterface element = wrappedObject.getProperty(traits.key(TaskInstanceTraitDefinition.DEFINED_BY_PROPERTY));
		return element != null ? element.as(BpmnElement.class) : null;
	}

	@Override
	public void setDefinedBy(final NodeInterface element) throws FrameworkException {
		wrappedObject.setProperty(traits.key(TaskInstanceTraitDefinition.DEFINED_BY_PROPERTY), element);
	}

	@Override
	public void setCreatedTime(final Date time) throws FrameworkException {
		wrappedObject.setProperty(traits.key(TaskInstanceTraitDefinition.CREATED_TIME_PROPERTY), time);
	}

	@Override
	public void setClaimedTime(final Date time) throws FrameworkException {
		wrappedObject.setProperty(traits.key(TaskInstanceTraitDefinition.CLAIMED_TIME_PROPERTY), time);
	}

	@Override
	public Date getCompletedTime() {
		return wrappedObject.getProperty(traits.key(TaskInstanceTraitDefinition.COMPLETED_TIME_PROPERTY));
	}

	@Override
	public void setCompletedTime(final Date time) throws FrameworkException {
		wrappedObject.setProperty(traits.key(TaskInstanceTraitDefinition.COMPLETED_TIME_PROPERTY), time);
	}

	@Override
	public Date getCancelledTime() {
		return wrappedObject.getProperty(traits.key(TaskInstanceTraitDefinition.CANCELLED_TIME_PROPERTY));
	}

	@Override
	public void setCancelledTime(final Date time) throws FrameworkException {
		wrappedObject.setProperty(traits.key(TaskInstanceTraitDefinition.CANCELLED_TIME_PROPERTY), time);
	}

	private List<NodeInterface> toList(final Iterable<NodeInterface> iterable) {
		final List<NodeInterface> out = new ArrayList<>();
		if (iterable != null) {
			for (final NodeInterface n : iterable) {
				out.add(n);
			}
		}
		return out;
	}
}
