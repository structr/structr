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
package org.structr.agent;

import org.structr.core.Command;
import org.structr.core.RunnableService;
import org.structr.core.Services;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.StructrConf;
import org.structr.schema.ConfigurationProvider;

//~--- classes ----------------------------------------------------------------

/**
 * The agent service main class.
 *
 *
 */
public class AgentService extends Thread implements RunnableService {

	private static final Logger logger = Logger.getLogger(AgentService.class.getName());

	//~--- fields ---------------------------------------------------------

	private final int maxAgents                          = 4;    // TODO: make configurable
	private final Map<String, List<Agent>> runningAgents = new ConcurrentHashMap<>(10, 0.9f, 8);
	private final Map<String, Class> agentClassCache     = new ConcurrentHashMap<>(10, 0.9f, 8);
	private final Queue<Task> taskQueue                  = new ConcurrentLinkedQueue<>();
	private Set<Class> supportedCommands                 = null;
	private boolean run                                  = false;

	//~--- constructors ---------------------------------------------------

	public AgentService() {

		super("AgentService");
		supportedCommands = new LinkedHashSet<>();
		supportedCommands.add(ProcessTaskCommand.class);

		super.setDaemon(true);
	}

	//~--- methods --------------------------------------------------------

	public void processTask(Task task) {

		synchronized (taskQueue) {

			taskQueue.add(task);
			logger.log(Level.FINE, "Task {0} added to task queue", task);
		}
	}

	public Agent findAgentForTask(Task task) {

		List<Agent> agents = getRunningAgentsForTask(task.getClass());

		synchronized (agents) {

			for (Agent agent : agents) {

				if (agent.getTaskQueue().contains(task)) {
					return (agent);
				}
			}
		}

		return (null);
	}

	@Override
	public void run() {

		logger.log(Level.INFO, "AgentService started");

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

	// <editor-fold defaultstate="collapsed" desc="interface RunnableService">
	@Override
	public void injectArguments(Command command) {
		command.setArgument("agentService", this);
	}

	@Override
	public void initialize(final Services services, final StructrConf config) {}

	@Override
	public void initialized() {}

	@Override
	public void shutdown() {}

	@Override
	public void startService() {

		run = true;
		this.start();
	}

	@Override
	public void stopService() {
		run = false;
	}

	@Override
	public boolean runOnStartup() {
		return (true);
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="private methods">
	private void assignNextAgentForTask(Task nextTask) {

		Class taskClass    = nextTask.getClass();
		List<Agent> agents = getRunningAgentsForTask(taskClass);

		// need to synchronize on agents
		synchronized (agents) {

			// find next free agent (agents should be sorted by load, so one
			// of the first should do..
			for (Agent agent : agents) {

				if (agent.assignTask(nextTask)) {

					// ok, task is assigned
					logger.log(Level.FINE, "Task assigned to agent {0}", agent.getName());

					return;
				}
			}
		}

		// FIXME: find better solution for hard limit here!
		if (agents.size() < maxAgents) {

			// if we get here, task was not assigned to any agent, need to
			// create a new one.
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

			logger.log(Level.FINE, "Overall agents limit reached, re-queueing task");

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

		Agent agent = null;

		try {

			agent = lookupAgent(forTask);

			if (agent != null) {

				// register us in agent..
				agent.setAgentService(this);
			}

		} catch (Exception ex) {

			// TODO: handle exception etc..
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

	// </editor-fold>

	//~--- get methods ----------------------------------------------------

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

			agents = Collections.synchronizedList(new LinkedList<Agent>());

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
}
