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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.agent.AgentService;
import org.structr.agent.Task;
import org.structr.api.DatabaseService;
import org.structr.api.NotFoundException;
import org.structr.api.config.Settings;
import org.structr.api.graph.Identity;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.service.Command;
import org.structr.api.service.Service;
import org.structr.api.util.FixedSizeCache;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.DummyFulltextIndexer;
import org.structr.common.fulltext.FulltextIndexer;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.graph.*;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.graph.search.SearchRelationshipCommand;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.module.StructrModule;
import org.structr.schema.ConfigurationProvider;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Stateful facade for accessing the Structr core layer.
 */
public class StructrApp implements App {

	private static final String INSTANCE_ID = StringUtils.replace(UUID.randomUUID().toString(), "-", "");
	private static final Logger logger      = LoggerFactory.getLogger(StructrApp.class);

	private static final URI schemaBaseURI                      = URI.create("https://structr.org/v1.1/#");
	private static final Map<URI, String> schemaIdMap           = new HashMap<>();
	private static final Map<String, URI> typeIdMap             = new HashMap<>();
	private static FixedSizeCache<String, Identity> nodeUuidMap = null;
	private static FixedSizeCache<String, Identity> relUuidMap  = null;
	private final Map<String, Object> appContextStore           = new HashMap<>();
	private RelationshipFactory relFactory                      = null;
	private NodeFactory nodeFactory                             = null;
	private DatabaseService graphDb                             = null;
	private SecurityContext securityContext                     = null;

	private StructrApp(final SecurityContext securityContext) {

		this.securityContext = securityContext;
		this.relFactory      = new RelationshipFactory(securityContext);
		this.nodeFactory     = new NodeFactory(securityContext);
	}

	// ----- public methods -----
	@Override
	public NodeInterface create(final String type, final String name) throws FrameworkException {
		return create(type, new NodeAttribute(Traits.of(type).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name));
	}

