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

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.process.bpmn.BpmnElementType;
import org.structr.process.entity.BpmnElement;
import org.structr.process.entity.BpmnLane;
import org.structr.process.entity.BpmnProcess;
import org.structr.process.entity.BpmnProcessListener;
import org.structr.process.entity.BpmnSequenceFlow;
import org.structr.process.traits.definitions.BpmnBaseNodeTraitDefinition;
import org.structr.process.traits.definitions.BpmnProcessTraitDefinition;

import java.util.LinkedList;
import java.util.List;

public class BpmnProcessTraitWrapper extends AbstractNodeTraitWrapper implements BpmnProcess {

	public BpmnProcessTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {

		super(traits, wrappedObject);
	}

	@Override
	public String getBpmnId() {

		return wrappedObject.getProperty(traits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
	}

	@Override
	public String getProcessId() {

		return wrappedObject.getProperty(traits.key(BpmnProcessTraitDefinition.PROCESS_ID_PROPERTY));
	}

	@Override
	public String getProcessName() {

		return wrappedObject.getProperty(traits.key(BpmnProcessTraitDefinition.PROCESS_NAME_PROPERTY));
	}

	@Override
	public boolean isExecutable() {

		return Boolean.TRUE.equals(wrappedObject.getProperty(traits.key(BpmnProcessTraitDefinition.PROCESS_IS_EXECUTABLE_PROPERTY)));
	}

	@Override
	public boolean isDefaultAssigneeFromInitiator() {

		return Boolean.TRUE.equals(wrappedObject.getProperty(traits.key(BpmnProcessTraitDefinition.DEFAULT_ASSIGNEE_FROM_INITIATOR_PROPERTY)));
	}

	@Override
	public Iterable<BpmnElement> getElements() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(BpmnProcessTraitDefinition.ELEMENTS_PROPERTY);

		return Iterables.map(n -> n.as(BpmnElement.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<BpmnSequenceFlow> getSequenceFlows() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(BpmnProcessTraitDefinition.SEQUENCE_FLOWS_PROPERTY);

		return Iterables.map(n -> n.as(BpmnSequenceFlow.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<BpmnLane> getLanes() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(BpmnProcessTraitDefinition.LANES_PROPERTY);

		return Iterables.map(n -> n.as(BpmnLane.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<NodeInterface> getMethods() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(BpmnProcessTraitDefinition.METHODS_PROPERTY);

		return wrappedObject.getProperty(key);
	}

	@Override
	public Iterable<BpmnProcessListener> getProcessListeners() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(BpmnProcessTraitDefinition.PROCESS_LISTENERS_PROPERTY);

		return Iterables.map(n -> n.as(BpmnProcessListener.class), wrappedObject.getProperty(key));
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

		final List<BpmnElement> startEvents = new LinkedList<>();

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
