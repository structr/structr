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
package org.structr.cron;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.agent.Task;
import org.structr.api.service.Command;
import org.structr.api.service.RunnableService;
import org.structr.api.service.StructrServices;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.schema.action.Actions;

/**
 * A service that keeps track of registered tasks and runs
 * them at their scheduled time.
 *
 *
 */
public class CronService extends Thread implements RunnableService {

	private static final Logger logger           = LoggerFactory.getLogger(CronService.class.getName());

	public static final String   TASKS             = "CronService.tasks";
	public static final String   EXPRESSION_SUFFIX = ".cronExpression";
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

				if (entry.getDelayToNextExecutionInMillis() < GRANULARITY_UNIT.toMillis(GRANULARITY)) {

					final String taskClassName = entry.getName();
					final Class taskClass      = instantiate(taskClassName);

					try {

						if (taskClass != null) {

							Task task = (Task)taskClass.newInstance();

							logger.debug("Starting task {}", taskClassName);
							StructrApp.getInstance().processTasks(task);

						} else {

							try (final Tx tx = StructrApp.getInstance().tx()) {

								// check for schema method with the given name
								Actions.call(taskClassName, Collections.EMPTY_MAP);

								tx.success();
							}
						}

					} catch (Throwable t) {
						logger.warn("Exception while executing cron task {}: {}", taskClassName, t.getMessage());
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
	public void initialize(final StructrServices services, final Properties config) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		final String taskList = config.getProperty(TASKS, "");
		if (taskList != null) {

			for(String task : taskList.split("[ \\t]+")) {

				String expression = config.getProperty(task.concat(EXPRESSION_SUFFIX));
				if(expression != null) {

					CronEntry entry = CronEntry.parse(task, expression);
					if(entry != null) {

						logger.info("Adding cron entry {} for {}", new Object[]{ entry, task });

						cronEntries.add(entry);

					} else {

						logger.warn("Unable to parse cron expression for taks {}, ignoring.", task);
					}

				} else {

					logger.warn("No cron expression for task {}, ignoring.", task);
				}
			}
		}
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

	// ----- private methods -----
	private Class instantiate(final String taskClass) {

		try {

			return Class.forName(taskClass);

		} catch (Throwable ignore) {}

		return null;
	}
}
