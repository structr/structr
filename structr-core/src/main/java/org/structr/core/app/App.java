/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.GraphObject;
import org.structr.core.Service;
import org.structr.agent.Task;
import org.structr.core.entity.Relation;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;

/**
 *
 *
 */
public interface App extends Closeable {

	public Tx tx();
	public Tx tx(final boolean doValidation);
	public Tx tx(final boolean doValidation, final boolean doCallbacks);
	public Tx tx(final boolean doValidation, final boolean doCallbacks, final boolean doNotifications);

	public <T extends NodeInterface> T create(final Class<T> type, final String name) throws FrameworkException;
	public <T extends NodeInterface> T create(final Class<T> type, final PropertyMap properties) throws FrameworkException;
	public <T extends NodeInterface> T create(final Class<T> type, final NodeAttribute<?>... attributes) throws FrameworkException;

	public void delete(final NodeInterface node) throws FrameworkException;

	public <A extends NodeInterface, B extends NodeInterface, R extends Relation<A, B, ?, ?>> R create(final A fromNode, final B toNode, final Class<R> relType) throws FrameworkException;
	public <A extends NodeInterface, B extends NodeInterface, R extends Relation<A, B, ?, ?>> R create(final A fromNode, final B toNode, final Class<R> relType, final PropertyMap properties) throws FrameworkException;

	public void delete(final RelationshipInterface relationship);

	public GraphObject get(final String uuid) throws FrameworkException;
	public NodeInterface getNodeById(final String uuid) throws FrameworkException;
	public RelationshipInterface getRelationshipById(final String uuid) throws FrameworkException;
	public <T extends GraphObject> List<T> get(final Class<T> type) throws FrameworkException;
	public <T extends GraphObject> T get(final Class<T> type, final String uuid) throws FrameworkException;

	public Query<? extends NodeInterface> nodeQuery();
	public Query<? extends NodeInterface> nodeQuery(final boolean exact);
	public <T extends NodeInterface> Query<T> nodeQuery(final Class<T> type);
	public <T extends NodeInterface> Query<T> nodeQuery(final Class<T> type, final boolean exact);

	public Query<? extends RelationshipInterface> relationshipQuery();
	public Query<? extends RelationshipInterface> relationshipQuery(final boolean exact);
	public <T extends RelationshipInterface> Query<T> relationshipQuery(final Class<T> type);
	public <T extends RelationshipInterface> Query<T> relationshipQuery(final Class<T> type, final boolean exact);

	public void shutdown();

	public <T extends Command> T command(final Class<T> commandType);

	public void processTasks(final Task... tasks);
	public <T extends Command & MaintenanceCommand> void maintenance(final Class<T> commandClass, final Map<String, Object> propertySet) throws FrameworkException;

	public List<GraphObject> cypher(final String cypherQuery, final Map<String, Object> parameters) throws FrameworkException;

	public <T extends Service> T getService(final Class<T> serviceClass);
	public GraphDatabaseService getGraphDatabaseService();

	public <T> T getGlobalSetting(final String key, final T defaultValue) throws FrameworkException;
	public void setGlobalSetting(final String key, final Object value) throws FrameworkException;

	/**
	 * Returns the unique instance ID of this Structr database instance. Please
	 * note that this method can throw a FrameworkException because it needs a
	 * transaction to access the database.
	 *
	 * @return a 32 character UUID
	 * @throws org.structr.common.error.FrameworkException
	 */
	public String getInstanceId() throws FrameworkException;
}
