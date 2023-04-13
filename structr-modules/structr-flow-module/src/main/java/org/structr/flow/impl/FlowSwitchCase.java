/*
 * Copyright (C) 2010-2023 Structr GmbH
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

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.flow.api.FlowElement;
import org.structr.flow.api.FlowType;
import org.structr.flow.api.Switch;
import org.structr.flow.impl.rels.FlowSwitchCases;
import org.structr.module.api.DeployableEntity;

import java.util.HashMap;
import java.util.Map;

public class FlowSwitchCase extends FlowNode implements Switch, DeployableEntity {
	public static final Property<String> switchCase                    = new StringProperty("case");
	public static final Property<FlowSwitch> switchNode                = new StartNode<>("switch", FlowSwitchCases.class);

	public static final View defaultView 						       = new View(FlowAction.class, PropertyView.Public, switchCase, next, switchNode);
	public static final View uiView      						       = new View(FlowAction.class, PropertyView.Ui, switchCase, next, switchNode);

	@Override
	public Map<String, Object> exportData() {
		Map<String, Object> result = new HashMap<>();

		result.put("id", this.getUuid());
		result.put("type", this.getClass().getSimpleName());
		result.put("case", this.getProperty(switchCase));
		result.put("visibleToPublicUsers", this.getProperty(visibleToPublicUsers));
		result.put("visibleToAuthenticatedUsers", this.getProperty(visibleToAuthenticatedUsers));

		return result;
	}

	@Override
	public FlowType getFlowType() {
		return FlowType.Switch;
	}

	@Override
	public FlowContainer getFlowContainer() {
		return getProperty(flowContainer);
	}

	@Override
	public FlowElement next() {
		return getProperty(next);
	}
}
