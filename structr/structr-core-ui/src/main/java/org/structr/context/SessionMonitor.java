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
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.StructrNode;
import org.structr.core.entity.User;
import org.structr.core.entity.log.Activity;
import org.structr.core.entity.log.LogNodeList;
import org.structr.core.entity.log.PageRequest;
import org.structr.core.node.CreateNodeCommand;
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
        private LogNodeList<Activity> activityList;

        public Session(final long id, String uid, final User user, Date loginTime) {
            this.id = id;
            this.uid = uid;
            this.state = State.ACTIVE;
            this.user = user;
            this.loginTimestamp = loginTime;
            activityList = new LogNodeList<Activity>();
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
         * Return number of seconds since last activity
         * @return seconds since last activity
         */
        public long getInactiveSince() {
            if (hasActivity()) {
                long ms = (new Date()).getTime() - getLastActivityEndTimestamp().getTime();
                return ms / 1000;
            }
            return Long.MIN_VALUE;
        }

        public String getUserName() {
            return user.getName();
        }

        /**
         * @param user the user to set
         */
        public void setUser(final User user) {
            this.user = user;
        }

        /**
         * @return the loginTime
         */
        public Date getLoginTimestamp() {
            return loginTimestamp;
        }

        /**
         * @param loginTimestamp the loginTime to set
         */
        public void setLoginTimestamp(final Date loginTimestamp) {
            this.loginTimestamp = loginTimestamp;
        }

        /**
         * @return the activityList
         */
        public LogNodeList<Activity> getActivityList() {
            return activityList;
        }

        private Activity getLastActivity() {
            if (activityList != null && !(activityList.isEmpty())) {
                return activityList.getLastNode();
            }
            return null;
        }

        /**
         * @param activityList the activityList to set
         */
        public void setActivityList(final List<Activity> activityList) {
            this.setActivityList(activityList);
        }

        /**
         * @return the logoutTimestamp
         */
        public Date getLogoutTimestamp() {
            return logoutTimestamp;
        }

        /**
         * @param logoutTimestamp the logoutTimestamp to set
         */
        public void setLogoutTime(final Date logoutTime) {
            this.logoutTimestamp = logoutTime;
        }

        /**
         * @return the id
         */
        public long getId() {
            return id;
        }

        /**
         * @param id the id to set
         */
        public void setId(final long id) {
            this.id = id;
        }

        /**
         * @return the uid
         */
        public String getUid() {
            return uid;
        }

        /**
         * @param uid the uid to set
         */
        public void setUid(final String uid) {
            this.uid = uid;
        }

        public void addActivity(final Activity activity) {
            activityList.add(activity);
        }

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
                            new NodeAttribute(PageRequest.REMOTE_ADDRESS_KEY, request.getRemoteAddr()),
                            new NodeAttribute(PageRequest.REMOTE_HOST_KEY, request.getRemoteHost()),
                            new NodeAttribute(PageRequest.START_TIMESTAMP_KEY, now),
                            new NodeAttribute(PageRequest.END_TIMESTAMP_KEY, now));

                    Activity activity = new Activity();
                    activity.init(s);

                    getSession(sessionId).getActivityList().add(activity);

                    // TODO: add logging?
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
