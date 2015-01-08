package org.structr.cloud.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.cloud.CloudService;
import org.structr.common.SecurityContext;
import org.structr.common.StructrConf;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.RunnableService;
import org.structr.core.StructrTransactionListener;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.ModificationEvent;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;

/**
 *
 * @author Christian Morgner
 */
public class SyncService extends Thread  implements RunnableService, StructrTransactionListener {

	private static final Logger logger                                    = Logger.getLogger(CloudService.class.getName());
	private static final BlockingQueue<List<ModificationEvent>> syncQueue = new ArrayBlockingQueue<>(1000);
	private boolean running                                               = false;
	private boolean active                                                = false;
	private String remoteHost                                             = null;
	private String remoteUser                                             = null;
	private String remotePwd                                              = null;
	private int remotePort                                                = 54555;

	public SyncService() {

		super("SyncService");
		this.setDaemon(true);
	}

	@Override
	public void injectArguments(Command command) {
	}

	@Override
	public void initialize(final StructrConf config) {

		active     = "true".equals(config.getProperty("sync.enabled", "false"));
		remoteHost = config.getProperty("sync.host", "localhost");
		remoteUser = config.getProperty("sync.user", "admin");
		remotePwd  = config.getProperty("sync.password", "admin");
		remotePort = Integer.valueOf(config.getProperty("sync.port", "54556"));

	}

	@Override
	public void initialized() {}

	@Override
	public void shutdown() {
		running = false;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public void startService() {

		TransactionCommand.registerTransactionListener(this);

		running = true;
		start();

		logger.log(Level.INFO, "SyncService successfully started.");
	}

	@Override
	public void run() {

		while (running) {

			try {

				final List<ModificationEvent> transaction = syncQueue.take();
				if (transaction != null) {

					try (final Tx tx = StructrApp.getInstance().tx()) {

						CloudService.doRemote(new SyncTransmission(transaction, remoteUser, remotePwd, remoteHost, remotePort), null);

						tx.success();

					} catch (FrameworkException fex) {
						fex.printStackTrace();
					}
				}

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	@Override
	public void stopService() {
		shutdown();
	}

	@Override
	public boolean runOnStartup() {
		return true;
	}


	@Override
	public boolean isVital() {
		return false;
	}

	// ----- interface StructrTransactionListener -----
	@Override
	public void transactionCommited(final SecurityContext securityContext, final List<ModificationEvent> modificationEvents) {

		// only react if desired
		if (active && running && !modificationEvents.isEmpty()) {

			// copy all modification events and return quickly
			syncQueue.add(new ArrayList<>(modificationEvents));

		}
	}
}
