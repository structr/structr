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
package org.structr.core.graph;

import org.structr.api.graph.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyMap;

import java.util.Map;

/**
 *
 *
 */
public interface ModificationEvent {

	boolean isNode();

	int getStatus();
	String getChangeLog();
	Map<String, StringBuilder> getUserChangeLogs();
	String getCallbackId();

	boolean isDeleted();
	boolean isModified();
	boolean isCreated();

	GraphObject getGraphObject();
	RelationshipType getRelationshipType();
	String getUuid();

	PropertyMap getNewProperties();
	PropertyMap getModifiedProperties();
	PropertyMap getRemovedProperties();

	Map<String, Object> getData(final SecurityContext securityContext) throws FrameworkException;
}
