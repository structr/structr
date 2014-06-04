/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.app;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.structr.agent.AgentService;
import org.structr.agent.Task;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.GraphObject;
import org.structr.core.Service;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Relation;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.core.graph.CypherQueryCommand;
import org.structr.core.graph.DeleteNodeCommand;
import org.structr.core.graph.DeleteRelationshipCommand;
import org.structr.core.graph.GraphDatabaseCommand;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.graph.search.SearchRelationshipCommand;
import org.structr.core.property.PropertyMap;
import org.structr.schema.ConfigurationProvider;

/**
 * Stateful facade for accessing the Structr core layer.
 *
 * @author Christian Morgner
 */
public class StructrApp implements App {

	private static final Logger logger = Logger.getLogger(StructrApp.class.getName());

//	private Map<Class<? extends Command>, Command> commandCache = new LinkedHashMap<>();
	private SecurityContext securityContext                     = null;

	private StructrApp(final SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	// ----- public methods -----
	@Override
	public <T extends NodeInterface> T create(final Class<T> type, final String name) throws FrameworkException {
		return create(type, new NodeAttribute(AbstractNode.name, name));
	}

	@Override
	public <T extends NodeInterface> T create(final Class<T> type, final PropertyMap source) throws FrameworkException {

		final CreateNodeCommand<T> command = command(CreateNodeCommand.class);
		final PropertyMap properties       = new PropertyMap(source);

		// add type information when creating a node
		properties.put(AbstractNode.type, type.getSimpleName());

		return command.execute(properties);
	}

	@Override
	public <T extends NodeInterface> T create(final Class<T> type, final NodeAttribute<?>... attributes) throws FrameworkException {

		final List<NodeAttribute<?>> attrs = new LinkedList<>(Arrays.asList(attributes));
		final CreateNodeCommand<T> command = command(CreateNodeCommand.class);

		// add type information when creating a node
		attrs.add(new NodeAttribute(AbstractNode.type, type.getSimpleName()));

		return command.execute(attrs);
	}

	@Override
	public void delete(final NodeInterface node) {
		command(DeleteNodeCommand.class).execute(node);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, R extends Relation<A, B, ?, ?>> R create(final A fromNode, final B toNode, final Class<R> relType) throws FrameworkException {
		return command(CreateRelationshipCommand.class).execute(fromNode, toNode, relType);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, R extends Relation<A, B, ?, ?>> R create(final A fromNode, final B toNode, final Class<R> relType, final PropertyMap properties) throws FrameworkException {
		return command(CreateRelationshipCommand.class).execute(fromNode, toNode, relType, properties);
	}

	@Override
	public void delete(final RelationshipInterface relationship) {
		command(DeleteRelationshipCommand.class).execute(relationship);
	}

	@Override
	public GraphObject get(final String uuid) throws FrameworkException {

		GraphObject entity = nodeQuery().uuid(uuid).includeDeletedAndHidden().getFirst();
		if (entity == null) {

			entity = relationshipQuery().uuid(uuid).getFirst();
		}

		return entity;
	}

	@Override
	public <T extends GraphObject> T get(final Class<T> type, final String uuid) throws FrameworkException {

		return (T) get(uuid);
	}

	@Override
	public <T extends GraphObject> List<T> get(final Class<T> type) throws FrameworkException {

		final Query<T> query = command(SearchNodeCommand.class);
		return query.andType(type).getAsList();
	}

	@Override
	public Query<NodeInterface> nodeQuery() {
		return command(SearchNodeCommand.class);
	}

	@Override
	public Query<NodeInterface> nodeQuery(final boolean exact) {
		return command(SearchNodeCommand.class).exact(exact);
	}

	@Override
	public <T extends NodeInterface> Query<T> nodeQuery(final Class<T> type) {
		return command(SearchNodeCommand.class).andTypes(type);
	}

	@Override
	public <T extends NodeInterface> Query<T> nodeQuery(final Class<T> type, final boolean exact) {
		return command(SearchNodeCommand.class).exact(exact).andTypes(type);
	}

	@Override
	public Query<RelationshipInterface> relationshipQuery() {
		return command(SearchRelationshipCommand.class);
	}

	@Override
	public Query<RelationshipInterface> relationshipQuery(final boolean exact) {
		return command(SearchRelationshipCommand.class).exact(exact);
	}

	@Override
	public <T extends RelationshipInterface> Query<T> relationshipQuery(final Class<T> type) {
		return command(SearchRelationshipCommand.class).andTypes(type);
	}

	@Override
	public <T extends RelationshipInterface> Query<T> relationshipQuery(final Class<T> type, final boolean exact) {
		return command(SearchRelationshipCommand.class).exact(exact).andTypes(type);
	}

	@Override
	public Tx tx() {
		return tx(true);
	}

	@Override
	public Tx tx(final boolean doValidation) {
		return tx(doValidation, true);
	}

	@Override
	public Tx tx(final boolean doValidation, final boolean doCallbacks) {
		return new Tx(securityContext, this, doValidation, doCallbacks).begin();
	}

	@Override
	public void shutdown() {
		Services.getInstance().shutdown();
	}

	@Override
	public <T extends Command> T command(Class<T> commandType) {
//
//		Command command = commandCache.get(commandType);
//		if (command == null) {
//
//			command = Services.getInstance().command(securityContext, commandType);
//			commandCache.put(commandType, command);
//		}
//
//		return (T)command;

		return Services.getInstance().command(securityContext, commandType);
	}

	@Override
	public void processTasks(Task... tasks) {

		final AgentService agentService = getService(AgentService.class);
		if(agentService != null) {

			for(final Task task : tasks) {

				agentService.processTask(task);
			}
		}
	}

	@Override
	public <T extends Command & MaintenanceCommand> void maintenance(final Class<T> commandClass, final Map<String, Object> propertySet) throws FrameworkException {
		((MaintenanceCommand)Services.getInstance().command(securityContext, commandClass)).execute(propertySet);
	}

	@Override
	public List<GraphObject> cypher(final String cypherQuery, final Map<String, Object> parameters) throws FrameworkException {
		return Services.getInstance().command(securityContext, CypherQueryCommand.class).execute(cypherQuery, parameters);
	}

	@Override
	public <T extends Service> T getService(Class<T> serviceClass) {
		return Services.getInstance().getService(serviceClass);
	}

	@Override
	public GraphDatabaseService getGraphDatabaseService() {
		return Services.getInstance().command(securityContext, GraphDatabaseCommand.class).execute();
	}

	// ----- public static methods ----
	/**
	 * Constructs a new stateful App instance, initialized with the given
	 * security context.
	 *
	 * @return
	 */
	public static App getInstance() {
		return new StructrApp(SecurityContext.getSuperUserInstance());
	}

	/**
	 * Constructs a new stateful App instance, initialized with the given
	 * security context.
	 *
	 * @param securityContext
	 * @return
	 */
	public static App getInstance(final SecurityContext securityContext) {
		return new StructrApp(securityContext);
	}

	public static ConfigurationProvider getConfiguration() {
		return Services.getInstance().getConfigurationProvider();
	}

	public static String getConfigurationValue(final String key) {
		return StringUtils.trim(Services.getInstance().getConfigurationValue(key, null));
	}

	public static String getConfigurationValue(final String key, final String defaultValue) {
		return StringUtils.trim(Services.getInstance().getConfigurationValue(key, defaultValue));
	}
}
