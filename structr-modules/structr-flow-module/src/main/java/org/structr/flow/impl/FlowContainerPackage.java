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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.flow.traits.definitions.FlowContainerPackageTraitDefinition;
import org.structr.module.api.DeployableEntity;

import java.util.HashMap;
import java.util.Map;

public class FlowContainerPackage extends AbstractNodeTraitWrapper implements DeployableEntity {

	private static final Logger logger = LoggerFactory.getLogger(FlowContainerPackage.class);

	public FlowContainerPackage(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public Iterable<FlowContainer> getFlows() {

		final Iterable<NodeInterface> nodes = wrappedObject.getProperty(traits.key(FlowContainerPackageTraitDefinition.FLOWS_PROPERTY));

		return Iterables.map(n -> n.as(FlowContainer.class), nodes);
	}

	public Iterable<FlowContainerPackage> getPackages() {

		final Iterable<NodeInterface> nodes = wrappedObject.getProperty(traits.key(FlowContainerPackageTraitDefinition.PACKAGES_PROPERTY));

		return Iterables.map(n -> n.as(FlowContainerPackage.class), nodes);
	}

	public String getEffectiveName() {
		return getProperty(traits.key(FlowContainerPackageTraitDefinition.EFFECTIVE_NAME_PROPERTY));
	}

	public void setScheduledForIndexing(final boolean b) throws FrameworkException {
		wrappedObject.setProperty(traits.key(FlowContainerPackageTraitDefinition.SCHEDULED_FOR_INDEXING_PROPERTY), b);
	}

	@Override
	public Map<String, Object> exportData() {
		Map<String, Object> result = new HashMap<>();

		result.put(GraphObjectTraitDefinition.ID_PROPERTY,                             getUuid());
		result.put(GraphObjectTraitDefinition.TYPE_PROPERTY,                           getType());
		result.put(NodeInterfaceTraitDefinition.NAME_PROPERTY,                         getName());
		result.put(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        isVisibleToPublicUsers());
		result.put(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, isVisibleToAuthenticatedUsers());

		return result;
	}

	public void scheduleIndexingForRelatedEntities() {

		final Iterable<FlowContainerPackage> p = getPackages();
		final Iterable<FlowContainer> c        = getFlows();
		final App app                          = StructrApp.getInstance();

		try (Tx tx = app.tx()) {

			for (FlowContainerPackage pack : p) {
				pack.setScheduledForIndexing(true);
			}

			for (FlowContainer cont : c) {
				cont.setScheduledForIndexing(true);
			}

			tx.success();

		} catch (FrameworkException ex) {

			logger.warn("Could not handle onDelete for FlowContainerPackage: " + ex.getMessage());
		}
	}

	public void deleteChildren() {

		final Iterable<FlowContainerPackage> p = getPackages();
		final Iterable<FlowContainer> c        = getFlows();
		final App app                          = StructrApp.getInstance();

		try (Tx tx = app.tx()) {

			for (FlowContainerPackage pack : p) {
				app.delete(pack);
			}

			for (FlowContainer cont : c) {
				app.delete(cont);
			}

			tx.success();

		} catch (FrameworkException ex) {

			logger.warn("Could not handle onDelete for FlowContainerPackage: " + ex.getMessage());
		}
	}
}
