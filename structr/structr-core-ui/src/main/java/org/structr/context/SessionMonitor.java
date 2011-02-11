/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.context;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.StructrNode;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
import org.structr.core.entity.log.Activity;
import org.structr.core.entity.log.LogNodeList;
import org.structr.core.entity.log.PageRequest;
import org.structr.core.log.LogCommand;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 * Helper class for handling session management
 *
 * @author axel
 */
public class SessionMonitor {

    private static final String USER_LIST_KEY = "userList";
    private static List<Session> sessions = null;
    public static final String SESSION_ID = "sessionId";

    /**
     * This class holds information about a user session, e.g.
     * the user object, the login and logout time, and a list
     * with activities.
     *
     */
    // <editor-fold defaultstate="collapsed" desc="Session">
    public static class Session {

        private long id;
        private String uid;
        private State state;
        private User user;
        private Date loginTimestamp;
        private Date logoutTimestamp;
//        private LogNodeList<Activity> activityList;
        private long lastActivityId;

        public Session(final long id, String uid, final User user, Date loginTime) {
            this.id = id;
            this.uid = uid;
            this.state = State.ACTIVE;
            this.user = user;
            this.loginTimestamp = loginTime;
//            this.activityList = getActivityList();
        }

        private boolean hasActivity() {
            Activity lastActivity = getLastActivity();
            return (lastActivity != null);
        }

        /**
         * @return last activity type
         */
        public String getLastActivityType() {
            Activity lastActivity = getLastActivity();
            return lastActivity != null ? getLastActivity().getType() : null;
        }

        /**
         * @return last activity start timestamp
         */
        public Date getLastActivityStartTimestamp() {
            return hasActivity() ? getLastActivity().getStartTimestamp() : null;
        }

        /**
         * @return last activity end timestamp
         */
        public Date getLastActivityEndTimestamp() {
            return hasActivity() ? getLastActivity().getEndTimestamp() : null;
        }

        /**
         * @return last activity URI
         */
        public String getLastActivityText() {
            return hasActivity() ? getLastActivity().getActivityText() : null;
        }

        /**
         * Return time since last activity
         * @return time since last activity
         */
        public String getInactiveSince() {
            if (hasActivity()) {
                long ms = (new Date()).getTime() - getLastActivityEndTimestamp().getTime();

                if (ms < 1000) {
                    return ms + " ms";
                } else if (ms < 60 * 1000) {
                    return ms / 1000 + " s";
                } else if (ms < 60 * 60 * 1000) {
                    return ms / (60 * 1000) + " m";
                } else if (ms < 24 * 60 * 60 * 1000) {
                    return ms / (60 * 60 * 1000) + " h";
                } else {
                    return "more than a day";
                }
            }
            return "";
        }

        public String getUserName() {
            return user.getName();
        }

        public User getUser() {
            return user;
        }

        public void setUser(final User user) {
            this.user = user;
        }

        public Date getLoginTimestamp() {
            return loginTimestamp;
        }

        public void setLoginTimestamp(final Date loginTimestamp) {
            this.loginTimestamp = loginTimestamp;
        }

        public LogNodeList<Activity> getActivityList() {

            LogNodeList<Activity> activityList;
            // First, try to find user's activity list
            for (StructrNode s : user.getDirectChildNodes(user)) {
                if (s instanceof LogNodeList) {

                    // Take the first LogNodeList
                    activityList = (LogNodeList) s;
                    return activityList;
                }
            }

            // Create a new activity list as child node of the respective user
            Command createNode = Services.createCommand(CreateNodeCommand.class);
            Command createRel = Services.createCommand(CreateRelationshipCommand.class);

            StructrNode s = (StructrNode) createNode.execute(user,
                    new NodeAttribute(StructrNode.TYPE_KEY, LogNodeList.class.getSimpleName()),
                    new NodeAttribute(StructrNode.NAME_KEY, user.getName() + "'s Activity Log"));
            activityList = new LogNodeList<Activity>();
            activityList.init(s);

            createRel.execute(user, activityList, RelType.HAS_CHILD);

            return activityList;
        }

        private void setLastActivity(final Activity activity) {
            lastActivityId = activity.getId();
        }

        private Activity getLastActivity() {
            Activity a = new Activity();
            StructrNode node = (StructrNode) Services.createCommand(FindNodeCommand.class).execute(new SuperUser(), lastActivityId);
            a.init(node);
            return a;
        }

//        public void setActivityList(final List<Activity> activityList) {
//            this.setActivityList(activityList);
//        }
        public Date getLogoutTimestamp() {
            return logoutTimestamp;
        }

        public void setLogoutTime(final Date logoutTime) {
            this.logoutTimestamp = logoutTime;
        }

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public String getUid() {
            return uid;
        }

