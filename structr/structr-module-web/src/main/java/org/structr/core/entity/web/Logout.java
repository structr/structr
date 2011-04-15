/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.web;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.structr.common.SessionContext;
import org.structr.context.SessionMonitor;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 * Logs current user out
 *
 * @author axel
 */
public class Logout extends WebNode {

    private static final Logger logger = Logger.getLogger(Logout.class.getName());
    private final static String ICON_SRC = "/images/door_out.png";

    /** Page to forward to after logout */
    public static final String FORWARD_PAGE_KEY = "forwardPage";

    public String getForwardPage() {
        return getStringProperty(FORWARD_PAGE_KEY);
    }

    public void setForwardPage(final String value) {
        setProperty(FORWARD_PAGE_KEY, value);
    }

    @Override
    public String getIconSrc() {
        return ICON_SRC;
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

//        String errorMsg;

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
                logout(user, session);
                out.append("<div class=\"okMsg\">").append("User ").append(usernameFromSession).append(" logged out").append("</div>");
                logger.log(Level.INFO, "User {0} logged out", usernameFromSession);
                return;
            }

        }
    }

    private void logout(final User user, HttpSession session) {

        String sessionId = session.getId();

        // Clear username in session
        session.removeAttribute(USERNAME_KEY);

        // Invalidate (destroy) session
        session.invalidate();

        SessionMonitor.logActivity(user, SessionMonitor.getSessionByUId(sessionId), "Logout");

    }

}
