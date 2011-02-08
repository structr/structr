/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.context;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletContext;
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
        private User user;
        private Date loginTime;
        private Date logoutTime;
        private List<Activity> activityList;

        public Session(final long id, final User user, Date loginTime) {
            this.id = id;
            this.user = user;
            this.loginTime = loginTime;
            activityList = new ArrayList<Activity>();
        }



        /**
         * @return last activity
         */
        public String getLastActivity() {
            // Assuming minium activity list size of 1 is safe because first activity is always a login
            Activity lastActivity = getActivityList().get(getActivityList().size() - 1);
            
            String lastActivityString = lastActivity.getAction() + " (" + lastActivity.getTime() + ")";
            return lastActivityString;
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
        public void setUser(User user) {
            this.user = user;
        }

        /**
         * @return the loginTime
         */
        public Date getLoginTime() {
            return loginTime;
        }

        /**
         * @param loginTime the loginTime to set
         */
        public void setLoginTime(Date loginTime) {
            this.loginTime = loginTime;
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
        public void setActivityList(List<Activity> activityList) {
            this.setActivityList(activityList);
        }

        /**
         * @return the logoutTime
         */
        public Date getLogoutTime() {
            return logoutTime;
        }

        /**
         * @param logoutTime the logoutTime to set
         */
        public void setLogoutTime(Date logoutTime) {
            this.logoutTime = logoutTime;
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
        public void setId(long id) {
            this.id = id;
        }

        public void addActivity(final Activity activity) {
            activityList.add(activity);
        }
    }// </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Activity">
    public static class Activity {

        private String action;
        private State state;
        private Date time;
//        private Date endTime;

        public Activity(final String action, final Date time) {
            this.action = action;
            this.time = time;
            this.state = State.UNDEFINED;
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
        public Date getTime() {
            return time;
        }

        /**
         * @param time the time to set
         */
        public void setTime(Date time) {
            this.time = time;
        }

    }// </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="State">
    public enum State {

        UNDEFINED, RUNNING, WAITING, FINISHED;
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
    public static long registerUser(final User user, final ServletContext context) {

        init(context);
        long id = nextId(context);
        sessions.add(new Session(id, user, new Date()));
        context.setAttribute(USER_LIST_KEY, sessions);
        return id;

    }

    /**
     * Unregister user in servlet context
     */
    public static void unregisterUser(final long id, final ServletContext context) {

        init(context);
        getSession(id).setLogoutTime(new Date());
        context.setAttribute(USER_LIST_KEY, sessions);

    }

    /**
     * Append an activity to the activity list
     * 
     * @param sessionId
     * @param action
     */
    public static void logActivity(final long sessionId, final String action) {
        getSession(sessionId).addActivity(new Activity(action, new Date()));
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
