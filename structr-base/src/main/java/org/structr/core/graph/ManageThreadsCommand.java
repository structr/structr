/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

			case "interrupt":
				return interruptThread();

			case "kill":
				return killThread();
		}

		return null;
	}

	// ----- private methods -----
	private Object listThreads() {

		final List<Map<String, Object>> threads = new ArrayList<>();
		final ThreadMXBean bean                 = ManagementFactory.getThreadMXBean();
		final Set<Long> deadlockedSet           = new LinkedHashSet<>();
		final long[] deadlocked                 = bean.findDeadlockedThreads();

		if (deadlocked != null) {

			for (final long id : bean.findDeadlockedThreads()) {
				deadlockedSet.add(id);
			}
		}

		for (final Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {

			final List<String> stack = filterAndTransform(entry.getValue());
			final Thread thread      = entry.getKey();
			final long id            = thread.getId();

			if (!stack.isEmpty()) {

				threads.add(Map.of(
					"id",       id,
					"name",     thread.getName(),
					"alive",    thread.isAlive(),
					"state",    thread.getState().toString(),
					"priority", thread.getPriority(),
					"cpuTime",  Double.valueOf(bean.getThreadCpuTime(id)) / 1_000_000_000.0,
					"deadlock", deadlockedSet.contains(id),
					"stack",    stack
				));
			}
		}

		// sort by id
		Collections.sort(threads, (a, b) -> {

			final double id1 = (double)a.get("cpuTime");
			final double id2 = (double)b.get("cpuTime");

			// sort descending
			return Double.compare(id2, id1);
		});

		return threads;
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

				logger.info("Trying to interrupt thread {}..", id);
				thread.interrupt();
			}
		}

		return null;
	}

	private List<String> filterAndTransform(final StackTraceElement[] input) {

		final List<String> list = new ArrayList<>();

		for (final StackTraceElement e : input) {

			final String transformed = transform(e);
			if (transformed != null) {

				list.add(0, transformed);
			}
		}

		return list;
	}

	private String transform(final StackTraceElement stackTraceElement) {

		final String className  = stackTraceElement.getClassName();
		final String methodName = stackTraceElement.getMethodName();
		final String key        = className + "." + methodName;

		for (final Entry<Pattern, String> entry : RecognizedStackElements.entrySet()) {

			final Pattern pattern    = entry.getKey();
			final Matcher matcher    = pattern.matcher(key);
			final String replacement = entry.getValue();

			if (matcher.matches()) {

				return matcher.replaceAll(replacement);
			}
		}

		return null;
	}

	private static final Map<Pattern, String> RecognizedStackElements = Map.ofEntries(

		// maintenance
		Map.entry(Pattern.compile("org.structr.rest.resource.MaintenanceResource.doPost"),      "Execute Maintenance Command"),
		Map.entry(Pattern.compile("org.structr.web.maintenance.DeployCommand.doImport"),        "Structr App Deployment Import"),
		Map.entry(Pattern.compile("org.structr.web.maintenance.DeployCommand.importFiles"),     "Deployment Phase: Import Files"),
		Map.entry(Pattern.compile("org.structr.web.maintenance.DeployCommand.importSchema"),    "Deployment Phase: Import Schema"),
		Map.entry(Pattern.compile("org.structr.core.graph.ManageThreadsCommand.listThreads"),   "List Running Threads"),

		// services
		Map.entry(Pattern.compile("org.structr.cron.CronService\\$1.run"),                        "Cron Service"),

		// schema
		Map.entry(Pattern.compile("org.structr.dynamic.([A-Za-z]+).onCreation"),                "$1.onCreate"),
		Map.entry(Pattern.compile("org.structr.dynamic.([A-Za-z]+).afterCreation"),             "$1.afterCreate"),
		Map.entry(Pattern.compile("org.structr.dynamic.([A-Za-z]+).onModification"),            "$1.onSave"),
		Map.entry(Pattern.compile("org.structr.dynamic.([A-Za-z]+).afterModification"),         "$1.afterSave"),
		Map.entry(Pattern.compile("org.structr.core.entity.AbstractEndpoint.getSingle"),        "Get Relationship"),
		Map.entry(Pattern.compile("org.structr.core.entity.AbstractEndpoint.getMultiple"),      "Get Relationships"),
		Map.entry(Pattern.compile("org.structr.core.graph.CreateNodeCommand.execute"),          "Create Node"),
		Map.entry(Pattern.compile("org.structr.core.graph.CreateRelationshipCommand.execute"),  "Create Relationship"),
		Map.entry(Pattern.compile("org.structr.schema.SchemaService.reloadSchema"),             "Compile Dynamic Schema"),

		// transactions
		Map.entry(Pattern.compile("org.structr.core.graph.Tx.begin"),                           "Begin Transaction"),
		Map.entry(Pattern.compile("org.structr.core.graph.Tx.success"),                         "Commit Transaction"),
		Map.entry(Pattern.compile("org.structr.core.graph.Tx.close"),                           "Close Transaction"),

		Map.entry(Pattern.compile("org.structr.core.graph.ModificationQueue.doInnerCallbacks"), "Transaction Phase: Callbacks Before Commit"),
		Map.entry(Pattern.compile("org.structr.core.graph.ModificationQueue.doValidation"),     "Transaction Phase: Indexing"),
		Map.entry(Pattern.compile("org.structr.core.graph.ModificationQueue.doPostProcessing"), "Transaction Phase: Callbacks After Commit"),


		// HTML
		Map.entry(Pattern.compile("org.structr.web.servlet.HtmlServlet.doGet"),                 "HTTP GET"),
		Map.entry(Pattern.compile("org.structr.dynamic.Page.render"),                           "Render Page"),
		Map.entry(Pattern.compile("org.structr.dynamic.Template.renderContent"),                "Render Template"),
		Map.entry(Pattern.compile("org.structr.dynamic.DOMElement.renderContent"),              "Render Element"),
		Map.entry(Pattern.compile("org.structr.dynamic.DOMNode.render"),                        "Render DOM Node"),

		Map.entry(Pattern.compile("org.structr.core.script.Scripting.evaluateJavascript"),      "Run Javascript"),
		Map.entry(Pattern.compile("org.structr.core.function.Functions.evaluate"),              "Run StructrScript"),

		Map.entry(Pattern.compile("org.structr.web.function.([A-Za-z]+).apply"),                "Execute $1"),

		// REST
		Map.entry(Pattern.compile("org.structr.rest.serialization.StreamingWriter.stream"),     "Stream JSON"),

		Map.entry(Pattern.compile("org.structr.rest.servlet.JsonRestServlet.doGet"),           "REST GET"),
		Map.entry(Pattern.compile("org.structr.rest.servlet.JsonRestServlet.doPut"),           "REST PUT"),
		Map.entry(Pattern.compile("org.structr.rest.servlet.JsonRestServlet.doPost"),          "REST POST"),
		Map.entry(Pattern.compile("org.structr.rest.servlet.JsonRestServlet.doPatch"),         "REST PATCH"),
		Map.entry(Pattern.compile("org.structr.rest.servlet.JsonRestServlet.doDelete"),        "REST DELETE"),
		Map.entry(Pattern.compile("org.structr.rest.servlet.JsonRestServlet.doHead"),          "REST HEAD"),
		Map.entry(Pattern.compile("org.structr.rest.servlet.JsonRestServlet.doOptions"),       "REST OPTIONS")
	);
}