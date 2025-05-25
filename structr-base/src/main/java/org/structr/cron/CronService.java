/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.cron;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.agent.Task;
import org.structr.api.config.Setting;
import org.structr.api.config.Settings;
import org.structr.api.service.*;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.schema.SchemaService;
import org.structr.schema.action.Actions;

import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

/**
 * A service that keeps track of registered tasks and runs
 * them at their scheduled time.
 *
 *
 */
@ServiceDependency(SchemaService.class)
@StopServiceForMaintenanceMode
public class CronService extends Thread implements RunnableService {

	private static final Logger logger           = LoggerFactory.getLogger(CronService.class.getName());

	public static final String   EXPRESSION_SUFFIX = "cronExpression";
	public static final TimeUnit GRANULARITY_UNIT  = TimeUnit.SECONDS;
	public static final long     GRANULARITY       = 1;
	public static final int      NUM_FIELDS        = 6;

	private LinkedList<CronEntry> cronEntries = new LinkedList<>();
	private boolean doRun = false;

	public CronService() {
		super("CronService");
		this.setDaemon(true);
	}

	@Override
	public void run() {

		final Services servicesInstance = Services.getInstance();

		// wait for service layer to be initialized
		while (!servicesInstance.isInitialized()) {
			try { Thread.sleep(1000); } catch(InterruptedException iex) { }
		}

		// sleep 5 seconds more
		try { Thread.sleep(5000); } catch(InterruptedException iex) { }

		while (doRun) {

			// sleep for some time
			try { Thread.sleep(GRANULARITY_UNIT.toMillis(GRANULARITY)); } catch(InterruptedException iex) { }

			for (CronEntry entry : cronEntries) {

				if (entry.shouldExecuteNow()) {

					final String taskClassName = entry.getName();
					final Class taskClass      = instantiate(taskClassName);

					if (entry.isRunning() && Settings.CronAllowParallelExecution.getValue() == false) {
						logger.warn("Prevented parallel execution of '{}' - if this happens regularly you should consider adjusting the cronExpression!", taskClassName);
					} else {

						new Thread(new Runnable() {

							@Override
							public void run() {

								try {

									entry.incrementRunCount();

									RuntimeEventLog.cron(taskClassName);

									if (taskClass != null) {

										Task task = (Task)taskClass.getDeclaredConstructor().newInstance();

										logger.debug("Starting task {}", taskClassName);
										StructrApp.getInstance().processTasks(task);

									} else {

										SecurityContext superUserSecurityContext = SecurityContext.getSuperUserInstance();

										try (final Tx tx = StructrApp.getInstance(superUserSecurityContext).tx()) {

											// check for schema method with the given name
											Actions.callWithSecurityContext(taskClassName, superUserSecurityContext, Collections.EMPTY_MAP);

											tx.success();
										}
									}

								} catch (FrameworkException fex) {

									logger.warn("Exception while executing cron task {}: {}", taskClassName, fex.toString());

								} catch (Throwable t) {

									logger.warn("Exception while executing cron task {}: {}", taskClassName, t.getMessage());

								} finally {

									entry.decrementRunCount();
								}
							}
						}).start();
					}
				}
			}
		}
	}

	// ----- interface RunnableService -----
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
	public void injectArguments(Command command) {
	}

	@Override
	public ServiceResult initialize(final StructrServices services, String serviceName) throws ReflectiveOperationException {

		final String taskList = Settings.CronTasks.getValue();
		if (StringUtils.isNotBlank(taskList)) {

			for(String task : taskList.split("[ \\t]+")) {

				if (StringUtils.isNotBlank(task)) {

					final Setting cronSetting = Settings.getCaseSensitiveSetting(task, EXPRESSION_SUFFIX);
					if (cronSetting != null) {

						CronEntry entry = CronEntry.parse(task, cronSetting.getValue().toString());
						if(entry != null) {

							logger.info("Adding cron entry {} for '{}'", entry, task);

							cronEntries.add(entry);

						} else {

							logger.warn("Unable to parse cron expression for task '{}', ignoring.", task);
						}

					} else {

						logger.warn("No cron expression for task '{}', ignoring.", task);
					}
				}
			}
		}

		return new ServiceResult(true);
	}

	@Override
	public void initialized() {}

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

	// ----- private methods -----
	private Class instantiate(final String taskClass) {

		try {

			return Class.forName(taskClass);

		} catch (Throwable ignore) {}

		return null;
	}

	// ----- interface Feature -----
	@Override
	public String getModuleName() {
		return "cron";
	}
}
