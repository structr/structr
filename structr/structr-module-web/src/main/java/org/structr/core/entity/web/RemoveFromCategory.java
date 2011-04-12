/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.web;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;
import org.structr.common.StructrContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 * Include a node of this type to remove objects from categories of the
 * currently logged in user.
 *
 * See {@link AddToCategory}
 * 
 * @author axel
 */
public class RemoveFromCategory extends WebNode {

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
            Boolean loggedIn = usernameFromSession != null;

            if (!loggedIn) {
                // Return silently when not logged in
                logger.log(Level.WARNING, "Not logged in");
                return;
            }

            Boolean sessionBlocked = (Boolean) session.getAttribute(SESSION_BLOCKED);

            if (Boolean.TRUE.equals(sessionBlocked)) {
                // Return silently when not user is blocked
                logger.log(Level.WARNING, "Session blocked");
                return;
            }

            // Get values from config page, or defaults
            String categoryParameterName = getCategoryParameterName() != null ? getCategoryParameterName() : defaultCategoryParameterName;

            String categoryName = request.getParameter(categoryParameterName);
            String objectId = request.getParameter("id");

            if (StringUtils.isEmpty(categoryName)) {
                // Don't process form if no category name was given
                logger.log(Level.WARNING, "No category given");
                return;
            }

            if (StringUtils.isEmpty(objectId)) {
                // Don't process form if no object id was given
                logger.log(Level.WARNING, "No object id given");
                return;
            }

            user.removeFromCategory(categoryName, objectId);

            // TODO: Give some feedback?

        }
    }
    private static final Logger logger = Logger.getLogger(LoginCheck.class.getName());
    private final static String ICON_SRC = "/images/tag_blue_add.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }
    protected final static String defaultCategoryParameterName = "category";
    /** Name of category parameter */
    public final static String CATEGORY_PARAMETER_NAME_KEY = "categoryParameterName";

    /**
     * Return name of category parameter
     *
     * @return
     */
    public String getCategoryParameterName() {
        return getStringProperty(CATEGORY_PARAMETER_NAME_KEY);
    }

    /**
     * Set name of category parameter
     *
     * @param value
     */
    public void setCategoryParameterName(final String value) {
        setProperty(CATEGORY_PARAMETER_NAME_KEY, value);
    }
}
