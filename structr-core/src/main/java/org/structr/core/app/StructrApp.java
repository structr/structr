package org.structr.core.app;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.GraphObject;
import org.structr.core.Service;
import org.structr.core.Services;
import org.structr.agent.AgentService;
import org.structr.agent.Task;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
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
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.search.SearchCommand;
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
	
	private Map<Class<? extends Command>, Command> commandCache = new LinkedHashMap<>();
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
	public void delete(final NodeInterface node) throws FrameworkException {
		command(DeleteNodeCommand.class).execute(node);
	}
	
	@Override
	public <T extends Relation> T create(final NodeInterface fromNode, final NodeInterface toNode, final Class<T> relType) throws FrameworkException {
		return command(CreateRelationshipCommand.class).execute(fromNode, toNode, relType);
	}

	@Override
	public <T extends Relation> T create(final NodeInterface fromNode, final NodeInterface toNode, final Class<T> relType, final PropertyMap properties) throws FrameworkException {
		return command(CreateRelationshipCommand.class).execute(fromNode, toNode, relType, properties);
	}

	@Override
	public void delete(final RelationshipInterface relationship) throws FrameworkException {
		command(DeleteRelationshipCommand.class).execute(relationship);
	}

	@Override
	public NodeInterface get(final String uuid) throws FrameworkException {

		final Class<? extends SearchCommand> searchType = SearchNodeCommand.class;

		final Query<NodeInterface> query = new StructrQuery<>(securityContext, searchType);
		return query.uuid(uuid).getFirst();
	}

	@Override
	public <T extends GraphObject> T get(final Class<T> type, final String uuid) throws FrameworkException {

		final Class<? extends SearchCommand> searchType = SearchNodeCommand.class;

		final Query<T> query = new StructrQuery<>(securityContext, searchType);
		return query.type(type).getFirst();
	}
	
	@Override
	public <T extends GraphObject> List<T> get(final Class<T> type) throws FrameworkException {

		final Class<? extends SearchCommand> searchType = SearchNodeCommand.class;
		
		final Query<T> query = new StructrQuery<>(securityContext, searchType);
		return query.type(type).getAsList();
	}
	
	@Override
	public <T extends NodeInterface> Query<T> nodeQuery(final Class<T> type) {

		Query<T> query = new StructrQuery<>(securityContext, SearchNodeCommand.class);
		query.types(type);
		
		return query;
	}
	
	@Override
	public <T extends NodeInterface> Query<T> nodeQuery(final Class<T> type, final boolean inexact) {

		Query<T> query = new StructrQuery<>(securityContext, SearchNodeCommand.class);
		query.types(type, inexact);
		
		return query;
	}

	@Override
	public <T extends RelationshipInterface> Query<T> relationshipQuery(final Class<T> type) {

		Query<T> query = new StructrQuery<>(securityContext, SearchRelationshipCommand.class);
		query.and(AbstractRelationship.type, type.getSimpleName());
		
		return query;
	}
	
	@Override
	public void beginTx() {
		command(TransactionCommand.class).beginTx();
	}
	
	@Override
	public void commitTx() throws FrameworkException {
		command(TransactionCommand.class).commitTx(true);
	}
	
	@Override
	public void finishTx() {
		command(TransactionCommand.class).finishTx();
	}
	
	@Override
	public void shutdown() {
		Services.getInstance().shutdown();
	}

	@Override
	public <T extends Command> T command(Class<T> commandType) {
		
		Command command = commandCache.get(commandType);
		if (command == null) {
			
			command = Services.getInstance().command(securityContext, commandType);
			commandCache.put(commandType, command);
		}
		
		return (T)command;
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
		return Services.getInstance().getConfigurationValue(key, null);
	}
	
	public static String getConfigurationValue(final String key, final String defaultValue) {
		return Services.getInstance().getConfigurationValue(key, defaultValue);
	}
}
