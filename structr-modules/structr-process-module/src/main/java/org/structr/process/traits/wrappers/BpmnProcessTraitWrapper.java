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
import org.structr.process.bpmn.BpmnElementType;
import org.structr.process.entity.BpmnElement;
import org.structr.process.entity.BpmnProcess;
import org.structr.process.traits.definitions.BpmnProcessTraitDefinition;

import java.util.ArrayList;
import java.util.List;

public class BpmnProcessTraitWrapper extends AbstractNodeTraitWrapper implements BpmnProcess {

	public BpmnProcessTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getProcessName() {
		return wrappedObject.getProperty(traits.key(BpmnProcessTraitDefinition.PROCESS_NAME_PROPERTY));
	}

	@Override
	public boolean isDefaultAssigneeFromInitiator() {
		return Boolean.TRUE.equals(wrappedObject.getProperty(traits.key(BpmnProcessTraitDefinition.DEFAULT_ASSIGNEE_FROM_INITIATOR_PROPERTY)));
	}

	@Override
	public List<BpmnElement> getElements() {
		final List<BpmnElement> out = new ArrayList<>();
		final PropertyKey<Iterable<NodeInterface>> key = traits.key(BpmnProcessTraitDefinition.ELEMENTS_PROPERTY);
		final Iterable<NodeInterface> elements = wrappedObject.getProperty(key);
		if (elements != null) {
			for (final NodeInterface element : elements) {
				out.add(element.as(BpmnElement.class));
			}
		}
		return out;
	}

	@Override
	public List<NodeInterface> getProcessListeners() {
		final List<NodeInterface> out = new ArrayList<>();
		final PropertyKey<Iterable<NodeInterface>> key = traits.key(BpmnProcessTraitDefinition.PROCESS_LISTENERS_PROPERTY);
		final Iterable<NodeInterface> listeners = wrappedObject.getProperty(key);
		if (listeners != null) {
			for (final NodeInterface listener : listeners) {
				out.add(listener);
			}
		}
		return out;
	}

	@Override
	public BpmnElement getElementByBpmnId(final String bpmnId) {
		if (bpmnId == null) {
			return null;
		}
		for (final BpmnElement element : getElements()) {
			if (bpmnId.equals(element.getBpmnId())) {
				return element;
			}
		}
		return null;
	}

	@Override
	public BpmnElement getStartEvent() throws FrameworkException {

		final List<BpmnElement> startEvents = new ArrayList<>();
		for (final BpmnElement element : getElements()) {
			// Only top-level start events start the process; a start event nested in
			// a sub-process belongs to that sub-process, not the process itself.
			if (element.isType(BpmnElementType.START_EVENT) && element.getParentElement() == null) {
				startEvents.add(element);
			}
		}

		if (startEvents.size() > 1) {
			throw new FrameworkException(422, "Process definition has " + startEvents.size()
				+ " top-level start events; a single start event is required to start an instance");
		}
		return startEvents.isEmpty() ? null : startEvents.get(0);
	}
}
