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

import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.PropertyMap;

public interface GraphObjectTrait extends PropertyContainerTrait {

	boolean isGranted(final GraphObject graphObject, final Permission permission, final SecurityContext securityContext, final boolean isCreation);
	boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer);
	void onCreation(final GraphObject obj, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException;
	void onModification(final GraphObject obj, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException;
	void onDeletion(final GraphObject obj, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException;
	void afterCreation(final GraphObject obj, final SecurityContext securityContext) throws FrameworkException;
	void afterModification(final GraphObject obj, final SecurityContext securityContext) throws FrameworkException;
	void afterDeletion(final GraphObject obj, final SecurityContext securityContext, final PropertyMap properties);
	void ownerModified(final GraphObject obj, final SecurityContext securityContext);
	void securityModified(final GraphObject obj, final SecurityContext securityContext);
	void locationModified(final GraphObject obj, final SecurityContext securityContext);
	void propagatedModification(final GraphObject obj, final SecurityContext securityContext);
}
