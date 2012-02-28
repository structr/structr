/*
 *  Copyright (C) 2011 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.core.cron;

import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.Command;
import org.structr.core.RunnableService;
import org.structr.core.Services;
import org.structr.core.agent.ProcessTaskCommand;
import org.structr.core.agent.Task;

/**
 * A service that keeps track of registered tasks and runs
 * them at their scheduled time.
 *
 * @author Christian Morgner
 */
public class CronService extends Thread implements RunnableService {

	private static final Logger logger           = Logger.getLogger(CronService.class.getName());

	public static final String   TASKS             = "CronService.tasks";
	public static final String   EXPRESSION_SUFFIX = ".cronExpression";
	public static final TimeUnit GRANULARITY_UNIT  = TimeUnit.SECONDS;
	public static final long     GRANULARITY       = 1;
	public static final int      NUM_FIELDS        = 6;

	private SecurityContext securityContext   = SecurityContext.getSuperUserInstance();
	private DelayQueue<CronEntry> cronEntries = new DelayQueue<CronEntry>();
	private boolean doRun = false;

	public CronService() {
		super("CronService");
	}

	@Override
	public void run() {
		
		while(doRun) {

			try {
				CronEntry entry = cronEntries.poll(GRANULARITY, GRANULARITY_UNIT);
				if(entry != null) {
					
					String taskClassName = entry.getName();
					try {
						Class taskClass = Class.forName(taskClassName);
						Task task = (Task)taskClass.newInstance();

						logger.log(Level.FINE, "Starting task {0}", taskClassName);
						Services.command(securityContext, ProcessTaskCommand.class).execute(task);

						// re-insert entry
						cronEntries.add(entry);

						logger.log(Level.FINEST, "Task {0} re-inserted, queue now contains {1} entries.", new Object[] {  taskClassName, cronEntries.size() } );
						
						// wait one step to avoid multiple tasks to be started
						Thread.sleep(GRANULARITY_UNIT.toMillis(GRANULARITY));

					} catch(Throwable t) {
						logger.log(Level.WARNING, "Could not start task {0}: {1}", new Object[] { taskClassName, t.getMessage() } );
					}

				} else {

					logger.log(Level.FINE, "No entry available in queue, queue size: {0}", cronEntries.size());

					CronEntry e = cronEntries.peek();
					if(e != null) {
						logger.log(Level.FINEST, "Expiry of next entry: {0}", e.getDelay(TimeUnit.MILLISECONDS));
					}
				}

			} catch(InterruptedException irex) {
				// who's there? :)
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
	public void initialize(Map<String, Object> context) {

		String taskList = Services.getConfigValue(context, TASKS, "");
		String[] tasks = taskList.split("[ \\t]+");

		for(String task : tasks) {

			String expression = (String)context.get(task.concat(EXPRESSION_SUFFIX));
			if(expression != null) {

				CronEntry entry = CronEntry.parse(task, expression);
				if(entry != null) {

					logger.log(Level.INFO, "Adding cron entry {0}", entry);
					
					cronEntries.add(entry);

				} else {
					logger.log(Level.WARNING, "Unable to parse cron expression for taks {0}, ignoring.", task);
				}

			} else {
				logger.log(Level.WARNING, "No cron expression for task {0}, ignoring.", task);
			}
		}
	}

	@Override
	public void shutdown() {
		this.doRun = false;
	}
}
