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
package org.structr.core.traits.operations;

import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.operations.graphobject.*;
import org.structr.schema.action.Actions;

public class LifecycleMethodAdapter implements OnCreation, OnModification, OnDeletion, AfterCreation, AfterModification, AfterDeletion {

	private final String source;

	public LifecycleMethodAdapter(final String source) {
		this.source = source;
	}

	@Override
	public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
		Actions.execute(securityContext, graphObject, "${" + source + "}", "onCreate");
	}

	@Override
	public void afterCreation(GraphObject graphObject, SecurityContext securityContext) throws FrameworkException {
		Actions.execute(securityContext, graphObject, "${" + source + "}", "afterCreate");
	}

	@Override
	public void afterDeletion(GraphObject graphObject, SecurityContext securityContext, PropertyMap properties) {

		try {
			Actions.execute(securityContext, graphObject, "${" + source + "}", "afterDelete");

		} catch (FrameworkException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void afterModification(GraphObject graphObject, SecurityContext securityContext) throws FrameworkException {
		Actions.execute(securityContext, graphObject, "${" + source + "}", "afterSave");
	}

	@Override
	public void onDeletion(GraphObject graphObject, SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {
		Actions.execute(securityContext, graphObject, "${" + source + "}", "onDelete");
	}

	@Override
	public void onModification(GraphObject graphObject, SecurityContext securityContext, ErrorBuffer errorBuffer, ModificationQueue modificationQueue) throws FrameworkException {
		Actions.execute(securityContext, graphObject, "${" + source + "}", "onSave");
	}
}