	@Override
	public NodeInterface create(final String type, final PropertyMap source) throws FrameworkException {

		if (type == null) {
			throw new FrameworkException(422, "Empty type (null). Please supply a valid class name in the type property.");
		}

		final CreateNodeCommand command = command(CreateNodeCommand.class);
		final PropertyMap properties    = new PropertyMap(source);
		String finalType                = type;

		// try to identify the actual type from input set (creation wouldn't work otherwise anyway)
		final String typeFromInput = properties.get(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.TYPE_PROPERTY));
		if (typeFromInput != null) {

			final Traits actualType = Traits.of(typeFromInput);
			if (actualType == null) {

				// overwrite type information when creating a node (adhere to type specified by resource!)
				properties.put(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.TYPE_PROPERTY), type);

			} else if (actualType.isInterface() || actualType.isAbstract()) {

				throw new FrameworkException(422, "Invalid abstract type " + type + ", please supply a non-abstract class name in the type property");

			} else {

				finalType = actualType.getName();
			}
		}

		// set type
		properties.put(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.TYPE_PROPERTY), finalType);

		return command.execute(properties);
	}

	@Override
	public NodeInterface create(final String type, final NodeAttribute<?>... attributes) throws FrameworkException {

		final List<NodeAttribute<?>> attrs = new LinkedList<>(Arrays.asList(attributes));
		final CreateNodeCommand command    = command(CreateNodeCommand.class);

		// add type information when creating a node
		attrs.add(new NodeAttribute(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.TYPE_PROPERTY), type));

		return command.execute(attrs);
	}

	@Override
	public void deleteAllNodesOfType(final String type) throws FrameworkException {

		final DeleteNodeCommand cmd = command(DeleteNodeCommand.class);
		boolean hasMore             = true;

		while (hasMore) {

			// will be set to true below if at least one result was processed
			hasMore = false;

			for (final NodeInterface t : this.nodeQuery(type).pageSize(Settings.FetchSize.getValue()).page(1).getAsList()) {

				cmd.execute(t);
				hasMore = true;
			}
		}
	}

	@Override
	public void delete(final NodeInterface node) throws FrameworkException {
		command(DeleteNodeCommand.class).execute(node);
	}

	@Override
	public RelationshipInterface create(final NodeInterface fromNode, final NodeInterface toNode, final String relType) throws FrameworkException {
		return command(CreateRelationshipCommand.class).execute(fromNode, toNode, relType);
	}

	@Override
	public RelationshipInterface create(final NodeInterface fromNode, final NodeInterface toNode, final String relType, final PropertyMap properties) throws FrameworkException {
		return command(CreateRelationshipCommand.class).execute(fromNode, toNode, relType, properties);
	}

	@Override
	public void delete(final RelationshipInterface relationship) {
		command(DeleteRelationshipCommand.class).execute(relationship);
	}

	@Override
	public NodeInterface getNodeById(final String uuid) throws FrameworkException {
		return getNodeById(null, uuid);
	}

	@Override
	public NodeInterface getNodeById(final String type, final String uuid) throws FrameworkException {

		if (uuid == null) {
			return null;
		}

		final Identity nodeId = getNodeFromCache(uuid);
		if (nodeId == null) {

			final Query<NodeInterface> query = nodeQuery();
			QueryGroup<NodeInterface> and    = query.and();

			and.uuid(uuid);

			// set type for faster query
			if (type != null) {
				and.type(type);
			}

			final NodeInterface entity = query.getFirst();
			if (entity != null) {

				final PropertyContainer container = entity.getPropertyContainer();

				nodeUuidMap.put(uuid, container.getId());
				return entity;
			}

		} else {

			try {

				final NodeInterface node = nodeFactory.instantiate(getDatabaseService().getNodeById(nodeId));
				if (node != null && node.is(type)) {

					return node;
				}

			} catch (NotFoundException ignore) {
				nodeUuidMap.remove(uuid);
			}
		}

		return null;
	}

	@Override
	public RelationshipInterface getRelationshipById(final String uuid) throws FrameworkException {
		return getRelationshipById(null, uuid);
	}

	@Override
	public RelationshipInterface getRelationshipById(final String type, final String uuid) throws FrameworkException {

		if (uuid == null) {
			return null;
		}

		final Identity id = getRelFromCache(uuid);
		if (id == null) {

			final QueryGroup<RelationshipInterface> query = relationshipQuery().and().uuid(uuid);

			// set type for faster query
			if (type != null) {

				query.type(type);

			} else {

				logger.warn("Relationship access by UUID can take a very long time. Please examine the following stack trace and amend.");
				Thread.dumpStack();
			}

			final RelationshipInterface entity = query.getFirst();
			if (entity != null) {

				final PropertyContainer container = entity.getPropertyContainer();

				relUuidMap.put(uuid, container.getId());
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
	public Query<NodeInterface> nodeQuery() {
		return command(SearchNodeCommand.class);
	}

	@Override
	public QueryGroup<NodeInterface> nodeQuery(final String type) {
		return command(SearchNodeCommand.class).and().type(type);
	}

	@Override
	public Query<RelationshipInterface> relationshipQuery() {
		return command(SearchRelationshipCommand.class);
	}

	@Override
	public QueryGroup<RelationshipInterface> relationshipQuery(final String type) {
		return command(SearchRelationshipCommand.class).and().type(type);
	}

	@Override
	public Tx tx() throws FrameworkException {
		return tx(true);
	}

	@Override
	public Tx tx(final boolean doValidation) throws FrameworkException {
		return tx(doValidation, true);
	}

	@Override
	public Tx tx(final boolean doValidation, final boolean doCallbacks) throws FrameworkException {
		return new Tx(securityContext, doValidation, doCallbacks).begin();
	}

	@Override
	public Tx tx(final boolean doValidation, final boolean doCallbacks, final boolean doNotifications) throws FrameworkException {
		return new Tx(securityContext, doValidation, doCallbacks, doNotifications).begin();
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
	public Iterable<GraphObject> query(final String nativeQuery, final Map<String, Object> parameters) throws FrameworkException {
		return Services.getInstance().command(securityContext, NativeQueryCommand.class).execute(nativeQuery, parameters);
	}

	@Override
	public <T extends Service> T getService(final Class<T> serviceClass) {
		return Services.getInstance().getService(serviceClass, "default");
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
	 * Constructs a new stateful App instance, initialized with a superuser security context
	 *
	 * @return superuser app instance
	 */
	public static App getInstance() {

		/*
		if (Thread.currentThread().isInterrupted()) {
			logger.info("Thread {} was interrupted, we could do something here...", Thread.currentThread().getName());
		}
		*/

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

		/*
		if (Thread.currentThread().isInterrupted()) {
			logger.info("Thread {} was interrupted, we could do something here...", Thread.currentThread().getName());
		}
		*/

		return new StructrApp(securityContext);
	}

	public static ConfigurationProvider getConfiguration() {
		/*
		if (Thread.currentThread().isInterrupted()) {
			logger.info("Thread {} was interrupted, we could do something here...", Thread.currentThread().getName());
		}
		*/

		return Services.getInstance().getConfigurationProvider();
	}

	public static <T extends GraphObject> URI getSchemaId(final Class<T> type) {
		return typeIdMap.get(type);
	}

	public static String resolveSchemaId(final URI uri) {
		return schemaIdMap.get(uri);
	}

	public static URI getSchemaBaseURI() {
		return schemaBaseURI;
	}

	public static void invalidate(final String uuid) {

		if (nodeUuidMap != null) {
			nodeUuidMap.remove(uuid);
		}

		if (relUuidMap != null) {
			relUuidMap.remove(uuid);
		}
	}

	/*
	public static <T> PropertyKey<T> key(final Class type, final String name, final boolean logMissing) {

		final ConfigurationProvider config = StructrApp.getConfiguration();
		PropertyKey<T> key                 = config.getPropertyKeyForJSONName(type, name, false);

		if (key == null) {

			// not found, next try: dynamic type
			final Class dynamicType = config.getNodeEntityClass(type.getSimpleName());
			if (dynamicType != null) {

				key = config.getPropertyKeyForJSONName(dynamicType, name, false);

			} else {

				final Class iface = config.getInterfaces().get(type.getSimpleName());
				if (iface != null) {

					key = config.getPropertyKeyForJSONName(iface, name, false);
				}
			}

			// store key in cache
			if (key != null) {

				config.setPropertyKeyForJSONName(type, name, key);
			}
		}

		if (key == null) {

			key = new GenericProperty(name);

			if (logMissing && !type.equals(GraphObjectMap.class)) {

				logger.warn("Unknown property key {}.{}! Using generic property key. This may lead to conversion problems. If you encounter problems please report the following source of the call.", type.getSimpleName(), name);

				try {

					// output for first stack trace element "above" this class
					for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {

						if (ste.getClassName().equals(Thread.class.getCanonicalName()) || ste.getClassName().equals(StructrApp.class.getCanonicalName())) {
							continue;
						}

						logger.warn("Source of this call: {}", ste.toString());
						break;
					}

				} catch (SecurityException se) {
					logger.warn("Unable to determine the stack source because the checkPermission flag is set.");
				}
			}
		}

		return key;
	}
	*/

	@Override
	public void invalidateCache(){

		if (nodeUuidMap != null) {
			nodeUuidMap.clear();
		}

		if (relUuidMap != null) {
			relUuidMap.clear();
		}

	}

	@Override
	public Map<String, Object> getAppContextStore() {
		return appContextStore;
	}

	@Override
	public String getInstanceId() throws FrameworkException {
		return StructrApp.INSTANCE_ID;
	}

	public static void initializeSchemaIds() {

		/*
		final Map<String, Class> interfaces                                = StructrApp.getConfiguration().getInterfaces();
		final Map<String, Class<? extends NodeInterface>> nodeTypes        = StructrApp.getConfiguration().getNodeEntities();
		final Map<String, Class<? extends RelationshipInterface>> relTypes = StructrApp.getConfiguration().getRelationshipEntities();

		// refresh schema IDs
		schemaIdMap.clear();
		typeIdMap.clear();

		for (final Class type : interfaces.values()) {

			// only register node types
			if (!type.getName().startsWith("org.structr.dynamic.")) {

				registerType(type);
			}
		}

		for (final Class type : nodeTypes.values()) {

			// only register node types
			if (!type.getName().startsWith("org.structr.dynamic.")) {

				registerType(type);
			}
		}

		for (final Class type : relTypes.values()) {

			// only register node types
			if (!type.getName().startsWith("org.structr.dynamic.")) {

				registerType(type);
			}
		}
		*/
	}

	// ----- private static methods -----
	private static void registerType(final String type) {

		final URI id = schemaBaseURI.resolve(URI.create(("definitions/" + type)));

		logger.debug("Registering type {} with {}", type, id);

		schemaIdMap.put(id, type);
		typeIdMap.put(type, id);
	}

	// ---------- private methods -----
	private synchronized Identity getNodeFromCache(final String uuid) {

		if (nodeUuidMap == null) {

			nodeUuidMap = new FixedSizeCache<>("Node UUID cache", Settings.UuidCacheSize.getValue());
		}

		return nodeUuidMap.get(uuid);
	}

	private synchronized Identity getRelFromCache(final String uuid) {

		if (relUuidMap == null) {

			relUuidMap = new FixedSizeCache<>("Relationship UUID cache", Settings.UuidCacheSize.getValue());
		}

		return relUuidMap.get(uuid);
	}
}
