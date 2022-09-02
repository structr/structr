/*
 * Copyright (C) 2010-2022 Structr GmbH
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
package org.structr.core.graph;

import static com.caucho.quercus.lib.JavaModule.java;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;

/**
 */
public class ManageThreadsCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(ManageThreadsCommand.class);
	private String command             = null;
	private long id                    = -1;

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		final String command = (String)attributes.get("command");
		if (StringUtils.isNotBlank(command)) {

			switch (command) {

				case "interrupt":
				case "kill":
					this.command = command;
					if (!attributes.containsKey("id")) {
						throw new FrameworkException(422, "ManageThreadsCommand: kill command needs id parameter.");
					} else {
						this.id = (long)attributes.get("id");
					}
					break;

				case "list":
					this.command = command;
					break;

				default:
					throw new FrameworkException(422, "ManageThreadsCommand: unknown command " + command + ", valid options are [list, kill].");
			}

		} else {

			throw new FrameworkException(422, "ManageThreadsCommand: missing command, valid options are [list, kill].");
		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return false;
	}

	@Override
	public Object getCommandResult() {

		switch (this.command) {

			case "list":
				return listThreads();

			case "kill":
				return killThread();
		}

		return null;
	}

	// ----- private methods -----
	private Object listThreads() {

		final List<Map<String, Object>> threads = new LinkedList<>();
		final ThreadMXBean bean                 = ManagementFactory.getThreadMXBean();

		for (final Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {

			final List<StackTraceElement> stack = filterForStructr(entry.getValue());
			final Thread thread                 = entry.getKey();
			final long id                       = thread.getId();

			if (!stack.isEmpty()) {

				threads.add(Map.of(
					"id",       id,
					"name",     thread.getName(),
					"alive",    thread.isAlive(),
					"state",    thread.getState().toString(),
					"priority", thread.getPriority(),
					"daemon",   thread.isDaemon(),
					"cpuTime",  Double.valueOf(bean.getThreadCpuTime(id)) / 1000000.0,
					"stack",    Arrays.asList(stack)
				));
			}
		}

		// sort by id
		Collections.sort(threads, (a, b) -> {
		
			final double id1 = (double)a.get("cpuTime");
			final double id2 = (double)b.get("cpuTime");

			return Double.compare(id1, id2);
		});

		return Map.of(
			"threads",    threads,
			"deadlocked", Arrays.asList(bean.findDeadlockedThreads())
		);
	}

	private Object killThread() {

		for (final Thread thread : Thread.getAllStackTraces().keySet()) {

			if (thread.getId() == id) {

				logger.info("Trying to kill thread {}..", id);
				thread.stop();
			}
		}
		
		return null;
	}

	private Object interruptThread() {

		for (final Thread thread : Thread.getAllStackTraces().keySet()) {

			if (thread.getId() == id) {

				logger.info("Trying to kill thread {}..", id);
				thread.interrupt();
			}
		}
		
		return null;
	}

	private List<StackTraceElement> filterForStructr(final StackTraceElement[] input) {

		final List<StackTraceElement> list = new LinkedList<>();

		for (final StackTraceElement e : input) {

			//if (e.toString().contains("structr")) {
			
				list.add(e);
			//}
		}

		return list;
	}
}