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

import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.process.bpmn.BpmnElementType;
import org.structr.process.entity.BpmnElement;
import org.structr.process.entity.BpmnSequenceFlow;
import org.structr.process.traits.definitions.BpmnBaseNodeTraitDefinition;
import org.structr.process.traits.definitions.BpmnElementTraitDefinition;

import java.util.ArrayList;
import java.util.List;

public class BpmnElementTraitWrapper extends AbstractNodeTraitWrapper implements BpmnElement {

	public BpmnElementTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getBpmnId() {
		return wrappedObject.getProperty(traits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
	}

	@Override
	public String getBpmnName() {
		return wrappedObject.getProperty(traits.key(BpmnElementTraitDefinition.BPMN_NAME_PROPERTY));
	}

	@Override
	public String getElementTypeName() {
		return wrappedObject.getProperty(traits.key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY));
	}

	@Override
	public BpmnElementType getElementType() {
		return BpmnElementType.fromBpmnName(getElementTypeName());
	}

	@Override
	public boolean isType(final BpmnElementType type) {
		return type != null && type == getElementType();
	}

	@Override
	public List<BpmnSequenceFlow> getOutgoingFlows() {
		return flows(BpmnElementTraitDefinition.OUTGOING_FLOWS_PROPERTY);
	}

	@Override
	public List<BpmnSequenceFlow> getIncomingFlows() {
		return flows(BpmnElementTraitDefinition.INCOMING_FLOWS_PROPERTY);
	}

	@Override
	public BpmnElement getParentElement() {
		return element(wrappedObject.getProperty(traits.key(BpmnElementTraitDefinition.PARENT_ELEMENT_PROPERTY)));
	}

	@Override
	public List<BpmnElement> getChildElements() {
		return elements(BpmnElementTraitDefinition.CHILD_ELEMENTS_PROPERTY);
	}

	@Override
	public BpmnElement getAttachedToElement() {
		return element(wrappedObject.getProperty(traits.key(BpmnElementTraitDefinition.ATTACHED_TO_PROPERTY)));
	}

	@Override
	public String getScriptContent() {
		return wrappedObject.getProperty(traits.key(BpmnElementTraitDefinition.SCRIPT_CONTENT_PROPERTY));
	}

	@Override
	public String getEventDefinitionType() {
		return wrappedObject.getProperty(traits.key(BpmnElementTraitDefinition.EVENT_DEF_TYPE_PROPERTY));
	}

	@Override
	public String getTimerType() {
		return wrappedObject.getProperty(traits.key(BpmnElementTraitDefinition.TIMER_TYPE_PROPERTY));
	}

	@Override
	public String getTimerValue() {
		return wrappedObject.getProperty(traits.key(BpmnElementTraitDefinition.TIMER_VALUE_PROPERTY));
	}

	@Override
	public List<NodeInterface> getPerformers() {
		return list(wrappedObject.getProperty(traits.key(BpmnElementTraitDefinition.PERFORMERS_PROPERTY)));
	}

	@Override
	public List<NodeInterface> getTaskListeners() {
		return list(wrappedObject.getProperty(traits.key(BpmnElementTraitDefinition.TASK_LISTENERS_PROPERTY)));
	}

	// ------------------------------------------------------------------

	private List<BpmnSequenceFlow> flows(final String propertyName) {
		final List<BpmnSequenceFlow> out = new ArrayList<>();
		final PropertyKey<Iterable<NodeInterface>> key = traits.key(propertyName);
		final Iterable<NodeInterface> flows = wrappedObject.getProperty(key);
		if (flows != null) {
			for (final NodeInterface flow : flows) {
				out.add(flow.as(BpmnSequenceFlow.class));
			}
		}
		return out;
	}

	private List<BpmnElement> elements(final String propertyName) {
		final List<BpmnElement> out = new ArrayList<>();
		final PropertyKey<Iterable<NodeInterface>> key = traits.key(propertyName);
		final Iterable<NodeInterface> elements = wrappedObject.getProperty(key);
		if (elements != null) {
			for (final NodeInterface element : elements) {
				out.add(element.as(BpmnElement.class));
			}
		}
		return out;
	}

	private BpmnElement element(final NodeInterface node) {
		return node != null ? node.as(BpmnElement.class) : null;
	}

	private List<NodeInterface> list(final Iterable<NodeInterface> iterable) {
		final List<NodeInterface> out = new ArrayList<>();
		if (iterable != null) {
			for (final NodeInterface n : iterable) {
				out.add(n);
			}
		}
		return out;
	}
}