        /**
         * @param uid the uid to set
         */
        public void setUid(final String uid) {
            this.uid = uid;
        }

//        public void addActivity(final Activity activity) {
//            activityList.add(activity);
//        }
        public State getState() {
            return state;
        }

        public void setState(final State state) {
            this.state = state;
        }
    }// </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="State">
    public enum State {

        UNDEFINED, ACTIVE, INACTIVE, WAITING, CLOSED, STARTED, FINISHED;
    }// </editor-fold>

    /**
     * Return list with all sessions
     * 
     * @return
     */
    public static List<Session> getSessions() {
        return sessions;
    }

    /**
     * Register user in servlet context
     */
    public static long registerUser(final User user, final HttpSession session) {

        init(session.getServletContext());
        long id = nextId(session.getServletContext());
        sessions.add(new Session(id, session.getId(), user, new Date()));
        session.getServletContext().setAttribute(USER_LIST_KEY, sessions);
        return id;

    }

    /**
     * Unregister user in servlet context
     */
    public static void unregisterUser(final long id, final ServletContext context) {

        init(context);
        Session session = getSession(id);
        session.setLogoutTime(new Date());
        session.setState(State.INACTIVE);
        context.setAttribute(USER_LIST_KEY, sessions);

    }

    /**
     * Append an activity to the activity list
     * 
     * @param sessionId
     * @param action
     */
    public static void logActivity(final User user, final long sessionId, final String action) {

        final Command transactionCommand = Services.createCommand(TransactionCommand.class);
        transactionCommand.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {

                Command createNode = Services.createCommand(CreateNodeCommand.class);
                Date now = new Date();

                StructrNode s = (StructrNode) createNode.execute(user,
                        new NodeAttribute(StructrNode.TYPE_KEY, Activity.class.getSimpleName()),
                        new NodeAttribute(StructrNode.NAME_KEY, action + " (" + now + ")"),
                        //new NodeAttribute(Activity.ACTIVITY_TEXT_KEY, user.getName() + ":" + action + ":" + now),
                        new NodeAttribute(Activity.SESSION_ID_KEY, sessionId),
                        new NodeAttribute(Activity.START_TIMESTAMP_KEY, now),
                        new NodeAttribute(Activity.END_TIMESTAMP_KEY, now));

                Activity activity = new Activity();
                activity.init(s);

                getSession(sessionId).setLastActivity(activity);
                Services.createCommand(LogCommand.class).execute(activity);

                return null;
            }
        });
    }

    /**
     * Append a page request to the activity list
     *
     * @param sessionId
     * @param action
     */
    public static void logPageRequest(final User user, final long sessionId, final String action, final HttpServletRequest request) {
        if (request != null) {

            final Command transactionCommand = Services.createCommand(TransactionCommand.class);
            transactionCommand.execute(new StructrTransaction() {

                @Override
                public Object execute() throws Throwable {

                    Command createNode = Services.createCommand(CreateNodeCommand.class);
                    Date now = new Date();

                    StructrNode s = (StructrNode) createNode.execute(user,
                            new NodeAttribute(StructrNode.TYPE_KEY, PageRequest.class.getSimpleName()),
                            new NodeAttribute(StructrNode.NAME_KEY, user.getName() + ":" + action + ":" + now),
                            new NodeAttribute(PageRequest.URI_KEY, request.getRequestURI()),
                            //new NodeAttribute(Activity.ACTIVITY_TEXT_KEY, user.getName() + ":" + action + ":" + now),
                            new NodeAttribute(PageRequest.REMOTE_ADDRESS_KEY, request.getRemoteAddr()),
                            new NodeAttribute(PageRequest.REMOTE_HOST_KEY, request.getRemoteHost()),
                            new NodeAttribute(Activity.SESSION_ID_KEY, sessionId),
                            new NodeAttribute(Activity.START_TIMESTAMP_KEY, now),
                            new NodeAttribute(Activity.END_TIMESTAMP_KEY, now));

                    Activity activity = new Activity();
                    activity.init(s);

                    getSession(sessionId).setLastActivity(activity);
                    Services.createCommand(LogCommand.class).execute(activity);

                    return null;
                }
            });
        }
    }
    // ---------------- private methods ---------------------    
    // <editor-fold defaultstate="collapsed" desc="private methods">

    private static Session getSession(final long sessionId) {
        for (Session s : sessions) {
            if (s.getId() == sessionId) {
                return s;
            }
        }
        return null;
    }

    private static void init(final ServletContext context) {
        if (sessions == null) {
            sessions = (List<Session>) context.getAttribute(USER_LIST_KEY);
        }

        if (sessions == null) {
            sessions = new ArrayList<Session>();
        }
    }

    private static long nextId(final ServletContext context) {
        init(context);
        return sessions.size();
    }
    // </editor-fold>
}
