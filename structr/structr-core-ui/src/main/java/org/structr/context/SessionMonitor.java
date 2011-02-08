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
import org.structr.core.entity.User;

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
        private List<Activity> activityList;
        private String remoteAddr;
        private String remoteHost;
        private String remoteUser;

        public Session(final long id, String uid, final User user, Date loginTime) {
            this.id = id;
            this.uid = uid;
            this.state = State.ACTIVE;
            this.user = user;
            this.loginTimestamp = loginTime;
            activityList = new ArrayList<Activity>();
        }

        /**
         * @return last activity
         */
        public String getLastActivity() {
            // Assuming minium activity list size of 1 is safe because first activity is always a login
            Activity lastActivity = getActivityList().get(getActivityList().size() - 1);

            return lastActivity.getAction();
        }

        /**
         * @return last activity timestamp
         */
        public Date getLastActivityTimestamp() {
            // Assuming minium activity list size of 1 is safe because first activity is always a login
            Activity lastActivity = getActivityList().get(getActivityList().size() - 1);

            return lastActivity.getTimestamp();
        }

        /**
         * @return last activity URI
         */
        public String getLastActivityUri() {
            // Assuming minium activity list size of 1 is safe because first activity is always a login
            Activity lastActivity = getActivityList().get(getActivityList().size() - 1);

            return lastActivity.getUri();
        }

        /**
         * Return number of seconds since last activity
         * @return seconds since last activity
         */
        public long getInactiveSince() {
            long ms = (new Date()).getTime() - getLastActivityTimestamp().getTime();
            return ms / 1000;
        }

        /**
         * @return the user
         */
        private User getUser() {
            return user;
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
        public List<Activity> getActivityList() {
            return activityList;
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

        /**
         * @return the remoteAddr
         */
        public String getRemoteAddr() {
            return remoteAddr;
        }

        /**
         * @param remoteAddr the remoteAddr to set
         */
        public void setRemoteAddr(final String remoteAddr) {
            this.remoteAddr = remoteAddr;
        }

        /**
         * @return the remoteHost
         */
        public String getRemoteHost() {
            return remoteHost;
        }

        /**
         * @param remoteHost the remoteHost to set
         */
        public void setRemoteHost(final String remoteHost) {
            this.remoteHost = remoteHost;
        }

        /**
         * @return the remoteUser
         */
        public String getRemoteUser() {
            return remoteUser;
        }

        /**
         * @param remoteUser the remoteUser to set
         */
        public void setRemoteUser(String remoteUser) {
            this.remoteUser = remoteUser;
        }
    }// </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Activity">
    public static class Activity {

        private String action;
        private State state;
        private Date timestamp;
        private String uri;
//        private Date endTime;

        public Activity(final String action, final Date timestamp, final String uri) {
            this.action = action;
            this.timestamp = timestamp;
            this.state = State.UNDEFINED;
            this.uri = uri;
        }

        /**
         * @return the URI
         */
        public String getUri() {
            return uri;
        }

        /**
         * @param URI the URI to set
         */
        public void setUri(String uri) {
            this.uri = uri;
        }

        /**
         * @return the action
         */
        public String getAction() {
            return action;
        }

        /**
         * @param action the action to set
         */
        public void setAction(String action) {
            this.action = action;
        }

        /**
         * @return the state
         */
        public State getState() {
            return state;
        }

        /**
         * @param state the state to set
         */
        public void setState(State state) {
            this.state = state;
        }

        /**
         * @return the time
         */
        public Date getTimestamp() {
            return timestamp;
        }

        /**
         * @param timestamp the time to set
         */
        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
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
    public static void logActivity(final long sessionId, final String action, final HttpServletRequest request) {
        if (request != null) {
            getSession(sessionId).addActivity(new Activity(action, new Date(), request.getRequestURI()));
            getSession(sessionId).setRemoteAddr(request.getRemoteAddr());
            getSession(sessionId).setRemoteHost(request.getRemoteHost());
            getSession(sessionId).setRemoteUser(request.getRemoteUser());
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
