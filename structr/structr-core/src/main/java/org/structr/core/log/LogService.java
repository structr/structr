/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.log;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.RunnableService;
import org.structr.core.Services;
import org.structr.core.entity.StructrNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
import org.structr.core.entity.log.Activity;
import org.structr.core.entity.log.LogNodeList;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.GraphDatabaseCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.NodeFactoryCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 * A logging service that will asynchronously persist log messages of type
 * {@see org.structr.core.entity.log.Activity}.
 * 
 * FIXME: javadoc..
 *
 * @author Christian Morgner
 */
public class LogService extends Thread implements RunnableService {

    private static final Hashtable<User, LogNodeList<Activity>> loggerCache = new Hashtable<User, LogNodeList<Activity>>();
    private static final Logger logger = Logger.getLogger(LogService.class.getName());
    private static final ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();
    private static final long DefaultInterval = TimeUnit.SECONDS.toMillis(10);
    private static final int DefaultThreshold = 10;
    private long interval = DefaultInterval;
    private int threshold = DefaultThreshold;
    // the global log (will be created)
    private LogNodeList<Activity> globalLogNodeList = null;
    private boolean run = false;

    public LogService() {
        super("LogService");

        // logging is a low-priority task
        this.setPriority(Thread.MIN_PRIORITY);
    }

    @Override
    public void run() {
        try {

            logger.log(Level.INFO, "Starting LogService");

            // initialize global log..
            getGlobalLog();

            while (run || !queue.isEmpty()) {
                logger.log(Level.FINER, "Checking queue..");

                // queue is not empty AND ((queue size is a above threshold) OR (service is to be stopped))
                if (!queue.isEmpty() && ((queue.size() > threshold) || !run)) {

                    logger.log(Level.FINEST, "+++ LogService active ... +++");

                    while (!queue.isEmpty()) {
                        Object o = queue.poll();
                        if (o != null && o instanceof Activity) {
                            Activity activity = (Activity) o;
                            User user = activity.getUser();

                            // Commit to database so node will have id and owner
                            activity.commit(user);

                            // append to global log
                            LogNodeList globalLog = getGlobalLog();
                            globalLog.add(activity);

                            logger.log(Level.FINEST, "Added activity {0} to global log.", activity.getId());

                            // append to user-specific log
                            LogNodeList userLog = getUserLog(user);
                            userLog.add(activity);

                            logger.log(Level.FINEST, "Added activity {0} to {1}''s log.", new Object[]{activity.getId(), user.getName()});

                        }

                        // cooperative multitasking :)
                        Thread.yield();
                    }

                    logger.log(Level.FINEST, "+++ LogService inactive. +++");
                }

                try {
                    Thread.sleep(interval);

                } catch (Throwable t) {
                    t.printStackTrace(System.out);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace(System.out);
        }
    }

    public LogNodeList getUserLog(final User user) {
        LogNodeList ret = loggerCache.get(user);

        if (ret == null) {
            // First, try to find user's activity list
            for (StructrNode s : user.getDirectChildNodes(user)) {
                if (s instanceof LogNodeList) {

                    // Take the first LogNodeList
                    ret = (LogNodeList) s;

                    // store in cache
                    loggerCache.put(user, ret);

                    return ret;
                }
            }

            ret = (LogNodeList) Services.createCommand(TransactionCommand.class).execute(new StructrTransaction() {

                @Override
                public Object execute() throws Throwable {
//                    LogNodeList newLogNodeList = null;

                    // Create a new activity list as child node of the respective user
                    Command createNode = Services.createCommand(CreateNodeCommand.class);
                    Command createRel = Services.createCommand(CreateRelationshipCommand.class);

                    LogNodeList<Activity> newLogNodeList = (LogNodeList<Activity>) createNode.execute(user,
                            new NodeAttribute(StructrNode.TYPE_KEY, LogNodeList.class.getSimpleName()),
                            new NodeAttribute(StructrNode.NAME_KEY, user.getName() + "'s Activity Log"));

//                    newLogNodeList = new LogNodeList<Activity>();
//                    newLogNodeList.init(s);

                    createRel.execute(user, newLogNodeList, RelType.HAS_CHILD);

                    return (newLogNodeList);
                }
            });

            // store in cache
            loggerCache.put(user, ret);
        }

        return (ret);
    }

    public LogNodeList getGlobalLog() {
        if (globalLogNodeList == null) {
            globalLogNodeList = (LogNodeList) Services.createCommand(TransactionCommand.class).execute(new StructrTransaction() {

                @Override
                public Object execute() throws Throwable {
                    GraphDatabaseService graphDb = (GraphDatabaseService) Services.createCommand(GraphDatabaseCommand.class).execute();
                    Command factory = Services.createCommand(NodeFactoryCommand.class);
                    LogNodeList ret = null;

                    if (graphDb != null) {
                        StructrNode rootNode = (StructrNode) factory.execute(graphDb.getReferenceNode());
                        if (rootNode != null) {
                            for (StructrRelationship rel : rootNode.getOutgoingChildRelationships()) {
                                if (rel.getEndNode() instanceof LogNodeList) {
                                    return ((LogNodeList) rel.getEndNode());
                                }
                            }
                        }

                        // if we arrive here, no global log node exists yet
                        Command createNode = Services.createCommand(CreateNodeCommand.class);
                        Command createRel = Services.createCommand(CreateRelationshipCommand.class);

                        ret = (LogNodeList<Activity>) createNode.execute(
                                new NodeAttribute(StructrNode.TYPE_KEY, LogNodeList.class.getSimpleName()),
                                new NodeAttribute(StructrNode.NAME_KEY, "Global Activity Log"));

//                        ret = new LogNodeList<Activity>();
//                        ret.init(s);

                        // load reference node and link new node to it..
                        createRel.execute(rootNode, ret, RelType.HAS_CHILD);
                    }

                    return (ret);
                }
            });
        }

        return (globalLogNodeList);
    }

    // <editor-fold defaultstate="collapsed" desc="interface RunnableService">
    @Override
    public void startService() {
        this.run = true;
        super.start();
    }

    @Override
    public void stopService() {
        this.run = false;
        try {
            this.interrupt();
        } catch (Throwable t) { /* ignore */ }
    }

    @Override
    public boolean isRunning() {
        return (this.run);
    }

    @Override
    public void injectArguments(Command command) {
        command.setArgument("queue", queue);
        command.setArgument("service", this);
    }

    @Override
    public void initialize(Map<String, Object> context) {
        // try to parse polling interval, set to default otherwise
        if (context.containsKey(Services.LOG_SERVICE_INTERVAL)) {
            try {
                interval = Long.parseLong(context.get(Services.LOG_SERVICE_INTERVAL).toString());

            } catch (Throwable t) {
                interval = DefaultInterval;
            }
        }

        // try to parse flushing threshold, set to default otherwise
        if (context.containsKey(Services.LOG_SERVICE_THRESHOLD)) {
            try {
                threshold = Integer.parseInt(context.get(Services.LOG_SERVICE_THRESHOLD).toString());

            } catch (Throwable t) {
                threshold = DefaultThreshold;
            }
        }
    }

    @Override
    public void shutdown() {
        this.run = false;
        try {
            this.interrupt();
        } catch (Throwable t) { /* ignore */ }
    }
    // </editor-fold>
}
