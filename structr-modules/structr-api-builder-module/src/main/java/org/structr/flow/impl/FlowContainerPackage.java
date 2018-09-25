/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.Tx;
import org.structr.core.property.*;
import org.structr.flow.impl.rels.FlowContainerPackageFlow;
import org.structr.flow.impl.rels.FlowContainerPackagePackage;
import org.structr.module.api.DeployableEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class FlowContainerPackage extends AbstractNode implements DeployableEntity {

	public static final Property<FlowContainerPackage> parent			= new StartNode<>("parent", FlowContainerPackagePackage.class);
	public static final Property<List<FlowContainerPackage>> packages	= new EndNodes<>("packages", FlowContainerPackagePackage.class);
	public static final Property<List<FlowContainer>> flows				= new EndNodes<>("flows", FlowContainerPackageFlow.class);

	public static final Property<String> name 							= new StringProperty("name").notNull().indexed();
	public static final Property<Object> effectiveName					= new FunctionProperty<>("effectiveName").indexed().unique().notNull().readFunction("if(empty(this.parent), this.name, concat(this.parent.effectiveName, \".\", this.name))").typeHint("String");
	public static final Property<Boolean> scheduledForIndexing			= new BooleanProperty("scheduledForIndexing").defaultValue(false);

	public static final View defaultView = new View(FlowContainer.class, PropertyView.Public, name, effectiveName, packages, flows, parent, scheduledForIndexing);
	public static final View uiView      = new View(FlowContainer.class, PropertyView.Ui,     name, effectiveName, packages, flows, parent, scheduledForIndexing);

	private static final Logger logger = LoggerFactory.getLogger(FlowContainerPackage.class);

	@Override
	public Map<String, Object> exportData() {
		Map<String, Object> result = new HashMap<>();

		result.put("id", this.getUuid());
		result.put("type", this.getClass().getSimpleName());
		result.put("name", this.getName());

		result.put("visibleToPublicUsers", this.getProperty(visibleToPublicUsers));
		result.put("visibleToAuthenticatedUsers", this.getProperty(visibleToAuthenticatedUsers));

		return result;
	}

	@Override
	public void onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		super.onCreation(securityContext, errorBuffer);

		this.setProperty(visibleToAuthenticatedUsers, true);
		this.setProperty(visibleToPublicUsers, true);;
	}

	@Override
	public void onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
		super.onModification(securityContext, errorBuffer, modificationQueue);
		if (modificationQueue.getModifiedProperties().contains(scheduledForIndexing)) {
			scheduleIndexingForChildren();
		}
		setProperty(scheduledForIndexing, false);
	}

	@Override
	public void onNodeDeletion() {
		deleteChildren();
	}

	private void scheduleIndexingForChildren() {

		List<FlowContainerPackage> p = getProperty(packages);
		List<FlowContainer> c = getProperty(flows);

		try {
			for (FlowContainerPackage pack : p) {
				pack.setProperty(FlowContainerPackage.scheduledForIndexing, true);
			}

			for (FlowContainer cont : c) {
				cont.setProperty(FlowContainer.scheduledForIndexing, true);
			}
		} catch (FrameworkException ex) {
			logger.warn("Could not handle onDelete for FlowContainerPackage: " + ex.getMessage());
		}

	}

	private void deleteChildren() {

		List<FlowContainerPackage> p = getProperty(packages);
		List<FlowContainer> c = getProperty(flows);
		App app = StructrApp.getInstance();

		try (Tx tx = app.tx()) {
			for (FlowContainerPackage pack : p) {
				app.delete(pack);
			}

			for (FlowContainer cont : c) {
				app.delete(cont);
			}
		} catch (FrameworkException ex) {
			logger.warn("Could not handle onDelete for FlowContainerPackage: " + ex.getMessage());
		}

	}

}
