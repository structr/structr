/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.traits.wrappers;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.entity.event.ParameterMapping;
import org.structr.web.traits.definitions.ParameterMappingTraitDefinition;

import java.util.Map;

public class ParameterMappingTraitWrapper extends AbstractNodeTraitWrapper implements ParameterMapping {

	public ParameterMappingTraitWrapper(final Traits traits, final NodeInterface node) {
		super(traits, node);
	}

	@Override
	public ActionMapping getActionMapping() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(ParameterMappingTraitDefinition.ACTION_MAPPING_PROPERTY));
		if (node != null) {

			return node.as(ActionMapping.class);
		}

		return null;
	}

	@Override
	public DOMElement getInputElement() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(ParameterMappingTraitDefinition.INPUT_ELEMENT_PROPERTY));
		if (node != null) {

			return node.as(DOMElement.class);
		}

		return null;
	}

	@Override
	public String getParameterType() {
		return wrappedObject.getProperty(traits.key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY));
	}

	@Override
	public String getParameterName() {
		return wrappedObject.getProperty(traits.key(ParameterMappingTraitDefinition.PARAMETER_NAME_PROPERTY));
	}

	@Override
	public String getConstantValue() {
		return wrappedObject.getProperty(traits.key(ParameterMappingTraitDefinition.CONSTANT_VALUE_PROPERTY));
	}

	@Override
	public String getScriptExpression() {
		return wrappedObject.getProperty(traits.key(ParameterMappingTraitDefinition.SCRIPT_EXPRESSION_PROPERTY));
	}

	@Override
	public String getMethodResult() {
		return wrappedObject.getProperty(traits.key(ParameterMappingTraitDefinition.METHOD_RESULT_PROPERTY));
	}

	@Override
	public String getFlowResult() {
		return wrappedObject.getProperty(traits.key(ParameterMappingTraitDefinition.FLOW_RESULT_PROPERTY));
	}

	public NodeInterface cloneParameterMapping(final Map<String, DOMNode> mapOfClonedNodes) throws FrameworkException {

		final SecurityContext securityContext = getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);
		final PropertyMap properties          = new PropertyMap();

		properties.put(traits.key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY),    getParameterType());
		properties.put(traits.key(ParameterMappingTraitDefinition.PARAMETER_NAME_PROPERTY),    getParameterName());
		properties.put(traits.key(ParameterMappingTraitDefinition.CONSTANT_VALUE_PROPERTY),    getConstantValue());
		properties.put(traits.key(ParameterMappingTraitDefinition.SCRIPT_EXPRESSION_PROPERTY), getScriptExpression());
		properties.put(traits.key(ParameterMappingTraitDefinition.METHOD_RESULT_PROPERTY),     getMethodResult());
		properties.put(traits.key(ParameterMappingTraitDefinition.FLOW_RESULT_PROPERTY),       getFlowResult());

		// clone input elements
		final DOMElement inputElement = getInputElement();
		if (inputElement != null) {

			// find cloned input element
			DOMNode clonedInputElement = mapOfClonedNodes.get(inputElement.getUuid());
			if (clonedInputElement == null) {

				// if the element was not cloned, it is outside of the cloned subtree and can be referenced directly
				clonedInputElement = inputElement;
			}

			properties.put(traits.key(ParameterMappingTraitDefinition.INPUT_ELEMENT_PROPERTY), clonedInputElement);
		}

		return app.create(StructrTraits.PARAMETER_MAPPING, properties);
	}
}
