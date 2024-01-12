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
package org.structr.agent;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.service.*;
import org.structr.core.Services;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.SchemaService;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The agent service main class.
 */
@ServiceDependency(SchemaService.class)
@StopServiceForMaintenanceMode
public class AgentService extends Thread implements RunnableService {

	private static final Logger logger = LoggerFactory.getLogger(AgentService.class.getName());

	private final int maxAgents                          = 10;    // TODO: make configurable
	private final Map<String, List<Agent>> runningAgents = new ConcurrentHashMap<>(10, 0.9f, 8);
	private final Map<String, Class> agentClassCache     = new ConcurrentHashMap<>(10, 0.9f, 8);
	private final Queue<Task> taskQueue                  = new ConcurrentLinkedQueue<>();
	private Set<Class> supportedCommands                 = null;
	private boolean run                                  = false;

	public AgentService() {

		super("AgentService");
		supportedCommands = new LinkedHashSet<>();
		supportedCommands.add(ProcessTaskCommand.class);

		super.setDaemon(true);
	}

	public void processTask(Task task) {

		synchronized (taskQueue) {

			taskQueue.add(task);
			logger.debug("Task {} added to task queue", task);
		}
	}

	@Override
	public void run() {

		logger.info("AgentService started");

		while (run) {

			Task nextTask = taskQueue.poll();
			if (nextTask != null) {

				assignNextAgentForTask(nextTask);
			}

			// let others act
			try { Thread.sleep(10); } catch(Throwable ignore) {}
		}
	}

	public void notifyAgentStart(Agent agent) {

		List<Agent> agents = getRunningAgentsForTask(agent.getSupportedTaskType());

		synchronized (agents) {
			agents.add(agent);
		}
	}

	public void notifyAgentStop(Agent agent) {

		List<Agent> agents = getRunningAgentsForTask(agent.getSupportedTaskType());

		synchronized (agents) {
			agents.remove(agent);
		}
	}

	public Map<String, Class<? extends Agent>> getAgents() {

		final ConfigurationProvider configuration = Services.getInstance().getConfigurationProvider();
		if (configuration != null) {

			return configuration.getAgents();
		}

		return Collections.emptyMap();
	}

	@Override
	public void injectArguments(Command command) {
		command.setArgument("agentService", this);
	}

	@Override
	public ServiceResult initialize(final StructrServices services, String serviceName) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		return new ServiceResult(true);
	}

	@Override
	public void initialized() {}

	@Override
	public void shutdown() {}

	@Override
	public void startService() throws Exception {

		run = true;
		this.start();
	}

	@Override
	public void stopService() {
		run = false;
	}

	@Override
	public boolean runOnStartup() {
		return true;
	}

	private void assignNextAgentForTask(final Task nextTask) {

		Class taskClass    = nextTask.getClass();
		List<Agent> agents = getRunningAgentsForTask(taskClass);

		// need to synchronize on agents
		synchronized (agents) {

			// find next free agent (agents should be sorted by load, so one
			// of the first should do..
			for (Agent agent : agents) {

				if (agent.assignTask(nextTask)) {

					// ok, task is assigned
					logger.debug("Task assigned to agent {} ({})", agent.getName(), agent.hashCode());

					return;
				}
			}
		}

		if (agents.size() < maxAgents) {

			// if we get here, task was not assigned to any agent, need to create a new one.
			Agent agent = createAgent(nextTask);

			if ((agent != null) && agent.assignTask(nextTask)) {

				agent.start();

			} else {

				// re-add task..
				synchronized (taskQueue) {
					taskQueue.add(nextTask);
				}
			}

		} else {

			logger.debug("Overall agents limit reached, re-queueing task");

			// re-add task..
			synchronized (taskQueue) {
				taskQueue.add(nextTask);
			}
		}
	}

	/**
	 * Creates a new agent for the given Task. Note that the agent must be
	 * started manually after creation.
	 *
	 * @param forTask
	 * @return a new agent for the given task
	 */
	private Agent createAgent(Task forTask) {

		logger.debug("Creating new agent for task {}", forTask.getClass().getSimpleName());

		Agent agent = null;

		try {

			agent = lookupAgent(forTask);

			if (agent != null) {

				// register us in agent..
				agent.setAgentService(this);
			}

		} catch (Throwable t) {
			logger.error(ExceptionUtils.getStackTrace(t));
		}

		return (agent);
	}

	private Agent lookupAgent(Task task) {

		// FIXME: superuser security context
		Class taskClass  = task.getClass();
		Agent agent      = null;
		Class agentClass = agentClassCache.get(taskClass.getName());

		// cache miss
		if (agentClass == null) {

			Map<String, Class<? extends Agent>> agentClassesMap = getAgents();

			if (agentClassesMap != null) {

				for (Entry<String, Class<? extends Agent>> classEntry : agentClassesMap.entrySet()) {

					Class<? extends Agent> supportedAgentClass = agentClassesMap.get(classEntry.getKey());

					try {

						Agent supportedAgent     = supportedAgentClass.newInstance();
						Class supportedTaskClass = supportedAgent.getSupportedTaskType();

						if (supportedTaskClass.equals(taskClass)) {
							agentClass = supportedAgentClass;
						}

						agentClassCache.put(supportedTaskClass.getName(), supportedAgentClass);

					} catch (Throwable ignore) {}
				}
			}
		}

		if (agentClass != null) {

			try {
				agent = (Agent) agentClass.newInstance();

			} catch (Throwable ignore) {}
		}

		return (agent);
	}

	/**
	 * Returns the current queue of remaining tasks.
	 * @return tasks
	 */
	public Collection<Task> getTaskQueue() {
		return (taskQueue);
	}

	/**
	 * Returns the current collection of running agents.
	 * @return agents
	 */
	public Map<String, List<Agent>> getRunningAgents() {
		return (runningAgents);
	}

	private List<Agent> getRunningAgentsForTask(Class taskClass) {

		List<Agent> agents = runningAgents.get(taskClass.getName());

		if (agents == null) {

			agents = Collections.synchronizedList(new LinkedList<>());

			// Hashtable is synchronized
			runningAgents.put(taskClass.getName(), agents);
		}

		return (agents);
	}

	@Override
	public boolean isRunning() {
		return (this.run);
	}

	@Override
	public boolean isVital() {
		return false;
	}

	@Override
	public boolean waitAndRetry() {
		return false;
	}

	// ----- interface Feature -----
	@Override
	public String getModuleName() {
		return "agents";
	}
}
