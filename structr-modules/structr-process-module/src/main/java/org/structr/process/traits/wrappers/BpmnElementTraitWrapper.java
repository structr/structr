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
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.process.bpmn.BpmnElementType;
import org.structr.process.entity.BpmnElement;
import org.structr.process.entity.BpmnPerformer;
import org.structr.process.entity.BpmnSequenceFlow;
import org.structr.process.entity.BpmnTaskListener;
import org.structr.process.traits.definitions.BpmnBaseNodeTraitDefinition;
import org.structr.process.traits.definitions.BpmnElementTraitDefinition;

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
	public Iterable<BpmnSequenceFlow> getOutgoingFlows() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(BpmnElementTraitDefinition.OUTGOING_FLOWS_PROPERTY);

		return Iterables.map(n -> n.as(BpmnSequenceFlow.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<BpmnSequenceFlow> getIncomingFlows() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(BpmnElementTraitDefinition.INCOMING_FLOWS_PROPERTY);

		return Iterables.map(n -> n.as(BpmnSequenceFlow.class), wrappedObject.getProperty(key));
	}

	@Override
	public BpmnElement getParentElement() {
		return element(wrappedObject.getProperty(traits.key(BpmnElementTraitDefinition.PARENT_ELEMENT_PROPERTY)));
	}

	@Override
	public Iterable<BpmnElement> getChildElements() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(BpmnElementTraitDefinition.CHILD_ELEMENTS_PROPERTY);

		return Iterables.map(n -> n.as(BpmnElement.class), wrappedObject.getProperty(key));
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
	public String getEventDefinitionId() {
		return wrappedObject.getProperty(traits.key(BpmnElementTraitDefinition.EVENT_DEF_ID_PROPERTY));
	}

	@Override
	public String getEventDefinitionRef() {
		return wrappedObject.getProperty(traits.key(BpmnElementTraitDefinition.EVENT_DEF_REF_PROPERTY));
	}

	@Override
	public String getTimerExpressionType() {
		return wrappedObject.getProperty(traits.key(BpmnElementTraitDefinition.TIMER_EXPRESSION_TYPE_PROPERTY));
	}

	@Override
	public String getDocumentation() {
		return wrappedObject.getProperty(traits.key(BpmnElementTraitDefinition.DOCUMENTATION_PROPERTY));
	}

	@Override
	public String getBpmnAttributes() {
		return wrappedObject.getProperty(traits.key(BpmnElementTraitDefinition.BPMN_ATTRIBUTES_PROPERTY));
	}

	@Override
	public Iterable<BpmnSequenceFlow> getChildFlows() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(BpmnElementTraitDefinition.CHILD_FLOWS_PROPERTY);

		return Iterables.map(n -> n.as(BpmnSequenceFlow.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<BpmnPerformer> getPerformers() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(BpmnElementTraitDefinition.PERFORMERS_PROPERTY);

		return Iterables.map(n -> n.as(BpmnPerformer.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<BpmnTaskListener> getTaskListeners() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(BpmnElementTraitDefinition.TASK_LISTENERS_PROPERTY);

		return Iterables.map(n -> n.as(BpmnTaskListener.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<NodeInterface> getMethods() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(BpmnElementTraitDefinition.METHODS_PROPERTY);

		return wrappedObject.getProperty(key);
	}

	// ------------------------------------------------------------------

	private BpmnElement element(final NodeInterface node) {
		return node != null ? node.as(BpmnElement.class) : null;
	}
}
