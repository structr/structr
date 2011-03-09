/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.web;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.node.FindUserCommand;

/**
 * Checks login credentials.
 *
 * If a user with given username and password exists, and is not blocked,
 * a redirect to the given target node is initiated.
 * 
 * @author axel
 */
public class LoginCheck extends WebNode {

    private static final Logger logger = Logger.getLogger(LoginCheck.class.getName());
    private final static String ICON_SRC = "/images/key.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }
    private final static String defaultUsernameFieldName = "username";
    private final static String defaultPasswordFieldName = "password";
    private final static int defaultMaxRetries = 10;
    private final static int defaultDelayThreshold = 3;
    private final static int defaultDelayTime = 10;
    private final static String NUMBER_OF_LOGIN_ATTEMPTS = "numberOfLoginAttempts";
    private final static String SESSION_BLOCKED = "sessionBlocked";
    private final static String USERNAME_KEY = "username";
    private final static String LOGIN_FAILURE_TEXT = "Wrong username or password!";
    /** Name of username field */
    public final static String USERNAME_FIELD_NAME_KEY = "usernameFieldName";
    /** Name of password field */
    public final static String PASSWORD_FIELD_NAME_KEY = "passwordFieldName";
    /** Absolute number of unsuccessful login attempts for a session */
    public final static String MAX_RETRIES_KEY = "maxRetries";
    /** Number of unsuccessful login attempts before retry delay becomes active */
    public final static String DELAY_THRESHOLD_KEY = "delayThreshold";
    /** Time to wait for retry after an unsuccessful login attempt */
    public final static String DELAY_TIME_KEY = "delayTime";

    /**
     * Return name of username field
     *
     * @return
     */
    public String getUsernameFieldName() {
        return getStringProperty(USERNAME_FIELD_NAME_KEY);
    }

    /**
     * Return name of password field
     *
     * @return
     */
    public String getPasswordFieldName() {
        return getStringProperty(PASSWORD_FIELD_NAME_KEY);
    }

    /**
     * Return number of unsuccessful login attempts for a session
     * 
     * If number is exceeded, login is blocked for this session.
     */
    public int getMaxRetries() {
        return getIntProperty(MAX_RETRIES_KEY);
    }

    /**
     * Return number of unsuccessful login attempts before retry delay becomes active
     */
    public int getDelayThreshold() {
        return getIntProperty(DELAY_THRESHOLD_KEY);
    }

    /**
     * Return time (in seconds) to wait for retry after an unsuccessful login attempt
     */
    public int getDelayTime() {
        return getIntProperty(DELAY_TIME_KEY);
    }

    /**
     * Set name of username field
     *
     * @param value
     */
    public void setUsernameFieldName(final String value) {
        setProperty(USERNAME_FIELD_NAME_KEY, value);
    }

    /**
     * Set name of password field
     *
     * @param value
     */
    public void setPasswordFieldName(final String value) {
        setProperty(USERNAME_FIELD_NAME_KEY, value);
    }

    /**
     * Set number of unsuccessful login attempts for a session.
     *
     * @param value
     */
    public void setMaxRetries(final int value) {
        setProperty(MAX_RETRIES_KEY, value);
    }

    /**
     * Set number of unsuccessful login attempts before retry delay becomes active
     *
     * @param value
     */
    public void setDelayThreshold(final int value) {
        setProperty(DELAY_THRESHOLD_KEY, value);
    }

    /**
     * Set time (in seconds) to wait for retry after an unsuccessful login attempt
     * 
     * @param value
     */
    public void setDelayTime(final int value) {
        setProperty(DELAY_TIME_KEY, value);
    }

    /**
     * Render view
     *
     * @param out
     * @param startNode
     * @param editUrl
     * @param editNodeId
     */
    @Override
    public void renderView(StringBuilder out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId, final User user) {

        String errorMsg;

        // if this page is requested to be edited, render edit frame
        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

            // otherwise, render subnodes in edit mode
        } else {

            HttpServletRequest request = getRequest();

            if (request == null) {
                return;
            }

            HttpSession session = request.getSession();

            if (session == null) {
                return;
            }

            Boolean sessionBlocked = (Boolean) session.getAttribute(SESSION_BLOCKED);

            if (Boolean.TRUE.equals(sessionBlocked)) {
                out.append("<div class=\"errorMsg\">").append("Too many login attempts, session is blocked!").append("</div>");
                return;
            }

            String usernameFieldName = getUsernameFieldName() != null ? getUsernameFieldName() : defaultUsernameFieldName;
            String passwordFieldName = getPasswordFieldName() != null ? getPasswordFieldName() : defaultPasswordFieldName;
            int maxRetries = getMaxRetries() == 0 ? getMaxRetries() : defaultMaxRetries;
            int delayThreshold = getDelayThreshold() == 0 ? getDelayThreshold() : defaultDelayThreshold;
            int delayTime = getDelayTime() == 0 ? getDelayTime() : defaultDelayTime;


            String username = request.getParameter(usernameFieldName);
            String password = request.getParameter(passwordFieldName);

            if (StringUtils.isEmpty(username)) {
                out.append("<div class=\"errorMsg\">").append("You must enter a username").append("</div>");
                return;
            }

            if (StringUtils.isEmpty(password)) {
                out.append("<div class=\"errorMsg\">").append("You must enter a password").append("</div>");
                return;
            }

            Integer retries = (Integer) session.getAttribute(NUMBER_OF_LOGIN_ATTEMPTS);

            if (retries != null && retries > maxRetries) {
                session.setAttribute(SESSION_BLOCKED, true);
                out.append("<div class=\"errorMsg\">").append("Too many unsuccessful login attempts, your session is blocked now!").append("</div>");
                return;
            } else if (retries > delayThreshold) {

                try {
                    Thread.sleep(delayTime * 1000);
                } catch (InterruptedException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }

            } else if (retries != null) {
                session.setAttribute(NUMBER_OF_LOGIN_ATTEMPTS, retries++);
            } else {
                session.setAttribute(NUMBER_OF_LOGIN_ATTEMPTS, 1);
            }

            // Here, the session is not blocked, and
            // we have a username and a password

            // First, check if we have a user with this name
            User loginUser = (User) Services.command(FindUserCommand.class).execute(username);

            if (loginUser == null) {
                out.append("<div class=\"errorMsg\">").append(LOGIN_FAILURE_TEXT).append("</div>");
            }

            // Here, we have a valid user

            // Check password

            if (user == null) {
                logger.log(Level.INFO, "No user found for name {0}", user);
                errorMsg = "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";
                out.append("<div class=\"errorMsg\">").append(errorMsg).append("</div>");
                return;
            }

            if (user.isBlocked()) {
                logger.log(Level.INFO, "User {0} is blocked", user);
                errorMsg = "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";
                out.append("<div class=\"errorMsg\">").append(errorMsg).append("</div>");
                return;
            }

            String encryptedPasswordValue = DigestUtils.sha512Hex(password);

            if (!encryptedPasswordValue.equals(user.getPassword())) {
                logger.log(Level.INFO, "Wrong password for user {0}", user);
                errorMsg = "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";
                out.append("<div class=\"errorMsg\">").append(errorMsg).append("</div>");
                return;
            }

            // username and password are both valid
            session.setAttribute(USERNAME_KEY, username);
            out.append("<div class=\"okMsg\">").append("Successfully logged in").append("</div>");


        }
    }
}
