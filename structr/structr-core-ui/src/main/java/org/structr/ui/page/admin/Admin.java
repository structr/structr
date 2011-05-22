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
package org.structr.ui.page.admin;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.click.control.ActionLink;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.PageLink;
import org.apache.click.control.Panel;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;
import org.apache.click.extras.control.AutoCompleteTextField;

import org.apache.click.util.Bindable;
import org.apache.click.extras.tree.Tree;
import org.apache.click.extras.tree.TreeNode;
import org.apache.commons.lang.ArrayUtils;
import org.structr.common.CurrentSession;
import org.structr.core.node.search.Search;
import org.structr.context.SessionMonitor;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Link;
import org.structr.core.entity.SuperUser;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.ui.page.LoginPage;
import org.structr.ui.page.StructrPage;

/**
 * Base class for admin pages.
 * 
 * @author amorgner
 */
public class Admin extends StructrPage {

    private static final Logger logger = Logger.getLogger(Admin.class.getName());
    protected static final String TABLE_CLASS = "structr";
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
    protected final static String SEARCH_RESULTS_KEY = "searchResults";
    protected final static String SEARCH_TEXT_KEY = "searchFor";
    /** list with ids of open nodes */
    protected List<TreeNode> openNodes;
    @Bindable
    protected Tree nodeTree;
    @Bindable
    protected Form simpleSearchForm = new Form();
    @Bindable
    protected Panel simpleSearchPanel = new Panel("simpleSearchPanel", "/panel/simple-search-panel.htm");
    @Bindable
    protected TextField searchTextField = new AutoCompleteTextField(SEARCH_TEXT_KEY, "Search for") {

        @Override
        public List<String> getAutoCompleteList(final String criteria) {
            return Search.getNodeNamesLike(criteria);
        }
    };
    @Bindable
    protected List<AbstractNode> searchResults;
    // use template for backend pages

    @Override
    public String getTemplate() {
        return "/admin-edit-template.htm";
    }
    @Bindable
    protected PageLink rootLink = new PageLink(Nodes.class);
    @Bindable
    protected ActionLink logoutLink = new ActionLink("logoutLink", "Logout", this, "onLogout");
    protected PageLink homeLink = new PageLink("homeLink", "Home", DefaultEdit.class);
    protected PageLink usersLink = new PageLink("usersLink", "Users", DefaultEdit.class);
    protected PageLink maintenanceLink = new PageLink("maintenanceLink", "Maintenance", Maintenance.class);
    @Bindable
    protected Panel actionsPanel = new Panel("actionsPanel", "/panel/actions-panel.htm");
    protected final Locale locale = getContext().getLocale();
    protected final SimpleDateFormat dateFormat = new SimpleDateFormat();

//    protected final SimpleDateFormat dateFormat =
//            (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
    public Admin() {
        super();
        title = "STRUCTR Admin Console";

        homeLink.setParameter("nodeId", "0");
        addControl(homeLink);

        addControl(usersLink);

        addControl(maintenanceLink);
    }

    @Override
    public void onInit() {
        super.onInit();
        
        PageLink returnLink = new PageLink("Return Link", getClass());
        returnLink.setParameter(NODE_ID_KEY, getNodeId());
        logoutLink.setParameter(RETURN_URL_KEY, returnLink.getHref());

        simpleSearchForm.add(searchTextField);
        simpleSearchForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
        simpleSearchForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
        simpleSearchForm.setListener(this, "onSimpleSearch");
        simpleSearchForm.setActionURL("search-results.htm#search-tab");
//        simpleSearchForm.add(new Submit("Search", this, "onSimpleSearch"));
        simpleSearchForm.add(new Submit("Search"));

        List<AbstractNode> usersNodes = (List<AbstractNode>) Services.command(SearchNodeCommand.class).execute(user, null, false, false, Search.andExactName("Users"));
        if (!(usersNodes.isEmpty())) {
            usersLink.setParameter("nodeId", usersNodes.get(0).getId());
        }

    }

    /**
     * Logout: invalidate session and clear user name
     *
     * @return
     */
    public boolean onLogout() {

        SessionMonitor.logActivity(sessionId, "Logout");

        getContext().getRequest().getSession().invalidate();
        userName = null;

        CurrentSession.setGlobalUsername(null);

//        if (returnUrl != null) {
//            setRedirect(returnUrl);
//        } else {
//            setRedirect(getRedirectPage(getNodeByIdOrPath(getNodeId()), this));
//        }
        Map<String, String> parameters = new HashMap<String, String>();
        if (returnUrl != null) {
            parameters.put(RETURN_URL_KEY, returnUrl);
        }

        setRedirect(LoginPage.class, parameters);

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

        List<Long> expandedNodes = new LinkedList<Long>();
        //long[] expandedNodes = new long[];
        for (TreeNode n : nodeTree.getExpandedNodes(true)) {

            AbstractNode s = (AbstractNode) n.getValue();
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

    /**
     * Return icon src
     *
     * If node is a link, assemble combined icon src
     * @param n
     * @return
     */
    protected String getIconSrc(AbstractNode n) {
        String iconSrc;
        if (n instanceof Link) {
            iconSrc = getLinkedIconSrc(((Link) n).getStructrNode().getIconSrc());
        } else {
            iconSrc = n.getIconSrc();
        }
        return iconSrc;
    }

    /**
     * Return icon variant
     *
     * @param iconSrc
     * @return
     */
    protected static String getLinkedIconSrc(String iconSrc) {
        int i = iconSrc.lastIndexOf('.');
        String ext = iconSrc.substring(i + 1);
        // TODO move suffix "_linked" to configuration
        iconSrc = iconSrc.substring(0, i) + "_linked." + ext;
        return iconSrc;
    }
}
