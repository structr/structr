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
package org.structr.core.traits;

import org.structr.api.graph.PropertyContainer;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.PropertyMap;

public class DynamicTypeTrait implements NodeTrait {

	protected DynamicTypeTrait(final PropertyContainer node) {
		super(node);
	}


	@Override
	public boolean isDeleted() {
		return false;
	}

	@Override
	public void onNodeCreation(SecurityContext securityContext) throws FrameworkException {

	}

	@Override
	public void onNodeDeletion(SecurityContext securityContext) throws FrameworkException {

	}

	@Override
	public boolean isValid(GraphObject obj, ErrorBuffer errorBuffer) {
		return false;
	}

	@Override
	public void onCreation(GraphObject obj, SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

	}

	@Override
	public void onModification(GraphObject obj, SecurityContext securityContext, ErrorBuffer errorBuffer, ModificationQueue modificationQueue) throws FrameworkException {

	}

	@Override
	public void onDeletion(GraphObject obj, SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {

	}

	@Override
	public void afterCreation(GraphObject obj, SecurityContext securityContext) throws FrameworkException {

	}

	@Override
	public void afterModification(GraphObject obj, SecurityContext securityContext) throws FrameworkException {

	}

	@Override
	public void afterDeletion(GraphObject obj, SecurityContext securityContext, PropertyMap properties) {

	}

	@Override
	public void ownerModified(GraphObject obj, SecurityContext securityContext) {

	}

	@Override
	public void securityModified(GraphObject obj, SecurityContext securityContext) {

	}

	@Override
	public void locationModified(GraphObject obj, SecurityContext securityContext) {

	}

	@Override
	public void propagatedModification(GraphObject obj, SecurityContext securityContext) {

	}
}
