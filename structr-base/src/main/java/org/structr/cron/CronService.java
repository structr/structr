/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;
import org.structr.core.api.NamedArguments;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.traits.Traits;
import org.structr.docs.*;
import org.structr.schema.SchemaService;
import org.structr.schema.action.Actions;
import org.structr.schema.action.EvaluationHints;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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

										final SecurityContext superUserSecurityContext = SecurityContext.getSuperUserInstance();

										try (final Tx tx = StructrApp.getInstance(superUserSecurityContext).tx()) {

											if (!taskClassName.contains(".")) {

												// check for user-defined function with the given name
												Actions.callWithSecurityContext(taskClassName, superUserSecurityContext, Collections.EMPTY_MAP);

											} else {

												final String[] parts = taskClassName.split("\\.");

												if (parts.length == 2) {

													final String typeName   = parts[0];
													final String methodName = parts[1];

													if (Traits.exists(typeName)) {

														final AbstractMethod method = Methods.resolveMethod(Traits.of(typeName), methodName);

														if (method != null) {

															method.execute(superUserSecurityContext, null,  new NamedArguments(), new EvaluationHints());

														} else {

															logger.warn("Unable to run cron task '{}'. No method '{}' found on type/service class '{}'!", taskClassName, methodName, typeName);
														}

													} else {

														logger.warn("Unable to run cron task '{}'. No type/service class '{}' found!", taskClassName, typeName);
													}

												} else {

													logger.warn("Unable to run cron task '{}'. Expected only 2 parts!", taskClassName);
												}
											}

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

	public static Documentable getDocumentation() {

		return new Documentable() {

			@Override
			public DocumentableType getDocumentableType() {
				return DocumentableType.Service;
			}

			@Override
			public String getName() {
				return "CronService";
			}

			@Override
			public String getShortDescription() {
				return "This service allows you to schedule periodic execution of built-in functions based on a pattern similar to the \"cron\" daemon on UNIX systems.";
			}

			@Override
			public String getLongDescription() {
				return """
				### How It Works
				Scheduled tasks for the CronService are configured in `structr.conf`. The main configuration key is `CronService.tasks`. It accepts a whitespace-separated list of user-defined function names. These are the tasks that are registered with the CronService.

				For each of those tasks, a `cronExpression` entry has to be configured which determines the execution time/date of the task. It consists of seven fields and looks similar to crontab entries in Unix-based operating systems:
				
				```
				<methodName>.cronExpression = <s> <m> <h> <dom> <m> <dow>
				```
				
				| Field | Explanation | Value Range |
				| --- | --- | --- |
				| `<methodName>` | name of the user-defined function | any existing user-defined function |
				| `<s>` | seconds of the minute | 0-59 |
				| `<m>` | minute of the hour | 0-59 |
				| `<h>` | hour of the day | 0-23 |
				| `<dom>` | day of the month | 0-31 |
				| `<m>` | month of the year | 1-12 |
				| `<dow>` | day of the week | 0-6 (0 = Sunday) |
				
				There are several supported notations for the fields:
				
				| Notation | Meaning |
				| --- | --- |
				| * | Execute for every possible value of the field. |
				| x | Execute at the given field value. |
				| x-y | Execute for value x up to value y of the field. |
				| */x | Execute for every multiple of x in the field (in relation to the next bigger field). |
				
				Examples:
				
				| Example | Meaning |
				| --- | --- |
				| Hours = * | Execute at every full hour. |
				| Hours = 12| Execute at 12 o’clock. |
				| Hours = 12-16 | Execute at 12, 13, 14, 15, 16 o’clock. |
				| Seconds = */15 | In every minute, execute at 0, 15, 30, 45 seconds. |
				| Seconds = */23 | Special case: In every minute, execute at 0, 23, 46 seconds. If the unit is not evenly divisible by the given number the last interval is shorter than the others. |

				### Notes
				- The scheduled functions are executed in the context of an admin user.
				- The CronService must be included in the structr.conf setting `configured.services` to be activated.
				- When a cronExpression is added, deleted or edited, the CronService has to be restarted for the changes to take effect. This can be done at runtime via the configuration tool or by restarting Structr.
				- By default, Structr prevents the same cron task to run run while the previous execution is still running. This can be changed via the setting `CronService.allowparallelexecution` in structr.conf.
				""";
			}

			@Override
			public List<Parameter> getParameters() {
				return null;
			}

			@Override
			public List<Example> getExamples() {
				return null;
			}

			@Override
			public List<String> getNotes() {
				return null;
			}

			@Override
			public List<Signature> getSignatures() {
				return null;
			}

			@Override
			public List<Language> getLanguages() {
				return null;
			}

			@Override
			public List<Usage> getUsages() {
				return null;
			}
		};
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
