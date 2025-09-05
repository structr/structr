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
package org.structr.core.traits.operations;

import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.operations.graphobject.*;
import org.structr.core.traits.operations.nodeinterface.OnNodeCreation;
import org.structr.core.traits.operations.nodeinterface.OnNodeDeletion;
import org.structr.schema.action.Actions;

import java.util.LinkedList;
import java.util.List;

public class LifecycleMethodAdapter implements OnCreation, OnModification, OnDeletion, AfterCreation, AfterModification, AfterDeletion, OnNodeCreation, OnNodeDeletion {

	private final List<String> sources = new LinkedList<>();

	public LifecycleMethodAdapter(final String input) {
		this.sources.add(input.trim());
	}

	public void addSource(final String source) {
		this.sources.add(source);
	}

	@Override
	public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		final String type = graphObject.getTraits().getName();

		for (final String source : sources) {
			Actions.execute(securityContext, graphObject, "${" + source + "}", type + ".onCreate");
		}
	}

	@Override
	public void afterCreation(final GraphObject graphObject, final SecurityContext securityContext) throws FrameworkException {

		final String type = graphObject.getTraits().getName();

		for (final String source : sources) {
			Actions.execute(securityContext, graphObject, "${" + source + "}", type + ".afterCreate");
		}
	}

	@Override
	public void afterDeletion(final GraphObject graphObject, final SecurityContext securityContext, final PropertyMap properties) {

		final String type = graphObject.getTraits().getName();

		try {

			securityContext.getContextStore().setConstant("data", properties.getAsMap());

			// entity is null because it is deleted, properties are available via "data" keyword
			for (final String source : sources) {

				Actions.execute(securityContext, null, "${" + source + "}", type + ".afterDelete");
			}

		} catch (FrameworkException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void afterModification(final GraphObject graphObject, final SecurityContext securityContext) throws FrameworkException {

		final String type = graphObject.getTraits().getName();

		for (final String source : sources) {
			Actions.execute(securityContext, graphObject, "${" + source + "}", type + ".afterSave");
		}
	}

	@Override
	public void onDeletion(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

		final String type = graphObject.getTraits().getName();

		for (final String source : sources) {
			Actions.execute(securityContext, graphObject, "${" + source + "}", type + ".onDelete");
		}
	}

	@Override
	public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		final String type = graphObject.getTraits().getName();

		for (final String source : sources) {
			Actions.execute(securityContext, graphObject, "${" + source + "}", type + ".onSave", modificationQueue, null);
		}
	}

	@Override
	public void onNodeCreation(final NodeInterface nodeInterface, final SecurityContext securityContext) throws FrameworkException {

		final String type = nodeInterface.getTraits().getName();

		for (final String source : sources) {
			Actions.execute(securityContext, nodeInterface, "${" + source + "}", type + ".onNodeCreation");
		}
	}

	@Override
	public void onNodeDeletion(final NodeInterface nodeInterface, final SecurityContext securityContext) throws FrameworkException {

		final String type = nodeInterface.getTraits().getName();

		for (final String source : sources) {
			Actions.execute(securityContext, nodeInterface, "${" + source + "}", type + ".onNodeDeletion");
		}
	}
}
