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
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.process.entity.ProcessInstance;
import org.structr.process.entity.ProcessToken;
import org.structr.process.traits.definitions.ProcessTokenTraitDefinition;

public class ProcessTokenTraitWrapper extends AbstractNodeTraitWrapper implements ProcessToken {

	public ProcessTokenTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getStatus() {
		return wrappedObject.getProperty(traits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY));
	}

	@Override
	public void setStatus(final String status) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ProcessTokenTraitDefinition.STATUS_PROPERTY), status);
	}

	@Override
	public boolean isActive() {
		return ProcessTokenTraitDefinition.STATUS_ACTIVE.equals(getStatus());
	}

	@Override
	public boolean isWaiting() {
		return ProcessTokenTraitDefinition.STATUS_WAITING.equals(getStatus());
	}

	@Override
	public boolean isCompleted() {
		return ProcessTokenTraitDefinition.STATUS_COMPLETED.equals(getStatus());
	}

	@Override
	public void markActive() throws FrameworkException {
		setStatus(ProcessTokenTraitDefinition.STATUS_ACTIVE);
	}

	@Override
	public void markWaiting() throws FrameworkException {
		setStatus(ProcessTokenTraitDefinition.STATUS_WAITING);
	}

	@Override
	public void markCompleted() throws FrameworkException {
		setStatus(ProcessTokenTraitDefinition.STATUS_COMPLETED);
	}

	@Override
	public NodeInterface getAtElement() {
		return wrappedObject.getProperty(traits.key(ProcessTokenTraitDefinition.AT_ELEMENT_PROPERTY));
	}

	@Override
	public void setAtElement(final NodeInterface element) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ProcessTokenTraitDefinition.AT_ELEMENT_PROPERTY), element);
	}

	@Override
	public ProcessInstance getProcessInstance() {

		final NodeInterface instance = wrappedObject.getProperty(traits.key(ProcessTokenTraitDefinition.PROCESS_INSTANCE_PROPERTY));
		return instance != null ? instance.as(ProcessInstance.class) : null;
	}

	@Override
	public void setProcessInstance(final NodeInterface instance) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ProcessTokenTraitDefinition.PROCESS_INSTANCE_PROPERTY), instance);
	}
}
