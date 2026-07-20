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
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.process.entity.BpmnElement;
import org.structr.process.entity.BpmnMessageFlow;
import org.structr.process.traits.definitions.BpmnBaseNodeTraitDefinition;
import org.structr.process.traits.definitions.BpmnMessageFlowTraitDefinition;

public class BpmnMessageFlowTraitWrapper extends AbstractNodeTraitWrapper implements BpmnMessageFlow {

	public BpmnMessageFlowTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getBpmnId() {
		return wrappedObject.getProperty(traits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
	}

	@Override
	public String getBpmnName() {
		return wrappedObject.getProperty(traits.key(BpmnMessageFlowTraitDefinition.BPMN_NAME_PROPERTY));
	}

	@Override
	public String getSourceRefId() {
		return wrappedObject.getProperty(traits.key(BpmnMessageFlowTraitDefinition.SOURCE_REF_ID_PROPERTY));
	}

	@Override
	public String getTargetRefId() {
		return wrappedObject.getProperty(traits.key(BpmnMessageFlowTraitDefinition.TARGET_REF_ID_PROPERTY));
	}

	@Override
	public BpmnElement getSourceElement() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(BpmnMessageFlowTraitDefinition.SOURCE_ELEMENT_PROPERTY));

		return node != null ? node.as(BpmnElement.class) : null;
	}

	@Override
	public BpmnElement getTargetElement() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(BpmnMessageFlowTraitDefinition.TARGET_ELEMENT_PROPERTY));

		return node != null ? node.as(BpmnElement.class) : null;
	}
}
