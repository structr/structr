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

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.structr.agent.AgentService;
import org.structr.agent.Task;
import org.structr.api.DatabaseService;
import org.structr.api.NotFoundException;
import org.structr.api.graph.GraphProperties;
import org.structr.api.service.Command;
import org.structr.api.service.Service;
import org.structr.api.util.FixedSizeCache;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.DummyFulltextIndexer;
import org.structr.common.fulltext.FulltextIndexer;
import org.structr.core.GraphObject;
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
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.graph.search.SearchRelationshipCommand;
import org.structr.core.property.PropertyMap;
import org.structr.module.StructrModule;
import org.structr.schema.ConfigurationProvider;

/**
 * Stateful facade for accessing the Structr core layer.
 *
 *
 */
public class StructrApp implements App {

	private static FixedSizeCache<String, Long> nodeUuidMap = null;
	private static FixedSizeCache<String, Long> relUuidMap  = null;
	private static final URI schemaBaseURI                  = URI.create("https://structr.org/v1.1/#");
	private static final Object globalConfigLock            = new Object();
	private RelationshipFactory relFactory                  = null;
	private NodeFactory nodeFactory                         = null;
	private DatabaseService graphDb                         = null;
	private SecurityContext securityContext                 = null;

	private StructrApp(final SecurityContext securityContext) {

		this.securityContext = securityContext;
		this.relFactory      = new RelationshipFactory<>(securityContext);
		this.nodeFactory     = new NodeFactory<>(securityContext);
	}

	// ----- public methods -----
	@Override
	public <T extends NodeInterface> T create(final Class<T> type, final String name) throws FrameworkException {
		return create(type, new NodeAttribute(getConfiguration().getPropertyKeyForJSONName(type, "name"), name));
	}

	@Override
	public <T extends NodeInterface> T create(final Class<T> type, final PropertyMap source) throws FrameworkException {

		final CreateNodeCommand<T> command = command(CreateNodeCommand.class);
		final PropertyMap properties       = new PropertyMap(source);
		String finalType                   = type.getSimpleName();

		// try to identify the actual type from input set (creation wouldn't work otherwise anyway)
		final String typeFromInput = properties.get(NodeInterface.type);
		if (typeFromInput != null) {

			Class actualType = StructrApp.getConfiguration().getNodeEntityClass(typeFromInput);
			if (actualType == null) {

				// overwrite type information when creating a node (adhere to type specified by resource!)
				properties.put(AbstractNode.type, type.getSimpleName());

			} else if (actualType.isInterface()) {

				throw new FrameworkException(422, "Invalid interface type " + type.getSimpleName() + ", please supply a non-interface class name in the type property");

			} else {

				finalType = actualType.getSimpleName();
			}
		}

		// set type
		properties.put(AbstractNode.type, finalType);

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

		if (uuid == null) {
			return null;
		}

		final NodeInterface node = getNodeById(uuid);
		if (node != null) {

			return node;
		}

		final RelationshipInterface rel = getRelationshipById(uuid);
		if (rel != null) {

			return rel;
		}

		return null;
	}

	@Override
	public NodeInterface getNodeById(final String uuid) throws FrameworkException {

		if (uuid == null) {
			return null;
		}

		final Long nodeId = getNodeFromCache(uuid);
		if (nodeId == null) {

			final GraphObject entity = nodeQuery().uuid(uuid).includeDeletedAndHidden().getFirst();
			if (entity != null && uuid.equals(entity.getUuid())) {

				nodeUuidMap.put(uuid, entity.getId());
				return (NodeInterface)entity;
			}

		} else {

			try {
				return nodeFactory.instantiate(getDatabaseService().getNodeById(nodeId));

			} catch (NotFoundException ignore) {
				nodeUuidMap.remove(uuid);
			}
		}

		return null;
	}

	@Override
	public RelationshipInterface getRelationshipById(final String uuid) throws FrameworkException {

		if (uuid == null) {
			return null;
		}

		final Long id = getRelFromCache(uuid);
		if (id == null) {

			final GraphObject entity = relationshipQuery().uuid(uuid).getFirst();
			if (entity != null && uuid.equals(entity.getUuid())) {

				relUuidMap.put(uuid, entity.getId());
				return (RelationshipInterface)entity;
			}

		} else {

			try {
				return relFactory.instantiate(getDatabaseService().getRelationshipById(id));

			} catch (NotFoundException ignore) {
				relUuidMap.remove(uuid);
			}
		}

		return null;
	}

