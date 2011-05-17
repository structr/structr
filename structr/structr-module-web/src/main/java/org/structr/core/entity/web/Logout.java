/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity.web;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.structr.common.CurrentRequest;
//import org.structr.common.CurrentSession;
import org.structr.common.CurrentSession;
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
            final String editUrl, final Long editNodeId) {

//        String errorMsg;

        // if this page is requested to be edited, render edit frame
        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

        } else {

            HttpServletRequest request = CurrentRequest.getRequest();

            if (request == null) {
                return;
            }

            HttpSession session = request.getSession();

            if (session == null) {
                return;
            }

            String usernameFromSession = (String) session.getAttribute(USERNAME_KEY);
//            String usernameFromSession = CurrentSession.getGlobalUsername();
            Boolean alreadyLoggedIn = usernameFromSession != null;

            if (alreadyLoggedIn) {
                logout(session);
                out.append("<div class=\"okMsg\">").append("User ").append(usernameFromSession).append(" logged out").append("</div>");
                logger.log(Level.INFO, "User {0} logged out", usernameFromSession);
                return;
            }

        }
    }

    private void logout(HttpSession session) {

        String sessionId = session.getId();

        // Clear username in session
        CurrentSession.setGlobalUsername(null);
        session.setAttribute(USERNAME_KEY, null);

        // Invalidate (destroy) session
        session.invalidate();

        SessionMonitor.logActivity(SessionMonitor.getSessionByUId(sessionId), "Logout");

    }

}
