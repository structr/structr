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
import org.structr.common.StructrContext;
import org.structr.context.SessionMonitor;
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
    private final static String ICON_SRC = "/images/door_in.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }
    protected final static String defaultSubmitButtonName = LoginForm.defaultSubmitButtonName;
    protected final static String defaultAntiRobotFieldName = LoginForm.defaultAntiRobotFieldName;
    protected final static String defaultUsernameFieldName = LoginForm.defaultUsernameFieldName;
    protected final static String defaultPasswordFieldName = LoginForm.defaultPasswordFieldName;
    protected final static int defaultMaxErrors = 1000;
    protected final static int defaultDelayThreshold = 100;
    protected final static int defaultDelayTime = 3;
    private final static String NUMBER_OF_LOGIN_ATTEMPTS = "numberOfLoginAttempts";
    public final static String SUBMIT_BUTTON_NAME_KEY = "submitButtonName";
    public final static String ANTI_ROBOT_FIELD_NAME_KEY = "antiRobotFieldName";
    private final static String LOGIN_FAILURE_TEXT = "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";
    /** Name of username field */
    public final static String USERNAME_FIELD_NAME_KEY = "usernameFieldName";
    /** Name of password field */
    public final static String PASSWORD_FIELD_NAME_KEY = "passwordFieldName";
    /** Absolute number of login errors (wrong inputs) for a session. Each wrong or missing input field is counted. */
    public final static String MAX_ERRORS_KEY = "maxRetries";
    /** Number of unsuccessful login attempts before retry delay becomes active */
    public final static String DELAY_THRESHOLD_KEY = "delayThreshold";
    /** Time to wait for retry after an unsuccessful login attempt */
    public final static String DELAY_TIME_KEY = "delayTime";

    /**
     * Return name of anti robot field
     *
     * @return
     */
    public String getAntiRobotFieldName() {
        return getStringProperty(ANTI_ROBOT_FIELD_NAME_KEY);
    }

    /**
     * Set name of anti robot field
     *
     * @param value
     */
    public void setAntiRobotFieldName(final String value) {
        setProperty(ANTI_ROBOT_FIELD_NAME_KEY, value);
    }

    /**
     * Return name of submit button
     *
     * @return
     */
    public String getSubmitButtonName() {
        return getStringProperty(SUBMIT_BUTTON_NAME_KEY);
    }

    /**
     * Set name of submit button
     *
     * @param value
     */
    public void setSubmitButtonName(final String value) {
        setProperty(SUBMIT_BUTTON_NAME_KEY, value);
    }

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
    public int getMaxErrors() {
        return getIntProperty(MAX_ERRORS_KEY);
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
    public void setMaxErrors(final int value) {
        setProperty(MAX_ERRORS_KEY, value);
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

        } else {

            HttpServletRequest request = StructrContext.getRequest();

            if (request == null) {
                return;
            }

            HttpSession session = request.getSession();

            if (session == null) {
                return;
            }

            String usernameFromSession = (String) session.getAttribute(USERNAME_KEY);
            Boolean alreadyLoggedIn = usernameFromSession != null;

            if (alreadyLoggedIn) {
//                out.append("<div class=\"errorMsg\">").append("Your are logged in as ").append(usernameFromSession).append("</div>");
                return;
            }

            Boolean sessionBlocked = (Boolean) session.getAttribute(SESSION_BLOCKED);

            if (Boolean.TRUE.equals(sessionBlocked)) {
                out.append("<div class=\"errorMsg\">").append("Too many login attempts, session is blocked for login").append("</div>");
                return;
            }

            // Get values from config page, or defaults
            String submitButtonName = getSubmitButtonName() != null ? getSubmitButtonName() : defaultSubmitButtonName;
            String antiRobotFieldName = getAntiRobotFieldName() != null ? getAntiRobotFieldName() : defaultAntiRobotFieldName;
            String usernameFieldName = getUsernameFieldName() != null ? getUsernameFieldName() : defaultUsernameFieldName;
            String passwordFieldName = getPasswordFieldName() != null ? getPasswordFieldName() : defaultPasswordFieldName;
            int maxRetries = getMaxErrors() > 0 ? getMaxErrors() : defaultMaxErrors;
            int delayThreshold = getDelayThreshold() > 0 ? getDelayThreshold() : defaultDelayThreshold;
            int delayTime = getDelayTime() > 0 ? getDelayTime() : defaultDelayTime;

            String username = request.getParameter(usernameFieldName);
            String password = request.getParameter(passwordFieldName);
            String submitButton = request.getParameter(submitButtonName);
            String antiRobot = request.getParameter(antiRobotFieldName);

            if (StringUtils.isEmpty(submitButton)) {
                // Don't process form at all if submit button was not pressed
                return;
            }

            if (StringUtils.isNotEmpty(antiRobot)) {
                // Don't process form if someone has filled the anti-robot field
                return;
            }

            if (StringUtils.isEmpty(username)) {
                out.append("<div class=\"errorMsg\">").append("You must enter a username").append("</div>");
                countLoginFailure(out, session, maxRetries, delayThreshold, delayTime);
                return;
            }

            if (StringUtils.isEmpty(password)) {
                out.append("<div class=\"errorMsg\">").append("You must enter a password").append("</div>");
                countLoginFailure(out, session, maxRetries, delayThreshold, delayTime);
                return;
            }

            // Session is not blocked, and we have a username and a password

            // First, check if we have a user with this name
            User loginUser = (User) Services.command(FindUserCommand.class).execute(username);

            // No matter what reason to deny login, always show the same error message to
            // avoid giving hints
            errorMsg = LOGIN_FAILURE_TEXT;

            if (loginUser == null) {
                logger.log(Level.INFO, "No user found for name {0}", loginUser);
                out.append("<div class=\"errorMsg\">").append(errorMsg).append("</div>");
                countLoginFailure(out, session, maxRetries, delayThreshold, delayTime);
                return;
            }

            // From here, we have a valid user

            if (loginUser.isBlocked()) {
                logger.log(Level.INFO, "User {0} is blocked", loginUser);
                out.append("<div class=\"errorMsg\">").append(errorMsg).append("</div>");
                countLoginFailure(out, session, maxRetries, delayThreshold, delayTime);
                return;
            }

            // Check password

            String encryptedPasswordValue = DigestUtils.sha512Hex(password);

            if (!encryptedPasswordValue.equals(loginUser.getPassword())) {
                logger.log(Level.INFO, "Wrong password for user {0}", loginUser);
                out.append("<div class=\"errorMsg\">").append(errorMsg).append("</div>");
                countLoginFailure(out, session, maxRetries, delayThreshold, delayTime);
                return;
            }

            // Username and password are both valid
            session.setAttribute(USERNAME_KEY, loginUser.getName());

            // Register user with internal session management
            long sessionId = SessionMonitor.registerUserSession(user, session);
            SessionMonitor.logActivity(user, sessionId, "Login");

            // Mark this session with the internal session id
            session.setAttribute(SessionMonitor.SESSION_ID, sessionId);

            // Clear all blocking stuff
            session.removeAttribute(SESSION_BLOCKED);
            session.removeAttribute(NUMBER_OF_LOGIN_ATTEMPTS);

            out.append("<div class=\"okMsg\">").append("Login successful. Welcome ").append(loginUser.getRealName()).append("!").append("</div>");

        }
    }

    private void countLoginFailure(final StringBuilder out, final HttpSession session, final int maxRetries, final int delayThreshold, final int delayTime) {

        Integer retries = (Integer) session.getAttribute(NUMBER_OF_LOGIN_ATTEMPTS);

        if (retries != null && retries > maxRetries) {

            logger.log(Level.SEVERE, "More than {0} login failures, session {1} is blocked", new Object[]{maxRetries, session.getId()});
            session.setAttribute(SESSION_BLOCKED, true);
            out.append("<div class=\"errorMsg\">").append("Too many unsuccessful login attempts, your session is blocked for login!").append("</div>");
            return;

        } else if (retries != null && retries > delayThreshold) {

            logger.log(Level.INFO, "More than {0} login failures, execution delayed by {1} seconds", new Object[]{delayThreshold, delayTime});

            try {
                Thread.sleep(delayTime * 1000);
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, null, ex);
            }

            out.append("<div class=\"errorMsg\">").append("You had more than ").append(delayThreshold).append(" unsuccessful login attempts, execution was delayed by ").append(delayTime).append(" seconds.").append("</div>");

        } else if (retries != null) {

            session.setAttribute(NUMBER_OF_LOGIN_ATTEMPTS, retries + 1);

        } else {

            session.setAttribute(NUMBER_OF_LOGIN_ATTEMPTS, 1);

        }

    }
}