	@Override
	public <T extends GraphObject> T get(final Class<T> type, final String uuid) throws FrameworkException {

		final GraphObject entity = get(uuid);

		if (type != null && entity != null && type.isAssignableFrom(entity.getClass())) {

			return (T) entity;

		} else {

			return null;
		}
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
	public <T extends NodeInterface> Query<T> nodeQuery(final Class<T> type) {
		return command(SearchNodeCommand.class).andTypes(type);
	}

	@Override
	public Query<RelationshipInterface> relationshipQuery() {
		return command(SearchRelationshipCommand.class);
	}

	@Override
	public <T extends RelationshipInterface> Query<T> relationshipQuery(final Class<T> type) {
		return command(SearchRelationshipCommand.class).andType(type);
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
	public Tx tx(final boolean doValidation, final boolean doCallbacks, final boolean doNotifications) {
		return new Tx(securityContext, this, doValidation, doCallbacks, doNotifications).begin();
	}

	@Override
	public void shutdown() {
		Services.getInstance().shutdown();
	}

	@Override
	public void close() throws IOException {
		shutdown();
	}

	@Override
	public <T extends Command> T command(Class<T> commandType) {
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
	public DatabaseService getDatabaseService() {

		// cache graphdb instance
		if (graphDb == null) {
			graphDb = Services.getInstance().command(securityContext, GraphDatabaseCommand.class).execute();
		}

		return graphDb;
	}

	@Override
	public <T> T getGlobalSetting(final String key, final T defaultValue) throws FrameworkException {

		final GraphProperties config = getDatabaseService().getGlobalProperties();
		T value                      = null;

		if (config != null) {
			value = (T)config.getProperty(key);
		}

		if (value == null) {
			return defaultValue;
		}

		return value;
	}

	@Override
	public void setGlobalSetting(final String key, final Object value) throws FrameworkException {

		final GraphProperties config = getDatabaseService().getGlobalProperties();
		if (config != null) {

			config.setProperty(key, value);
		}
	}

	@Override
	public String getInstanceId() throws FrameworkException {

		synchronized (globalConfigLock) {

			String instanceId = (String) getGlobalSetting("structr.instance.id", null);
			System.out.println("instance id from getGlobalSetting: " + instanceId);
			if (instanceId == null) {

				instanceId = NodeServiceCommand.getNextUuid();
				setGlobalSetting("structr.instance.id", instanceId);
			}
			System.out.println("instance id: " + instanceId);
			return instanceId;
		}
	}

	@Override
	public FulltextIndexer getFulltextIndexer(final Object... params) {

		final Map<String, StructrModule> modules = StructrApp.getConfiguration().getModules();
		final StructrModule module               = modules.get("text-search");

		if (module != null && module instanceof FulltextIndexer) {
			return (FulltextIndexer)module;
		}

		return new DummyFulltextIndexer();
	}

	// ----- public static methods ----
	/**
	 * Constructs a new stateful App instance, initialized with the given
	 * security context.
	 *
	 * @return superuser app instance
	 */
	public static App getInstance() {
		return new StructrApp(SecurityContext.getSuperUserInstance());
	}

	/**
	 * Constructs a new stateful App instance, initialized with the given
	 * security context.
	 *
	 * @param securityContext
	 * @return app instance
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

	public static <T extends GraphObject> URI getSchemaId(final Class<T> type) {
		initializeSchemaIds();
		return typeIdMap.get(type);
	}

	public static Class resolveSchemaId(final URI uri) {
		initializeSchemaIds();
		return schemaIdMap.get(uri);
	}

	public static URI getSchemaBaseURI() {
		return schemaBaseURI;
	}

	// ----- private static methods -----
	private static void initializeSchemaIds() {

		if (schemaIdMap.isEmpty()) {

			for (final Class type : StructrApp.getConfiguration().getNodeEntities().values()) {
				registerType(type);
			}

			for (final Class type : StructrApp.getConfiguration().getRelationshipEntities().values()) {
				registerType(type);
			}
		}
	}

	private static void registerType(final Class type) {

		final URI id = schemaBaseURI.resolve(URI.create(("definitions/" + type.getSimpleName())));

		schemaIdMap.put(id, type);
		typeIdMap.put(type, id);
	}

	private static final Map<URI, Class> schemaIdMap = new LinkedHashMap<>();
	private static final Map<Class, URI> typeIdMap   = new LinkedHashMap<>();

	// ---------- private methods -----
	private synchronized Long getNodeFromCache(final String uuid) {

		if (nodeUuidMap == null) {

			final int cacheSize = Services.parseInt(StructrApp.getConfigurationValue(Services.APPLICATION_UUID_CACHE_SIZE), 100000);
			nodeUuidMap = new FixedSizeCache<>(cacheSize);
		}

		return nodeUuidMap.get(uuid);
	}

	private synchronized Long getRelFromCache(final String uuid) {

		if (relUuidMap == null) {

			final int cacheSize = Services.parseInt(StructrApp.getConfigurationValue(Services.APPLICATION_UUID_CACHE_SIZE), 100000);
			relUuidMap = new FixedSizeCache<>(cacheSize);
		}

		return relUuidMap.get(uuid);
	}

	@Override
	public void invalidateCache(){

		if (nodeUuidMap != null) {
			nodeUuidMap.clear();
		}

		if (relUuidMap != null) {
			relUuidMap.clear();
		}

	}
}
