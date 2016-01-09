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

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Command;
import org.structr.core.RunnableService;
import org.structr.core.Services;
import org.structr.agent.Task;
import org.structr.common.StructrConf;
import org.structr.core.app.StructrApp;

/**
 * A service that keeps track of registered tasks and runs
 * them at their scheduled time.
 *
 *
 */
public class CronService extends Thread implements RunnableService {

	private static final Logger logger           = Logger.getLogger(CronService.class.getName());

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
		while(!servicesInstance.isInitialized()) {
			try { Thread.sleep(1000); } catch(InterruptedException iex) { }
		}

		// sleep 5 seconds more
		try { Thread.sleep(5000); } catch(InterruptedException iex) { }

		while(doRun) {

			// sleep for some time
			try { Thread.sleep(GRANULARITY_UNIT.toMillis(GRANULARITY)); } catch(InterruptedException iex) { }

			for(CronEntry entry : cronEntries) {

				if(entry.getDelayToNextExecutionInMillis() < GRANULARITY_UNIT.toMillis(GRANULARITY)) {

					String taskClassName = entry.getName();

					try {
						Class taskClass = Class.forName(taskClassName);
						Task task = (Task)taskClass.newInstance();

						logger.log(Level.FINE, "Starting task {0}", taskClassName);
						StructrApp.getInstance().processTasks(task);

					} catch(Throwable t) {
						logger.log(Level.WARNING, "Could not start task {0}: {1}", new Object[] { taskClassName, t.getMessage() } );
					}
				}
			}
		}
	}

	// ----- interface RunnableService -----
	@Override
	public void startService() {
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
	public void initialize(final Services services, final StructrConf config) {

		final String taskList = config.getProperty(TASKS, "");
		if (taskList != null) {

			for(String task : taskList.split("[ \\t]+")) {

				String expression = config.getProperty(task.concat(EXPRESSION_SUFFIX));
				if(expression != null) {

					CronEntry entry = CronEntry.parse(task, expression);
					if(entry != null) {

						logger.log(Level.INFO, "Adding cron entry {0} for {1}", new Object[]{ entry, task });

						cronEntries.add(entry);

					} else {

						logger.log(Level.WARNING, "Unable to parse cron expression for taks {0}, ignoring.", task);
					}

				} else {

					logger.log(Level.WARNING, "No cron expression for task {0}, ignoring.", task);
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
}
