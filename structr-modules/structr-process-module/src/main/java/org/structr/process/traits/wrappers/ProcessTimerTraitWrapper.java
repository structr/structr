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
import org.structr.process.entity.BpmnElement;
import org.structr.process.entity.ProcessInstance;
import org.structr.process.entity.ProcessTimer;
import org.structr.process.entity.ProcessToken;
import org.structr.process.traits.definitions.ProcessTimerTraitDefinition;

import java.util.Date;

public class ProcessTimerTraitWrapper extends AbstractNodeTraitWrapper implements ProcessTimer {

	public ProcessTimerTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getStatus() {
		return wrappedObject.getProperty(traits.key(ProcessTimerTraitDefinition.STATUS_PROPERTY));
	}

	@Override
	public void setStatus(final String status) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ProcessTimerTraitDefinition.STATUS_PROPERTY), status);
	}

	@Override
	public boolean isPending() {
		return ProcessTimerTraitDefinition.STATUS_PENDING.equals(getStatus());
	}

	@Override
	public String getTimerType() {
		return wrappedObject.getProperty(traits.key(ProcessTimerTraitDefinition.TIMER_TYPE_PROPERTY));
	}

	@Override
	public Boolean getCancelActivity() {
		return wrappedObject.getProperty(traits.key(ProcessTimerTraitDefinition.CANCEL_ACTIVITY_PROPERTY));
	}

	@Override
	public ProcessInstance getInstance() {

		final NodeInterface instance = wrappedObject.getProperty(traits.key(ProcessTimerTraitDefinition.INSTANCE_PROPERTY));
		return instance != null ? instance.as(ProcessInstance.class) : null;
	}

	@Override
	public ProcessToken getToken() {

		final NodeInterface token = wrappedObject.getProperty(traits.key(ProcessTimerTraitDefinition.TOKEN_PROPERTY));
		return token != null ? token.as(ProcessToken.class) : null;
	}

	@Override
	public BpmnElement getElement() {

		final NodeInterface element = wrappedObject.getProperty(traits.key(ProcessTimerTraitDefinition.ELEMENT_PROPERTY));
		return element != null ? element.as(BpmnElement.class) : null;
	}

	@Override
	public void setFiredAt(final Date firedAt) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ProcessTimerTraitDefinition.FIRED_AT_PROPERTY), firedAt);
	}

	@Override
	public void setErrorMessage(final String message) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ProcessTimerTraitDefinition.ERROR_MESSAGE_PROPERTY), message);
	}
}
