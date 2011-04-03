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
import org.structr.context.SessionMonitor;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.node.FindUserCommand;

/**
 * Logs current user out
 *
 * @author axel
 */
public class Logout extends WebNode {

    private static final Logger logger = Logger.getLogger(Logout.class.getName());
    private final static String ICON_SRC = "/images/door_out.png";

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

            HttpServletRequest request = getRequest();

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
