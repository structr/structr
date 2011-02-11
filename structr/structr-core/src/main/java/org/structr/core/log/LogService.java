/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.log;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.RunnableService;
import org.structr.core.Services;
import org.structr.core.entity.StructrNode;
import org.structr.core.entity.User;
import org.structr.core.entity.log.Activity;
import org.structr.core.entity.log.LogNodeList;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.NodeAttribute;

/**
 *
 * @author Christian Morgner
 */
public class LogService extends Thread implements RunnableService
{
	private static final Hashtable<User, LogNodeList<Activity>> loggerCache = new Hashtable<User, LogNodeList<Activity>>();
	private static final Logger logger = Logger.getLogger(LogService.class.getName());
	private static final ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();
//	private static final long DefaultInterval = TimeUnit.SECONDS.toMillis(30);
//	private static final int DefaultThreshold = 1000;
	private static final long DefaultInterval = 2000;
	private static final int DefaultThreshold = 10;

	private long interval = DefaultInterval;
	private int threshold = DefaultThreshold;

	// the global log (will be created)
	private LogNodeList<Activity> globalLogNodeList = null;
	private boolean run = false;

	public LogService()
	{
		super("LogService");

		// logging is a low-priority task
		this.setPriority(Thread.MIN_PRIORITY);
	}

	@Override
	public void run()
	{
		logger.log(Level.INFO, "Starting LogService");

		while(run || !queue.isEmpty())
		{
			logger.log(Level.FINER, "Checking queue..");

			// queue is not empty AND ((queue size is a above threshold) OR (service is to be stopped))
			if(!queue.isEmpty() && ((queue.size() > threshold) || !run) )
			{
				while(!queue.isEmpty())
				{
					Object o = queue.poll();
					if(o != null && o instanceof Activity)
					{
						Activity activity = (Activity)o;
						User owner = activity.getOwnerNode();

						// append to user-specific log
						LogNodeList userLog = getUserLog(owner);
						userLog.add(activity);

						// append to global log
						LogNodeList globalLog = getGlobalLog();
						globalLog.add(activity);
					}

					// cooperative multitasking :)
					Thread.yield();
				}
			}

			try
			{
				Thread.sleep(interval);

			} catch(Throwable t)
			{
				// ignore
			}
		}
	}

	// <editor-fold defaultstate="collapsed" desc="private methods">
	private LogNodeList getUserLog(User user)
	{
		LogNodeList ret = loggerCache.get(user);

		if(ret == null)
		{
			// First, try to find user's activity list
			for(StructrNode s : user.getDirectChildNodes(user))
			{
				if(s instanceof LogNodeList)
				{

					// Take the first LogNodeList
					ret = (LogNodeList) s;

					// store in cache
					loggerCache.put(user, ret);

					return ret;
				}
			}

			// Create a new activity list as child node of the respective user
			Command createNode = Services.createCommand(CreateNodeCommand.class);
			Command createRel = Services.createCommand(CreateRelationshipCommand.class);

			StructrNode s = (StructrNode) createNode.execute(user,
				new NodeAttribute(StructrNode.TYPE_KEY, LogNodeList.class.getSimpleName()),
				new NodeAttribute(StructrNode.NAME_KEY, user.getName() + "'s Activity Log"));

			ret = new LogNodeList<Activity>();
			ret.init(s);

			createRel.execute(user, ret, RelType.HAS_CHILD);

			// store in cache
			loggerCache.put(user, ret);
		}

		return (ret);
	}

	private LogNodeList getGlobalLog()
	{
		if(globalLogNodeList == null)
		{
			Command createNode = Services.createCommand(CreateNodeCommand.class);
			Command createRel = Services.createCommand(CreateRelationshipCommand.class);

			StructrNode s = (StructrNode) createNode.execute(
				new NodeAttribute(StructrNode.TYPE_KEY, LogNodeList.class.getSimpleName()),
				new NodeAttribute(StructrNode.NAME_KEY, "Global Activity Log"));

			globalLogNodeList = new LogNodeList<Activity>();
			globalLogNodeList.init(s);

			createRel.execute(globalLogNodeList, RelType.HAS_CHILD);
		}

		return(globalLogNodeList);
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="interface RunnableService">
	@Override
	public void startService()
	{
		this.run = true;
		super.start();
	}

	@Override
	public void stopService()
	{
		this.run = false;
		try { this.interrupt(); } catch(Throwable t) { /* ignore */ }
	}

	@Override
	public boolean isRunning()
	{
		return(this.run);
	}

	@Override
	public void injectArguments(Command command)
	{
	        command.setArgument("queue", queue);
	}

	@Override
	public void initialize(Map<String, Object> context)
	{
		// try to parse polling interval, set to default otherwise
		if(context.containsKey(Services.LOG_SERVICE_INTERVAL))
		{
			try
			{
				interval = Long.parseLong(context.get(Services.LOG_SERVICE_INTERVAL).toString());

			} catch(Throwable t)
			{
				interval = DefaultInterval;
			}
		}

		// try to parse flushing threshold, set to default otherwise
		if(context.containsKey(Services.LOG_SERVICE_THRESHOLD))
		{
			try
			{
				threshold = Integer.parseInt(context.get(Services.LOG_SERVICE_THRESHOLD).toString());

			} catch(Throwable t)
			{
				threshold = DefaultThreshold;
			}
		}
	}

	@Override
	public void shutdown()
	{
		this.run = false;
		try { this.interrupt(); } catch(Throwable t) { /* ignore */ }
	}
	// </editor-fold>
}
