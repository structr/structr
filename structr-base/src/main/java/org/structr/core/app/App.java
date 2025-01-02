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
package org.structr.core.app;

import org.structr.agent.Task;
import org.structr.api.DatabaseService;
import org.structr.api.service.Command;
import org.structr.api.service.Service;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.ContentAnalyzer;
import org.structr.common.fulltext.FulltextIndexer;
import org.structr.core.GraphObject;
import org.structr.core.graph.*;
import org.structr.core.property.PropertyMap;

import java.io.Closeable;
import java.util.Map;

/**
 *
 *
 */
public interface App extends Closeable {

	Tx tx() throws FrameworkException;
	Tx tx(final boolean doValidation) throws FrameworkException;
	Tx tx(final boolean doValidation, final boolean doCallbacks) throws FrameworkException;
	Tx tx(final boolean doValidation, final boolean doCallbacks, final boolean doNotifications) throws FrameworkException;

	NodeInterface create(final String type, final String name) throws FrameworkException;
	NodeInterface create(final String type, final PropertyMap properties) throws FrameworkException;
	NodeInterface create(final String type, final NodeAttribute<?>... attributes) throws FrameworkException;

	void deleteAllNodesOfType(final String type) throws FrameworkException;
	void delete(final NodeInterface node) throws FrameworkException;

	RelationshipInterface create(final NodeInterface fromNode, final NodeInterface toNode, final String relType) throws FrameworkException;
	RelationshipInterface create(final NodeInterface fromNode, final NodeInterface toNode, final String relType, final PropertyMap properties) throws FrameworkException;

	void delete(final RelationshipInterface relationship);

	NodeInterface getNodeById(final String type, final String uuid) throws FrameworkException;
	NodeInterface getNodeById(final String uuid) throws FrameworkException;
	RelationshipInterface getRelationshipById(final String type, final String uuid) throws FrameworkException;
	RelationshipInterface getRelationshipById(final String uuid) throws FrameworkException;

	Query<NodeInterface> nodeQuery();
	Query<NodeInterface> nodeQuery(final String type);

	Query<RelationshipInterface> relationshipQuery();
	Query<RelationshipInterface> relationshipQuery(final String type);

	void shutdown();

	<T extends Command> T command(final Class<T> commandType);

	void processTasks(final Task... tasks);
	<T extends Command & MaintenanceCommand> void maintenance(final Class<T> commandClass, final Map<String, Object> propertySet) throws FrameworkException;

	ContentAnalyzer getContentAnalyzer(final Object... params);
	FulltextIndexer getFulltextIndexer(final Object... params);

	Iterable<GraphObject> query(final String nativeQuery, final Map<String, Object> parameters) throws FrameworkException;

	<T extends Service> T getService(final Class<T> serviceClass);
	DatabaseService getDatabaseService();

	/**
	 * Returns the unique instance ID of this Structr database instance. Please
	 * note that this method can throw a FrameworkException because it needs a
	 * transaction to access the database.
	 *
	 * @return a 32 character UUID
	 * @throws org.structr.common.error.FrameworkException
	 */
	String getInstanceId() throws FrameworkException;

	void invalidateCache();

	Map<String, Object> getAppContextStore();
}
