/**
 * Copyright (C) 2010-2016 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud.sync;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.structr.cloud.CloudHost;
import org.structr.cloud.CloudListener;
import org.structr.cloud.CloudService;
import org.structr.cloud.transmission.SingleTransmission;
import org.structr.common.SecurityContext;
import org.structr.common.StructrConf;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.RunnableService;
import org.structr.core.Services;
import org.structr.core.StructrTransactionListener;
import org.structr.core.TransactionSource;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.ModificationEvent;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;

/**
 *
 *
 */
public class SyncService extends Thread  implements RunnableService, StructrTransactionListener {

	private static final BlockingQueue<List<ModificationEvent>> syncQueue = new ArrayBlockingQueue<>(1000);
	private static final Logger logger                                    = Logger.getLogger(CloudService.class.getName());

	public enum SyncRole {
		master,
		slave
	};

	private final List<SyncHostInfo> syncHosts = new LinkedList<>();
	private boolean running                    = false;
	private boolean active                     = false;
	private String allowedMaster               = null;
	private SyncRole role                      = null;
	private int requiredSyncCount              = 0;
	private int retryInterval                  = 60;

	public SyncService() {

		super("SyncService");
		this.setDaemon(true);
	}

	@Override
	public void injectArguments(Command command) {
	}

