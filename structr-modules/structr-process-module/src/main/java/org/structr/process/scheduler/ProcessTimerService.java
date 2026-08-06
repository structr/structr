/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.process.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.service.Command;
import org.structr.api.service.RunnableService;
import org.structr.api.service.ServiceDependency;
import org.structr.api.service.ServiceResult;
import org.structr.api.service.StopServiceForMaintenanceMode;
import org.structr.api.service.StructrServices;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.Traits;
import org.structr.process.ProcessTraits;
import org.structr.process.engine.ProcessEngine;
import org.structr.process.traits.definitions.ProcessTimerTraitDefinition;
import org.structr.schema.SchemaService;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Polls the graph for due {@link ProcessTimerTraitDefinition} nodes and fires
 * them via {@link ProcessEngine#fireTimer(NodeInterface)}. Each fire is a
 * separate transaction so a single failed timer does not block the rest.
 *
 * Polling granularity is 5 seconds by default. This is a deliberate compromise:
 * sub-second precision isn't realistic for BPMN timers (process modelling tends
 * to express timers in minutes / hours / days), and a 5s loop keeps the
 * database load light even with thousands of pending timers.
 *
 * Persistence: timers are graph nodes, so the queue survives restart. On
 * service start, the loop fires any timers whose {@code fireAt} elapsed during
 * downtime ("better late than never" semantics).
 *
 * To enable, add {@code ProcessTimerService} to {@code configured.services} in
 * structr.conf:
 * <pre>
 *   configured.services = NodeService SchemaService HttpService ... ProcessTimerService
 * </pre>
 */
@ServiceDependency(SchemaService.class)
@StopServiceForMaintenanceMode
public class ProcessTimerService extends Thread implements RunnableService {

	private static final Logger logger = LoggerFactory.getLogger(ProcessTimerService.class);

	private static final long POLL_INTERVAL_SECONDS = 5;

	// volatile: written by the service-manager thread (stopService/shutdown) and
	// read by the polling thread's loop; without it the poll thread may never
	// observe a stop request and keep firing timers after shutdown/maintenance.
	private volatile boolean doRun = false;

	public ProcessTimerService() {

		super("ProcessTimerService");
		this.setDaemon(true);
	}

	@Override
	public void run() {

		final Services servicesInstance = Services.getInstance();
		while (!servicesInstance.isInitialized()) {

			try { Thread.sleep(1000); } catch (InterruptedException ignored) { }
		}
		// Small extra delay so SchemaService has finished registering ProcessTimer.
		try { Thread.sleep(5000); } catch (InterruptedException ignored) { }

		logger.info("ProcessTimerService started, polling every {}s", POLL_INTERVAL_SECONDS);

		while (doRun) {

			try {
				pollAndFire();

			} catch (Throwable t) {

				logger.error("ProcessTimerService poll cycle failed: {}", t.getMessage(), t);
			}
			try { Thread.sleep(TimeUnit.SECONDS.toMillis(POLL_INTERVAL_SECONDS)); } catch (InterruptedException ignored) { }
		}
	}

	/**
	 * Find pending timers whose fireAt has elapsed and dispatch each.
	 * Each timer is fired in its own transaction so partial failures don't
	 * block the rest of the queue.
	 */
	private void pollAndFire() {

		final SecurityContext superUser = SecurityContext.getSuperUserInstance();
		final List<String> dueTimerIds = new LinkedList<>();

		// 1. Snapshot due timer IDs in one read transaction.
		try (final Tx tx = StructrApp.getInstance(superUser).tx()) {

			final App app = StructrApp.getInstance(superUser);
			final Traits timerTraits = Traits.of(ProcessTraits.PROCESS_TIMER);
			final Date now = new Date();

			final Iterable<NodeInterface> pending = app.nodeQuery(ProcessTraits.PROCESS_TIMER)
				.key(timerTraits.key(ProcessTimerTraitDefinition.STATUS_PROPERTY), ProcessTimerTraitDefinition.STATUS_PENDING)
				.getResultStream();

			for (final NodeInterface timer : pending) {

				final Date fireAt = timer.getProperty(timerTraits.key(ProcessTimerTraitDefinition.FIRE_AT_PROPERTY));
				if (fireAt != null && !fireAt.after(now)) {

					dueTimerIds.add(timer.getUuid());
				}
			}

			tx.success();

		} catch (FrameworkException ex) {

			logger.warn("ProcessTimerService: error querying due timers: {}", ex.getMessage());
			return;
		}

		if (dueTimerIds.isEmpty()) {
			return;
		}

		logger.debug("ProcessTimerService: firing {} due timer(s)", dueTimerIds.size());

		// 2. Fire each in its own transaction so a single bad timer doesn't block the rest.
		for (final String timerId : dueTimerIds) {
			fireOne(timerId);
		}
	}

	private void fireOne(final String timerId) {

		final SecurityContext superUser = SecurityContext.getSuperUserInstance();
		try (final Tx tx = StructrApp.getInstance(superUser).tx()) {

			final NodeInterface timer = StructrApp.getInstance(superUser).getNodeById(timerId);
			if (timer == null) {

				tx.success();
				return;
			}

			final ProcessEngine engine = new ProcessEngine(superUser);
			engine.fireTimer(timer);

			tx.success();

		} catch (Throwable t) {

			logger.warn("ProcessTimerService: failed to fire timer {}: {}", timerId, t.getMessage());
		}
	}

	// ----- RunnableService -----

	@Override
	public void startService() throws Exception {

		this.doRun = true;
		this.start();
	}

	@Override
	public void stopService() {
		this.doRun = false;
	}

	@Override
	public boolean runOnStartup() {
		return true;
	}

	@Override
	public boolean isRunning() {
		return doRun;
	}

	@Override
	public void injectArguments(final Command command) {
	}

	@Override
	public ServiceResult initialize(final StructrServices services, final String serviceName) {
		return new ServiceResult(true);
	}

	@Override
	public void initialized() {
	}

	@Override
	public void shutdown() {
		this.doRun = false;
	}

	@Override
	public boolean isVital() {
		return false;
	}

	@Override
	public boolean waitAndRetry() {
		return false;
	}

	@Override
	public String getModuleName() {
		return "process";
	}
}
