/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.flow.impl;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.flow.traits.definitions.FlowConstantTraitDefinition;
import org.structr.module.api.DeployableEntity;

public class FlowConstant extends FlowDataSource implements DeployableEntity {

	public enum ConstantType {
		String,
		Boolean,
		Integer,
		Double,
		Date
	}

	public FlowConstant(Traits traits, NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public final String getConstantType() {
		return wrappedObject.getProperty(traits.key(FlowConstantTraitDefinition.CONSTANT_TYPE_PROPERTY));
	}

	public final Object getValue() {
		return wrappedObject.getProperty(traits.key(FlowConstantTraitDefinition.VALUE_PROPERTY));
	}
}
