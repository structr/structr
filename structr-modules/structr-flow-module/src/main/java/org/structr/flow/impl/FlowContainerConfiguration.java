/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.module.api.DeployableEntity;

public interface FlowContainerConfiguration extends NodeInterface, DeployableEntity {

	/*
	public static final Property<FlowContainer> flow				= new EndNode<>("flow", FlowContainerConfigurationFlow.class);
	public static final Property<FlowContainer> activeForFlow       = new EndNode<>("activeForFlow", FlowActiveContainerConfiguration.class);
	public static final Property<String> validForEditor				= new StringProperty("validForEditor").indexed();
	public static final Property<String> configJson            		= new StringProperty("configJson");

	public static final View defaultView 							= new View(FlowAction.class, PropertyView.Public, validForEditor, configJson);
	public static final View uiView      							= new View(FlowAction.class, PropertyView.Ui, flow, activeForFlow, validForEditor, configJson);

	@Override
	public Map<String, Object> exportData() {
		Map<String, Object> result = new HashMap<>();

		result.put("id", this.getUuid());
		result.put("type", this.getClass().getSimpleName());
		result.put("name", this.getName());
		result.put("validForEditor", this.getProperty(validForEditor));
		result.put("configJson", this.getProperty(configJson));
		result.put("visibleToPublicUsers", true);
		result.put("visibleToAuthenticatedUsers", true);

		return result;
	}

	@Override
	public void onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		super.onCreation(securityContext, errorBuffer);

		this.setProperty(visibleToAuthenticatedUsers, true);
		this.setProperty(visibleToPublicUsers, true);
	}
	*/
}
