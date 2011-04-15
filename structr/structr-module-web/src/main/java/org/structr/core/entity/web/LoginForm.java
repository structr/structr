/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;
import org.structr.common.SessionContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 * Render a login form.
 *
 * Username and password field names have to match with a corresponding
 * LoginCheck node to make login work.
 *
 * @author axel
 */
public class LoginForm extends Form {

    protected final static String defaultUsernameFieldName = "loginForm_username";
    protected final static String defaultPasswordFieldName = "loginForm_password";
    /** Name of username field */
    public final static String USERNAME_FIELD_NAME_KEY = "usernameFieldName";
    /** Name of password field */
    public final static String PASSWORD_FIELD_NAME_KEY = "passwordFieldName";

    private final static String ICON_SRC = "/images/form.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
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
        setProperty(PASSWORD_FIELD_NAME_KEY, value);
    }

    /**
     * Render edit view
     *
     * @param out
     * @param startNode
     * @param editUrl
     * @param editNodeId
     */
    @Override
    public void renderView(StringBuilder out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId, final User user) {

        // if this page is requested to be edited, render edit frame
        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

        } else {

            HttpServletRequest request = SessionContext.getRequest();

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
                out.append("<div class=\"okMsg\">").append("Your are logged in as ").append(usernameFromSession).append("</div>");
                return;
            }

            Boolean sessionBlocked = (Boolean) session.getAttribute(SESSION_BLOCKED);

            if (Boolean.TRUE.equals(sessionBlocked)) {
                out.append("<div class=\"errorMsg\">").append("Too many login attempts, session is blocked for login").append("</div>");
                return;
            }

            // Get values from config page, or defaults
            String action = getAction() != null ? getAction() : defaultAction;
            String submitButtonName = getSubmitButtonName() != null ? getSubmitButtonName() : defaultSubmitButtonName;
            String antiRobotFieldName = getAntiRobotFieldName() != null ? getAntiRobotFieldName() : defaultAntiRobotFieldName;
            String usernameFieldName = getUsernameFieldName() != null ? getUsernameFieldName() : defaultUsernameFieldName;
            String passwordFieldName = getPasswordFieldName() != null ? getPasswordFieldName() : defaultPasswordFieldName;
            String cssClass = getCssClass() != null ? getCssClass() : defaultCssClass;
            String label = getLabel() != null ? getLabel() : defaultLabel;
            
            String username = StringUtils.trimToEmpty(param(usernameFieldName));
            String password = StringUtils.trimToEmpty(param(passwordFieldName));

            out.append("<form name=\"").append(getName()).append("\" action=\"").append(action).append("\" method=\"post\">");
            out.append("<input type=\"hidden\" name=\"").append(antiRobotFieldName).append("\" value=\"\">");
            out.append("<table class=\"").append(cssClass).append("\">");
            out.append("<tr><th><span class=\"heading\">").append(label).append("</span></th><th></th></tr>");
            out.append("<tr><td class=\"label\">Username</td></tr>");
            out.append("<tr><td class=\"field\"><input type=\"text\" name=\"").append(usernameFieldName).append("\" value=\"").append(username).append("\" size=\"30\"></td></tr>");
            out.append("<tr><td class=\"label\">Password</td></tr>");
            out.append("<tr><td class=\"field\"><input type=\"password\" name=\"").append(passwordFieldName).append("\" value=\"").append(password).append("\" size=\"30\"></td></tr>");
            out.append("<tr><td class=\"button\"><input type=\"submit\" name=\"").append(submitButtonName).append("\" value=\"Submit\"></td></tr>");
            out.append("</table>");
            out.append("</form>");

        }
    }
}
