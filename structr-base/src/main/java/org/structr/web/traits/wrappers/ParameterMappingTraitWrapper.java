/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.GraphObjectTraitWrapper;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.event.ParameterMapping;

public class ParameterMappingTraitWrapper extends GraphObjectTraitWrapper<NodeInterface> implements ParameterMapping {

	public ParameterMappingTraitWrapper(final Traits traits, final NodeInterface node) {
		super(traits, node);
	}

	@Override
	public DOMElement getInputElement() {

		final NodeInterface node = wrappedObject.getProperty(traits.key("inputElement"));
		if (node != null) {

			return node.as(DOMElement.class);
		}

		return null;
	}

	@Override
	public String getParameterType() {
		return wrappedObject.getProperty(traits.key("parameterType"));
	}

	@Override
	public String getParameterName() {
		return wrappedObject.getProperty(traits.key("parameterName"));
	}

	@Override
	public String getConstantValue() {
		return wrappedObject.getProperty(traits.key("constantValue"));
	}

	@Override
	public String getScriptExpression() {
		return wrappedObject.getProperty(traits.key("scriptExpression"));
	}

	@Override
	public String getMethodResult() {
		return wrappedObject.getProperty(traits.key("methodResult"));
	}

	@Override
	public String getFlowResult() {
		return wrappedObject.getProperty(traits.key("flowResult"));
	}
}
