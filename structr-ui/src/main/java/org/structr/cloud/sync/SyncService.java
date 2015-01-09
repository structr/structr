package org.structr.cloud.sync;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.structr.cloud.CloudListener;
import org.structr.cloud.CloudService;
import org.structr.cloud.message.Ping;
import org.structr.cloud.transmission.SingleTransmission;
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

	private static final BlockingQueue<List<ModificationEvent>> syncQueue = new ArrayBlockingQueue<>(1000);
	private static final Logger logger                                    = Logger.getLogger(CloudService.class.getName());

	private final List<SyncHostInfo> syncHosts = new LinkedList<>();
	private boolean running                    = false;
	private boolean active                     = false;
	private int requiredSyncCount              = 0;
	private int retryInterval                  = 60;
	private long timeout                       = 0;

	public SyncService() {

		super("SyncService");
		this.setDaemon(true);
	}

	@Override
	public void injectArguments(Command command) {
	}

	@Override
	public void initialize(final StructrConf config) {

		active = "true".equals(config.getProperty("sync.enabled", "false"));

		if (active) {

			final String minimum = config.getProperty("sync.minimum", "1");
			final String retry   = config.getProperty("sync.retry", "60");
			final String hosts   = config.getProperty("sync.hosts");
			final String users   = config.getProperty("sync.users");
			final String pwds    = config.getProperty("sync.passwords");
			final String ports   = config.getProperty("sync.ports");

			if (StringUtils.isEmpty(hosts)) {
				throw new IllegalStateException("no sync hosts set but service is enabled, please set sync.hosts in structr.conf.");
			}

			if (StringUtils.isEmpty(users)) {
				throw new IllegalStateException("no sync users set but service is enabled, please set sync.users in structr.conf.");
			}

			if (StringUtils.isEmpty(pwds)) {
				throw new IllegalStateException("no sync passwords set but service is enabled, please set sync.passwords in structr.conf.");
			}

			if (StringUtils.isEmpty(ports)) {
				throw new IllegalStateException("no sync ports set but service is enabled, please set sync.ports in structr.conf.");
			}

			if (StringUtils.isNotBlank(retry)) {

				this.retryInterval = Integer.valueOf(retry);
			}

			logger.log(Level.INFO, "Retry interval is set to {0} seconds", retryInterval);

			final String[] remoteHosts = hosts.split("[, ]+");
			final String[] remoteUsers = users.split("[, ]+");
			final String[] remotePwds  = pwds.split("[, ]+");
			final String[] remotePorts = ports.split("[, ]+");

			String previousUser = null;
			String previousPwd  = null;
			String previousPort = null;

			for (int i=0; i<remoteHosts.length; i++) {

				final String host = remoteHosts[i];

				final String user = remoteUsers.length > i ? remoteUsers[i] : previousUser;
				final String pwd  = remotePwds.length > i ?  remotePwds[i]   : previousPwd;
				final String port = remotePorts.length > i ? remotePorts[i] : previousPort;

				previousUser = user;
				previousPwd  = pwd;
				previousPort = port;

				if (StringUtils.isEmpty(user)) {
					throw new IllegalStateException("no sync user found for remote host " + host + ", please set sync.users in structr.conf.");
				}

				if (StringUtils.isEmpty(pwd)) {
					throw new IllegalStateException("no sync password found for remote host " + host + ", please set sync.passwords in structr.conf.");
				}

				if (StringUtils.isEmpty(port)) {
					throw new IllegalStateException("no sync port found for remote host " + host + ", please set sync.ports in structr.conf.");
				}

				logger.log(Level.INFO, "Adding synchronization host {0}:{1}, user {2}", new Object[] { host, port, user } );

				syncHosts.add(new SyncHostInfo(host, user, pwd, port));
			}

			// check and initialize sync hosts and policy
			initializeSyncHosts(minimum);
		}
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

				synchronized (syncQueue) {

					while (syncQueue.isEmpty()) {
						syncQueue.wait();
					}
				}

				final List<ModificationEvent> transaction = syncQueue.peek();
				if (transaction != null) {

					// define success as "at least one sync process was successful"
					final SyncListener successListener = new SyncListener(requiredSyncCount);

					try (final Tx tx = StructrApp.getInstance().tx()) {

						final SyncTransmission transmission = new SyncTransmission(transaction);

						for (final SyncHostInfo info : syncHosts) {

							try {

								CloudService.doRemote(transmission, info.getUser(), info.getPwd(), info.getHost(), info.getPort(), successListener);

							} catch (FrameworkException fex) {
								logger.log(Level.WARNING, "Unable to synchronize with host {0}: {1}", new Object[] { info.getHost(), fex.getMessage() } );
							}
						}

						tx.success();

					} catch (FrameworkException fex) {

						// should not happen, and should output a stracktrace if it happens
						fex.printStackTrace();
					}

					// remove sync changeset from queue when sync
					// was successful (see above)
					if (successListener.wasSuccessful()) {

						syncQueue.remove(transaction);

					} else {

						logger.log(Level.WARNING, "Unable to synchronize with required number of hosts, retrying in {0} seconds..", retryInterval);

						// sleep
						try { Thread.sleep(retryInterval * 1000); } catch (Throwable t) {}
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
		return true;
	}

	// ----- interface StructrTransactionListener -----
	@Override
	public void transactionCommited(final SecurityContext securityContext, final List<ModificationEvent> modificationEvents) {

		// only react if desired
		if (active && running && !modificationEvents.isEmpty()) {

			try {
				// copy all modification events and return quickly
				syncQueue.put(new ArrayList<>(modificationEvents));

				// notify sync queue of new input
				synchronized (syncQueue) { syncQueue.notify(); }

			} catch (InterruptedException iex) {
				iex.printStackTrace();
			}

		}
	}

	// ----- private methods -----
	private void initializeSyncHosts(final String minimum) {

		final int numSyncHosts = syncHosts.size();
		requiredSyncCount      = Integer.valueOf(minimum);

		if (numSyncHosts < requiredSyncCount) {
			throw new IllegalStateException("synchronization policy requires at least " + requiredSyncCount + " hosts, but only " + numSyncHosts + " are set.");
		}

		logger.log(Level.INFO, "Synchronization to {0} host{1} required.", new Object[] { requiredSyncCount, requiredSyncCount == 1 ? "" : "s" } );

		// check connection and version of sync host
		for (final SyncHostInfo host : syncHosts) {

			try {

				final Ping ping = CloudService.doRemote(new SingleTransmission<>(new Ping()), host.getUser(), host.getPwd(), host.getHost(), host.getPort(), null);
				if (ping == null) {

					// flow will not reach this point when the protocol versions mismatch
					throw new IllegalStateException("synchronization server " + host.getHost() + " not available.");
				}

			} catch (FrameworkException ex) {

				throw new IllegalStateException("synchronization server " + host.getHost() + " not available.");
			}
		}
	}

	// ----- nested classes -----
	private static class SyncListener implements CloudListener {

		private int successCount         = 0;
		private int requiredSuccessCount = 0;

		public SyncListener(final int requiredSuccessCount) {
			this.requiredSuccessCount = requiredSuccessCount;
		}

		@Override
		public void transmissionStarted() {
		}

		@Override
		public void transmissionFinished() {
			successCount++;
		}

		@Override
		public void transmissionAborted() {
		}

		@Override
		public void transmissionProgress(int current, int total) {
		}

		public boolean wasSuccessful() {
			return successCount >= requiredSuccessCount;
		}
	}

	private static class SyncHostInfo {

		private long timeout = 0;
		private String host = null;
		private String user = null;
		private String pwd  = null;
		private int port    = -1;

		public SyncHostInfo(final String host, final String user, final String pwd, final String portSource) {

			this.host = host;
			this.user = user;
			this.pwd  = pwd;
			this.port = Integer.valueOf(portSource);
		}

		public String getHost() {
			return host;
		}

		public String getUser() {
			return user;
		}

		public String getPwd() {
			return pwd;
		}

		public int getPort() {
			return port;
		}

		public long getTimeout() {
			return timeout;
		}
	}
}
