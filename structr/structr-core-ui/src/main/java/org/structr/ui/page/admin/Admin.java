/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import org.apache.click.control.ActionLink;
import org.apache.click.control.PageLink;
import org.apache.click.control.Panel;

import org.apache.click.util.Bindable;
import org.apache.click.extras.tree.Tree;
import org.apache.click.extras.tree.TreeNode;
import org.apache.commons.lang.ArrayUtils;
import org.structr.core.entity.StructrNode;
import org.structr.core.entity.SuperUser;
import org.structr.ui.page.StructrPage;

/**
 * Base class for admin pages.
 * 
 * @author amorgner
 */
public class Admin extends StructrPage {

    // TODO: move to global configuration
    public static final Integer THUMBNAIL_WIDTH = 100;
    public static final Integer THUMBNAIL_HEIGHT = 100;
    public static final Integer PREVIEW_WIDTH = 600;
    public static final Integer PREVIEW_HEIGHT = 400;

    public static final Integer DEFAULT_PAGESIZE = 25;
    public static final Integer DEFAULT_PAGER_MIN = 5;
    public static final Integer DEFAULT_PAGER_MAX = 1000;
    /** key for expanded nodes stored in session */
    public final static String EXPANDED_NODES_KEY = "expandedNodes";
    /** list with ids of open nodes */
    protected List<TreeNode> openNodes;
    @Bindable
    protected Tree nodeTree;

    // use template for backend pages
    @Override
    public String getTemplate() {
        return "/admin-edit-template.htm";
    }
    @Bindable
    protected PageLink rootLink = new PageLink(Nodes.class);
    @Bindable
    protected ActionLink logoutLink = new ActionLink("logoutLink", "Logout", this, "onLogout");
    @Bindable
    protected Panel actionsPanel = new Panel("actionsPanel", "/panel/actions-panel.htm");
    protected final Locale locale = getContext().getLocale();
    protected final SimpleDateFormat dateFormat = new SimpleDateFormat();

//    protected final SimpleDateFormat dateFormat =
//            (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
    public Admin() {

        super();

        title = "STRUCTR Admin Console";

    }

    @Override
    public void onInit() {

        super.onInit();

        logoutLink.setParameter(NODE_ID_KEY, getNodeId());
    }

    /**
     * Logout: invalidate session and clear user name
     *
     * @return
     */
    public boolean onLogout() {

        getContext().getRequest().getSession().invalidate();
        userName = null;

        setRedirect(getRedirectPage(getNodeByIdOrPath(getNodeId()), this));

        return false;
    }

    /**
     * Reset expanded nodes (clear values stored in session)
     */
    protected void resetExpandedTreeNodes() {

        getContext().getSession().setAttribute(EXPANDED_NODES_KEY, null);
    }

    /**
     * Store a list (String[]) with expanded nodes in the user profile
     */
    protected void storeExpandedNodesInUserProfile(long[] expandedNodesArray) {

        if (user != null && !(user instanceof SuperUser)) {
            user.setProperty(EXPANDED_NODES_KEY, expandedNodesArray);
        }

    }

    /**
     * Store a list (String[]) with expanded nodes in the user profile
     */
    protected void storeExpandedNodesInUserProfile() {

        List<Long> expandedNodes = new ArrayList<Long>();
        //long[] expandedNodes = new long[];
        for (TreeNode n : nodeTree.getExpandedNodes(true)) {

            StructrNode s = (StructrNode) n.getValue();
            expandedNodes.add(s.getId());

        }

        long[] expandedNodesArray = ArrayUtils.toPrimitive(expandedNodes.toArray(new Long[expandedNodes.size()]));

        storeExpandedNodesInUserProfile(expandedNodesArray);

    }

    /**
     * Store last visited node in the user profile
     */
    protected void storeLastVisitedNodeInUserProfile() {
        if (user != null && !(user instanceof SuperUser)) {
            user.setProperty(LAST_VISITED_NODE_KEY, nodeId);
        }
    }

    /**
     * Restore a list (String[]) with expanded nodes from the user profile
     */
    protected long[] getExpandedNodesFromUserProfile() {

        long[] expandedNodesArray = null;

        if (user != null && !(user instanceof SuperUser)) {
            try {
                expandedNodesArray = (long[]) user.getProperty(EXPANDED_NODES_KEY);

            } catch (Exception e) {

                logger.log(Level.WARNING, "Could not load expanded nodes as long[]\n{0}", e.getMessage());

                Object expandedNodesProperty = user.getProperty(EXPANDED_NODES_KEY);

                if (expandedNodesProperty != null) {

                    if (expandedNodesProperty instanceof String[]) {

                        String[] nodeIds = (String[]) expandedNodesProperty;
                        expandedNodesArray = new long[nodeIds.length];

                        for (int i = 0; i < nodeIds.length; i++) {
                            expandedNodesArray[i] = Long.parseLong(nodeIds[i]);
                        }

                        // convert existing String-based lists to Long-based
                        storeExpandedNodesInUserProfile(expandedNodesArray);

                    } else if (expandedNodesProperty instanceof Long[]) {

                        Long[] nodeIds = (Long[]) expandedNodesProperty;
                        expandedNodesArray = new long[nodeIds.length];

                        for (int i = 0; i < nodeIds.length; i++) {
                            expandedNodesArray[i] = nodeIds[i].longValue();
                        }

                        // convert existing String-based lists to Long-based
                        storeExpandedNodesInUserProfile(expandedNodesArray);

                    }
                }
            }


        }
        return expandedNodesArray;
    }
}
