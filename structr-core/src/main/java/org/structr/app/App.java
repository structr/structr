package org.structr.app;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;

/**
 * Stateful facade for accessing the Structr core layer.
 * 
 * @author Christian Morgner
 */
public class App {

	private static final Logger logger = Logger.getLogger(App.class.getName());
	
	private Map<Class<? extends Command>, Command> commandCache = new LinkedHashMap<>();
	private SecurityContext securityContext                     = null;
	
	private App(final String basePath, final SecurityContext securityContext) {
		
		this.securityContext = securityContext;
		
		if (!Services.isInitialized()) {

			final Map<String, String> context = new LinkedHashMap<>();

			logger.log(Level.INFO, "Initializing Structr with base path {0}..", basePath);

			context.put(Services.CONFIGURED_SERVICES, "ModuleService NodeService");
			context.put(Services.TMP_PATH,          "/tmp/");
			context.put(Services.BASE_PATH,         basePath);
			context.put(Services.DATABASE_PATH,     basePath + "/db");
			context.put(Services.FILES_PATH,        basePath + "/files");
			context.put(Services.LOG_DATABASE_PATH, basePath + "/logDb.dat");

			Services.initialize(context);
			
			// wait for initialization
			while (!Services.isInitialized()) {
				
				try { Thread.sleep(100); } catch (Throwable t) {}
			}
			
			// register shutdown hook
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

				@Override
				public void run() {
					
					Services.shutdown();
				}
				
			}));
			
			logger.log(Level.INFO, "Initialization done.");
		}
	}
	
	// ----- public methods -----
	public <T extends NodeInterface> T create(final Class<T> type, final String name) throws FrameworkException {
		return create(type, new NodeAttribute(AbstractNode.name, name));
	}
	
	public <T extends NodeInterface> T create(final Class<T> type, final NodeAttribute<?>... attributes) throws FrameworkException {
		
		final CreateNodeCommand<T> cmd = command(CreateNodeCommand.class);
		final TransactionCommand tx    = command(TransactionCommand.class);
		
		return tx.execute(new StructrTransaction<T>() {

			@Override
			public T execute() throws FrameworkException {

				List<NodeAttribute<?>> attrs = new LinkedList<>(Arrays.asList(attributes));
				attrs.add(new NodeAttribute(AbstractNode.type, type.getSimpleName()));

				return cmd.execute(attrs);
			}
		});
		
	}
	
	public <T extends NodeInterface> List<T> findNodes(final Class<T> type, final SearchAttribute<?>... attributes) throws FrameworkException {
		
		final SearchNodeCommand<T> cmd = command(SearchNodeCommand.class);
		final TransactionCommand tx                = command(TransactionCommand.class);
		
		return tx.execute(new StructrTransaction<List<T>>() {

			@Override
			public List<T> execute() throws FrameworkException {
				
				List<SearchAttribute<?>> searchAttrs = new LinkedList<>(Arrays.asList(attributes));
				searchAttrs.add(Search.andExactType(type));
				
				Result<T> result = cmd.execute(searchAttrs);
				
				return result.getResults();
				
				
			}
		});
		
	}
	
	public void beginTx() {
		
		// begins a new transaction
		
		// forces the previous transaction to be committed
		
		// should setProperty calls be done in their own transactions?
		
		
	}
	
	public void commitTx() throws FrameworkException {
		
		// commits the transaction that is currently registered
		// in this app context
		
		// should we support "auto-commit"?
		
		// a transaction that is not finished yet must be committed
		// before the next getInstance() call (?)
	}
	
	public void shutdown() {
		Services.shutdown();
	}
	
	
	// ----- public static methods ----
	/**
	 * Constructs a new stateful App instance, initialized with the given
	 * security context.
	 * 
	 * @param basePath
	 * @param securityContext
	 * @return 
	 */
	public static App getInstance(final String basePath, final SecurityContext securityContext) {
		return new App(basePath, securityContext);
	}
	
	/**
	 * Constructs a new stateful App instance, initialized with the given
	 * username and password.
	 * 
	 * @param basePath
	 * @param username
	 * @param password
	 * @return
	 * @throws FrameworkException 
	 */
	public static App getInstance(final String basePath, final String username, final String password) throws FrameworkException {
		
		final Principal user = null;
		final SecurityContext securityContext = SecurityContext.getInstance(user, AccessMode.Backend);
		
		return new App(basePath, securityContext);
	}
	
	/**
	 * Constructs a new stateful App instance, initialized with a
	 * super user context.
	 * 
	 * @param basePath
	 * @return 
	 */
	public static App getInstance(final String basePath) {
		return new App(basePath, SecurityContext.getSuperUserInstance());
	}
	
	// ----- private methods -----
	private <T extends Command> T command(Class<T> commandType) {
		
		Command command = commandCache.get(commandType);
		if (command == null) {
			
			command = Services.command(securityContext, commandType);
			commandCache.put(commandType, command);
		}
		
		return (T)command;
	}
}