	@Override
	public void initialize(final Services services, final StructrConf config) {

		active = "true".equals(config.getProperty("sync.enabled", "false"));

		if (active) {

			// initialize role and master
			role          = SyncRole.valueOf(config.getProperty("sync.role", "master"));
			allowedMaster = config.getProperty("sync.master", null);

			if (allowedMaster == null && SyncRole.slave.equals(role)) {
				throw new IllegalStateException("no master address set for this slave, please set sync.master in structr.conf.");
			}

			final String minimum = config.getProperty("sync.minimum", "1");
			final String retry   = config.getProperty("sync.retry", "60");
			final String hosts   = config.getProperty("sync.hosts");
			final String users   = config.getProperty("sync.users");
			final String pwds    = config.getProperty("sync.passwords");
			final String ports   = config.getProperty("sync.ports");

			// check only if we are a replication master
			if (SyncRole.master.equals(role)) {

				if (StringUtils.isEmpty(hosts)) {
					throw new IllegalStateException("no slave hosts set for this master, please set sync.hosts in structr.conf.");
				}

				if (StringUtils.isEmpty(users)) {
					throw new IllegalStateException("no slave users set for this master, please set sync.users in structr.conf.");
				}

				if (StringUtils.isEmpty(pwds)) {
					throw new IllegalStateException("no slave passwords set for this master, please set sync.passwords in structr.conf.");
				}

				if (StringUtils.isEmpty(ports)) {
					throw new IllegalStateException("no slave ports set for this master, please set sync.ports in structr.conf.");
				}

				final String[] remoteHosts = hosts != null ? hosts.split("[, ]+") : new String[0];
				final String[] remoteUsers = users != null ? users.split("[, ]+") : new String[0];
				final String[] remotePwds  = pwds  != null ? pwds.split("[, ]+")  : new String[0];
				final String[] remotePorts = ports != null ? ports.split("[, ]+") : new String[0];

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

					final SyncHostInfo syncHostInfo = new SyncHostInfo(host, user, pwd, port);
					syncHosts.add(syncHostInfo);

					logger.log(Level.INFO, "Adding slave host {0}, user {2}", new Object[] { syncHostInfo, port, user } );
				}

				try {
					// check and initialize sync hosts and policy
					initializeSyncHosts(minimum);

				} catch (FrameworkException fex) {
					fex.printStackTrace();
				}
			}

			if (StringUtils.isNotBlank(retry)) {
				this.retryInterval = Integer.valueOf(retry);
			}

			logger.log(Level.INFO, "Retry interval is set to {0} seconds", retryInterval);
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

				// wait to be notified when new data is available
				synchronized (syncQueue) {

					while (syncQueue.isEmpty()) {
						syncQueue.wait();
					}
				}

				// load the head of the queue without removing it
				// (will be removed later if sync was successful)
				final List<ModificationEvent> transaction = syncQueue.peek();
				if (transaction != null) {

					// define success as "at least one sync process was successful"
					final SyncListener successListener = new SyncListener(requiredSyncCount);

					final SyncTransmission transmission = new SyncTransmission(transaction);

					for (final SyncHostInfo info : syncHosts) {

						try {

							CloudService.doRemote(SecurityContext.getSuperUserInstance(), transmission, info, successListener);

						} catch (FrameworkException fex) {
							logger.log(Level.WARNING, "Unable to synchronize with host {0}: {1}", new Object[] { info, fex.getMessage() } );
						}
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
	public void beforeCommit(final SecurityContext securityContext, final Collection<ModificationEvent> modificationEvents, final TransactionSource source) throws FrameworkException {

		// prevent all (!) transactions from being committed on slave instances
		if (SyncRole.slave.equals(role) && (source == null || !allowedMaster.equals(source.getOriginAddress()))) {

			if (modificationEvents != null && !modificationEvents.isEmpty()) {

				throw new FrameworkException(500, "Illegal write transaction on active slave.");
			}
		}
	}

	@Override
	public void afterCommit(final SecurityContext securityContext, final Collection<ModificationEvent> modificationEvents, final TransactionSource source) {

		if (source != null && source.isRemote()) {
			return;
		}

		// only react if desired
		if (active && running && !modificationEvents.isEmpty()) {

			try {
				// store last sync timestamp for the given instance ID
				final App app = StructrApp.getInstance();
				app.setGlobalSetting(app.getInstanceId() + ".lastModified", System.currentTimeMillis());

			} catch (FrameworkException fex) {

				logger.log(Level.SEVERE, "Unable to store last modified date for current instance.", fex);
			}


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
	private void initializeSyncHosts(final String minimum) throws FrameworkException {

		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		final String instanceId           = StructrApp.getInstance().getInstanceId();

		// check connection and version of sync host
		for (Iterator<SyncHostInfo> it = syncHosts.iterator(); it.hasNext();) {

			final SyncHostInfo host = it.next();
			boolean reachable       = true;

			try {

				final SingleTransmission<ReplicationStatus> transmission = new SingleTransmission<>(new ReplicationStatus(instanceId));
				final ReplicationStatus status = CloudService.doRemote(SecurityContext.getSuperUserInstance(), transmission, host, new LoggingListener());
				if (status != null) {

					final String slaveId = status.getSlaveId();
					if (slaveId != null ) {

						final long syncTimestamp  = status.getLastSync();
						String lastSyncString     = "not synced yet";

						if (syncTimestamp != 0L) {

							lastSyncString = "last sync was " + dateFormat.format(syncTimestamp);
						}

						logger.log(Level.INFO, "Determined instance ID of {0} to be {1}, {2}.", new Object[] { host, slaveId, lastSyncString } );

						// store replication status in host info
						host.setReplicationStatus(status);

					} else {

						reachable = false;
					}

				} else {

					reachable = false;
				}

			} catch (Throwable t) {

				t.printStackTrace();
				reachable = false;
			}

			if (!reachable) {

				logger.log(Level.WARNING, "Synchronization slave {0} not reachable, removing from list.", host);
				it.remove();
			}
		}

		// check number of synchronization hosts
		final int numSyncHosts  = syncHosts.size();
		requiredSyncCount       = Integer.valueOf(minimum);

		if (numSyncHosts < requiredSyncCount) {
			throw new IllegalStateException("synchronization policy requires at least " + requiredSyncCount + " hosts, but only " + numSyncHosts + " are reachable.");
		}

		logger.log(Level.INFO, "Synchronization to {0} host{1} required.", new Object[] { requiredSyncCount, requiredSyncCount == 1 ? "" : "s" } );


		// prepare synchronization hosts
		for (final SyncHostInfo host : syncHosts) {

			// try to copy database contents to synchronization slave
			checkAndInitializeSyncHost(host);
		}
	}

	private void checkAndInitializeSyncHost(final SyncHostInfo host) throws FrameworkException {

		final String masterId         = StructrApp.getInstance().getInstanceId();
		final SimpleDateFormat df     = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		final long localSyncTimestamp = StructrApp.getInstance().getGlobalSetting(masterId + ".lastModified", 0L);

		if (localSyncTimestamp == 0L) {

			// no synchronization with this slave yet, clear and initialize slave database
			synchronizeSlave(host);

		} else {

			// there has been a synchronization in the past
			if (host.getLastSyncTimestamp() != localSyncTimestamp) {

				logger.log(Level.INFO, "Replication host {0} is out of sync, last remote update was {1} whereas last local update was {2}",
					new Object[] { host,  df.format(host.getLastSyncTimestamp()), df.format(localSyncTimestamp) }
				);

				// clear and re-initialize slave database..
				synchronizeSlave(host);

			} else {

				logger.log(Level.INFO, "Replication host {0} is in sync, last update was {1}", new Object[] { host, df.format(localSyncTimestamp) } );
			}
		}
	}

	private void synchronizeSlave(final SyncHostInfo info) {

		logger.log(Level.INFO, "Establishing initial replication.");

		try (final Tx tx = StructrApp.getInstance().tx()) {

			CloudService.doRemote(SecurityContext.getSuperUserInstance(), new UpdateTransmission(), info, new LoggingListener());

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		logger.log(Level.INFO, "Done.");
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
		public void transmissionProgress(final String message) {
		}

		public boolean wasSuccessful() {
			return successCount >= requiredSuccessCount;
		}
	}

	private static class SyncHostInfo implements CloudHost {

		private ReplicationStatus status = null;
		private String instanceId        = null;
		private String host              = null;
		private String user              = null;
		private String pwd               = null;
		private int port                 = -1;

		public SyncHostInfo(final String host, final String user, final String pwd, final String portSource) {

			this.host = host;
			this.user = user;
			this.pwd  = pwd;
			this.port = Integer.valueOf(portSource);
		}

		@Override
		public String toString() {
			return host + ":" + port;
		}

		@Override
		public String getHostName() {
			return host;
		}

		@Override
		public String getUserName() {
			return user;
		}

		@Override
		public String getPassword() {
			return pwd;
		}

		@Override
		public int getPort() {
			return port;
		}

		public void setReplicationStatus(final ReplicationStatus status) {

			this.instanceId = status.getSlaveId();
			this.status     = status;
		}

		public long getLastSyncTimestamp() {
			return status.getLastSync();
		}

		public String getInstanceId() {
			return instanceId;
		}
	}

	private class LoggingListener implements CloudListener {

		@Override
		public void transmissionStarted() {
			logger.log(Level.INFO, "Transmission started");
		}

		@Override
		public void transmissionFinished() {
			logger.log(Level.INFO, "Transmission finished");
		}

		@Override
		public void transmissionAborted() {
			logger.log(Level.INFO, "Transmission aborted");
		}

		@Override
		public void transmissionProgress(final String message) {
			logger.log(Level.INFO, "Transmission progress {0}", new Object[] { message } );
		}
	}
}
