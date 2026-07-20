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
import org.structr.process.entity.BpmnProcess;
import org.structr.process.entity.ProcessInstance;
import org.structr.process.entity.ProcessParameterValue;
import org.structr.process.entity.ProcessToken;
import org.structr.process.entity.TaskInstance;
import org.structr.process.traits.definitions.ProcessInstanceTraitDefinition;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProcessInstanceTraitWrapper extends AbstractNodeTraitWrapper implements ProcessInstance {

	public ProcessInstanceTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getStatus() {
		return wrappedObject.getProperty(traits.key(ProcessInstanceTraitDefinition.STATUS_PROPERTY));
	}

	@Override
	public void setStatus(final String status) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ProcessInstanceTraitDefinition.STATUS_PROPERTY), status);
	}

	@Override
	public boolean isRunning() {
		return ProcessInstanceTraitDefinition.STATUS_RUNNING.equals(getStatus());
	}

	@Override
	public boolean isCompleted() {
		return ProcessInstanceTraitDefinition.STATUS_COMPLETED.equals(getStatus());
	}

	@Override
	public boolean isSuspended() {
		return ProcessInstanceTraitDefinition.STATUS_SUSPENDED.equals(getStatus());
	}

	@Override
	public boolean isTerminated() {
		return ProcessInstanceTraitDefinition.STATUS_TERMINATED.equals(getStatus());
	}

	@Override
	public BpmnProcess getProcess() {
		final NodeInterface process = wrappedObject.getProperty(traits.key(ProcessInstanceTraitDefinition.PROCESS_PROPERTY));
		return process != null ? process.as(BpmnProcess.class) : null;
	}

	@Override
	public void setProcess(final NodeInterface process) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ProcessInstanceTraitDefinition.PROCESS_PROPERTY), process);
	}

	@Override
	public NodeInterface getSubject() {
		return wrappedObject.getProperty(traits.key(ProcessInstanceTraitDefinition.SUBJECT_PROPERTY));
	}

	@Override
	public void setSubject(final NodeInterface subject) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ProcessInstanceTraitDefinition.SUBJECT_PROPERTY), subject);
	}

	@Override
	public NodeInterface getInitiator() {
		return wrappedObject.getProperty(traits.key(ProcessInstanceTraitDefinition.INITIATOR_PROPERTY));
	}

	@Override
	public void setInitiator(final NodeInterface initiator) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ProcessInstanceTraitDefinition.INITIATOR_PROPERTY), initiator);
	}

	@Override
	public List<ProcessToken> getTokens() {

		final List<ProcessToken> out                   = new ArrayList<>();
		final PropertyKey<Iterable<NodeInterface>> key = traits.key(ProcessInstanceTraitDefinition.TOKENS_PROPERTY);
		final Iterable<NodeInterface> tokens           = wrappedObject.getProperty(key);

		if (tokens != null) {

			for (final NodeInterface token : tokens) {

				out.add(token.as(ProcessToken.class));
			}
		}
		return out;
	}

	@Override
	public List<TaskInstance> getTasks() {
		final List<TaskInstance> out = new ArrayList<>();
		final PropertyKey<Iterable<NodeInterface>> key = traits.key(ProcessInstanceTraitDefinition.TASKS_PROPERTY);
		final Iterable<NodeInterface> tasks = wrappedObject.getProperty(key);
		if (tasks != null) {
			for (final NodeInterface task : tasks) {
				out.add(task.as(TaskInstance.class));
			}
		}
		return out;
	}

	@Override
	public List<ProcessParameterValue> getParameterValues() {
		final List<ProcessParameterValue> out = new ArrayList<>();
		final PropertyKey<Iterable<NodeInterface>> key = traits.key(ProcessInstanceTraitDefinition.PARAMETER_VALUES_PROPERTY);
		final Iterable<NodeInterface> values = wrappedObject.getProperty(key);
		if (values != null) {
			for (final NodeInterface value : values) {
				out.add(value.as(ProcessParameterValue.class));
			}
		}
		return out;
	}

	@Override
	public void setStartTime(final Date startTime) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ProcessInstanceTraitDefinition.START_TIME_PROPERTY), startTime);
	}

	@Override
	public Date getEndTime() {
		return wrappedObject.getProperty(traits.key(ProcessInstanceTraitDefinition.END_TIME_PROPERTY));
	}

	@Override
	public void setEndTime(final Date endTime) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ProcessInstanceTraitDefinition.END_TIME_PROPERTY), endTime);
	}
}
