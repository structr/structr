/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.agent;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.structr.core.Command;
import org.structr.core.RunnableService;
import org.structr.core.Services;
import org.structr.core.module.GetAgentsCommand;

/**
 *
 * @author cmorgner
 */
public class AgentService extends Thread implements RunnableService
{
	private final Map<Class, List<Agent>> runningAgents = new ConcurrentHashMap<Class, List<Agent>>();
	private final Map<Class, Class> agentClassCache = new ConcurrentHashMap<Class, Class>();
	private final Queue<Task> taskQueue = new ConcurrentLinkedQueue<Task>();
	private Set<Class> supportedCommands = null;
	private boolean run = false;

	public AgentService()
	{
		super("AgentService");

		supportedCommands = new LinkedHashSet<Class>();
		supportedCommands.add(ProcessTaskCommand.class);
	}

	public void processTask(Task task)
	{
		synchronized(taskQueue)
		{
			taskQueue.add(task);
		}
	}

	public Agent findAgentForTask(Task task)
	{
		List<Agent> agents = getRunningAgentsForTask(task.getClass());
		synchronized(agents)
		{
			for(Agent agent : agents)
			{
				if(agent.getTaskQueue().contains(task))
				{
					return(agent);
				}
			}
		}

		return(null);
	}

	/**
	 * Returns the current queue of remaining tasks.
	 * @return
	 */
	public Collection<Task> getTaskQueue()
	{
		return(taskQueue);
	}

	/**
	 * Returns the current collection of running agents.
	 * @return
	 */
	public Map<Class, List<Agent>> getRunningAgents()
	{
		return(runningAgents);
	}

	@Override
	public void run()
	{
		// FIXME: use logger here..
		// System.out.println("AgentService.run(): started");

		while(run)
		{
			Task nextTask = null;

			synchronized(taskQueue)
			{
				nextTask = taskQueue.poll();

				if(nextTask != null)
				{
					assignNextAgentForTask(nextTask);
				}

				// sleep a bit waiting for tasks..
				try
				{
					Thread.sleep(10);

				} catch(Exception ex)
				{
				}
			}
		}
	}

	public void notifyAgentStart(Agent agent)
	{
		List<Agent> agents = getRunningAgentsForTask(agent.getSupportedTaskType());
		synchronized(agents)
		{
			agents.add(agent);
		}
	}

	public void notifyAgentStop(Agent agent)
	{
		List<Agent> agents = getRunningAgentsForTask(agent.getSupportedTaskType());

		synchronized(agents)
		{
			agents.remove(agent);
		}
	}

	// <editor-fold defaultstate="collapsed" desc="interface RunnableService">
	@Override
	public void injectArguments(Command command)
	{
		command.setArgument("agentService", this);
	}

	@Override
	public boolean isRunning()
	{
		return (this.run);
	}

	@Override
	public void initialize(Map<String, Object> context)
	{
	}

	@Override
	public void shutdown()
	{
	}

	@Override
	public void startService()
	{
		run = true;
		this.start();
	}

	@Override
	public void stopService()
	{
		run = false;
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="private methods">
	private void assignNextAgentForTask(Task nextTask)
	{
		Class taskClass = nextTask.getClass();
		List<Agent> agents = getRunningAgentsForTask(taskClass);

		// need to synchronize on agents
		synchronized(agents)
		{
			// find next free agent (agents should be sorted by load, so one
			// of the first should do..

			for(Agent agent : agents)
			{

				if(agent.assignTask(nextTask))
				{
					// ok, task is assigned
					return;
				}
			}
		}

		// if we get here, task was not assigned to any agent, need to
		// create a new one.
		Agent agent = createAgent(nextTask);

		if(agent != null && agent.assignTask(nextTask))
		{
			agent.start();

		} else
		{
			// re-add task..
			synchronized(taskQueue)
			{
				taskQueue.add(nextTask);
			}
		}
	}

	private List<Agent> getRunningAgentsForTask(Class taskClass)
	{
		List<Agent> agents = runningAgents.get(taskClass);

		if(agents == null)
		{
			agents = Collections.synchronizedList(new LinkedList<Agent>());

			// Hashtable is synchronized
			runningAgents.put(taskClass, agents);
		}

		return (agents);
	}

	/**
	 * Creates a new agent for the given Task. Note that the agent must be
	 * started manually after creation.
	 *
	 * @param forTask
	 * @return a new agent for the given task
	 */
	private Agent createAgent(Task forTask)
	{
		Agent agent = null;

		try
		{
			agent = lookupAgent(forTask);

			if(agent != null)
			{
				// register us in agent..
				agent.setAgentService(this);
			}

		} catch(Exception ex)
		{
			// TODO: handle exception etc..
		}

		return (agent);
	}

	private Agent lookupAgent(Task task)
	{
		Class taskClass = task.getClass();
		Agent agent = null;

		Class agentClass = agentClassCache.get(taskClass);

		// cache miss
		if(agentClass == null)
		{

//            Set<Class> agentClasses = ClasspathEntityLocator.locateEntitiesByType(Agent.class);
			Map<String, Class> agentClassesMap = (Map<String, Class>)Services.createCommand(GetAgentsCommand.class).execute();

			for(String className : agentClassesMap.keySet())
			{

				Class supportedAgentClass = agentClassesMap.get(className);
				try
				{
					Agent supportedAgent = (Agent)supportedAgentClass.newInstance();
					Class supportedTaskClass = supportedAgent.getSupportedTaskType();

					if(supportedTaskClass.equals(taskClass))
					{
						agentClass = supportedAgentClass;
					}

					agentClassCache.put(supportedTaskClass, supportedAgentClass);

				} catch(IllegalAccessException iaex)
				{
				} catch(InstantiationException itex)
				{
				}
			}
		}

		if(agentClass != null)
		{
			try
			{
				agent = (Agent)agentClass.newInstance();

			} catch(IllegalAccessException iaex)
			{
			} catch(InstantiationException itex)
			{
			}
		}

		return (agent);
	}
	// </editor-fold>
}
