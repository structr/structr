/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.entity.AbstractNode;
import org.apache.click.Page;
import org.apache.click.control.PageLink;
import org.apache.click.service.ConfigService;
import org.apache.click.util.Bindable;
import org.apache.click.util.ClickUtils;
import org.apache.commons.lang.StringUtils;
import org.structr.common.TreeHelper;
import org.structr.context.SessionMonitor;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.Link;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.FindUserCommand;
import org.structr.ui.page.admin.DefaultEdit;
import org.structr.ui.page.admin.Edit;

/**
 * Basic page template for structr framework without security restrictions.
 *
 * Most constants are defined here.
 *
 * @author amorgner
 */
public class StructrPage extends Page {

    private static final Logger logger = Logger.getLogger(StructrPage.class.getName());
    @Bindable
    protected String title;
    @Bindable
    protected String nodeId;
    @Bindable
    protected Long editNodeId;
    @Bindable
    protected String renderMode;
    @Bindable
    protected String userName;
    protected long sessionId;
    @Bindable
    protected User user;
    protected boolean isSuperUser;
    protected boolean readAllowed;
    protected boolean showTreeAllowed;
    protected boolean writeAllowed;
    protected boolean createNodeAllowed;
    protected boolean deleteNodeAllowed;
    protected boolean editPropertiesAllowed;
    protected boolean editVisibilityAllowed;
    protected boolean addRelationshipAllowed;
    protected boolean removeRelationshipAllowed;
    protected boolean accessControlAllowed;
    /** root node */
    @Bindable
    protected AbstractNode rootNode;
    /** current node */
    @Bindable
    protected AbstractNode node;
    /** id of parent node (needed for link deletion */
    @Bindable
    protected String parentNodeId;
    @Bindable
    protected String returnUrl;
    protected final static String SUPERUSER_KEY = "superadmin";
    protected final static String USERNAME_KEY = "username";
    protected final static String KEY_KEY = "key";
    protected final static String VALUE_KEY = "value";
    protected final static String NODE_ID_KEY = "nodeId";
    protected final static String EDIT_NODE_ID_KEY = "editNodeId";
    protected final static String RETURN_URL_KEY = "returnUrl";
    protected final static String RENDER_MODE_KEY = "renderMode";
    protected final static String START_NODE_ID_KEY = "startNodeId";
    protected final static String END_NODE_ID_KEY = "endNodeId";
    protected final static String START_NODE_KEY = "startNode";
    protected final static String END_NODE_KEY = "endNode";
    protected final static String REL_TYPE_KEY = "relType";
    protected final static String REL_POSITION_KEY = "relPosition";
    protected final static String RELATIONSHIP_ID_KEY = "relationshipId";
    protected final static String NEW_PARENT_NODE_ID_KEY = "newParentNodeId";
    protected final static String TARGET_NODE_ID_KEY = "targetNodeId";
    protected final static String PARENT_NODE_ID_KEY = "parentNodeId";
    protected final static String RECURSIVE_KEY = "recursive";
    protected static final String EDIT_MODE = "edit";
    protected static final String LOCAL_MODE = "local";
    protected static final String PUBLIC_MODE = "public";
    protected static final String INLINE_MODE = "inline";
    protected static final String ERROR_MSG_KEY = "errorMsg";
    protected static final String OK_MSG_KEY = "okMsg";
    protected static final String WARN_MSG_KEY = "warnMsg";
    /** key for last visited node stored in session */
    protected final static String LAST_VISITED_NODE_KEY = "lastVisitedNode";
    /** key for currently logged in users */
    protected final static String USER_LIST_KEY = "userList";
    // TODO: move to global configuration
    protected final String FILES_PATH;
    @Bindable
    public static String contextPath;
    @Bindable
    protected String errorMsg = "";
    //@Bindable
    //protected String infoMsg = "";
    @Bindable
    protected String okMsg = "";
    @Bindable
    protected String warnMsg = "";
    //@Bindable
    //protected Table pageListTable = new Table();
    // cached list with all avaliable page classes
    private List<Class<? extends Page>> configuredPageClasses;

    public StructrPage() {

        super();

        contextPath = getContext().getRequest().getContextPath();
        FILES_PATH = Services.getFilesPath();

        //Command graphDbCommand = Services.command(GraphDatabaseCommand.class);
        //graphDb = (GraphDatabaseService)graphDbCommand.execute();

        //userName = getContext().getRequest().getRemoteUser();
        userName = (String) getContext().getRequest().getSession().getAttribute(USERNAME_KEY);
        user = getUserNode();

        if (userName != null && userName.equals(SUPERUSER_KEY)) {
            isSuperUser = true;
        }




//        pageListTable.addColumn(new Column("canonicalName"));
//        pageListTable.setDataProvider(new DataProvider() {
//
//            @Override
//            public List<Class<? extends Page>> getData() {
//                return getPageClassList();
//            }
//        });


    }

    @Override
    public void onInit() {

        super.onInit();


        if (user != null) {
            sessionId = (Long) getContext().getRequest().getSession().getAttribute(SessionMonitor.SESSION_ID);
            SessionMonitor.logPageRequest(user, sessionId, "Page Request", getContext().getRequest());
        }

        // Catch both, id and path
        node = getNodeByIdOrPath(nodeId);

        if (node == null) {
            return;
        }

        // Internally, use node ids
        nodeId = node.getIdString();

        if (isSuperUser) {

            readAllowed = true;
            showTreeAllowed = true;
            writeAllowed = true;
            accessControlAllowed = true;
            createNodeAllowed = true;
            deleteNodeAllowed = true;
            editPropertiesAllowed = true;
            editVisibilityAllowed = true;
            addRelationshipAllowed = true;
            removeRelationshipAllowed = true;

        } else if (user != null && node != null) {

            readAllowed = node.readAllowed(user);
            showTreeAllowed = node.showTreeAllowed(user);
            writeAllowed = node.writeAllowed(user);
            accessControlAllowed = node.accessControlAllowed(user);
            createNodeAllowed = node.createSubnodeAllowed(user);
            deleteNodeAllowed = node.deleteNodeAllowed(user);
            editPropertiesAllowed = node.editPropertiesAllowed(user);
            editVisibilityAllowed = node.editPropertiesAllowed(user);// TODO: add access rights for visibility
            addRelationshipAllowed = node.addRelationshipAllowed(user);
            removeRelationshipAllowed = node.removeRelationshipAllowed(user);

        }

    }

    /**
     * @see Page#onSecurityCheck()
     */
    @Override
    public boolean onSecurityCheck() {

        //userName = getContext().getRequest().getRemoteUser();
        if (userName != null) {
            return true;

        } else {
            Map<String, String> parameters = new HashMap<String, String>();
            PageLink returnLink = new PageLink("Return Link", getClass());
            returnLink.setParameter(NODE_ID_KEY, getNodeId());

            parameters.put(RETURN_URL_KEY, returnLink.getHref());

            setRedirect(LoginPage.class, parameters);
            return false;
        }
    }

    /**
     * Restore last visited node from user profile
     */
    protected String restoreLastVisitedNodeFromUserProfile() {
        if (user != null && !(user instanceof SuperUser)) {
            String lastVisitedNodeId = (String) user.getProperty(LAST_VISITED_NODE_KEY);
            return (lastVisitedNodeId == null ? "0" : lastVisitedNodeId);
        }
        return "0";
    }

    protected String getNodeId() {
        return nodeId;
    }

    protected final AbstractNode getNodeByIdOrPath(Object nodeId) {

        if (nodeId != null) {
            if (nodeId instanceof String) {

                String nodeIdString = (String) nodeId;

                try {

                    if (nodeIdString.startsWith("/")) {

                        AbstractNode byPathNode = TreeHelper.getNodeByPath(getRootNode(), nodeIdString, true, user);
                        if (byPathNode != null) {
                            return byPathNode;
                        }

                        // If node not found by path, try to parse the path string as numerical id
                        String byNumber = StringUtils.remove(nodeIdString, "/");
                        AbstractNode byNumberNode = getNodeById(Long.parseLong(byNumber));
                        if (byNumberNode != null) {
                            return byNumberNode;
                        }

                    }

                    return getNodeById(Long.parseLong((String) nodeId));

                } catch (Exception e) {

                    logger.log(Level.SEVERE, "Could not handle nodeId {0}", nodeId);
                    return null;

                }

            } else if (nodeId instanceof Long) {
                return getNodeById((Long) nodeId);
            } else {
                logger.log(Level.SEVERE, "Node requested by unknown object: {0}", nodeId);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Locates and returns a node by id or path. Note that this method does
     * not run in a transaction.
     *
     * @param requestedId
     * @return
     */
    protected AbstractNode getNodeById(final Long requestedId) {
        Command findNode = Services.command(FindNodeCommand.class);
        AbstractNode ret = null;

        ret = (AbstractNode) findNode.execute(user, requestedId);

        return (ret);
    }

    /**
     * Locates and returns a node by id or path. Note that this method does
     * not run in a transaction.
     *
     * @param requestedId
     * @return
     */
//    protected AbstractNode getNodeByPath(final String requestedPath) {
//        Command findNode = Services.command(FindNodeCommand.class);
//        AbstractNode ret = null;
//        long id = 0;
//
//        String rpath = requestedPath;
//
//        // remove trailing slash (fixes STRUCTR-7)
//        // FIXME: workaround is temporary, see STRUCTR-10
//        if (rpath.endsWith("/")) {
//            rpath = rpath.substring(0, rpath.lastIndexOf("/"));
//        }
//
//        // parsing to Long failed, try xpath
//        XPath xpath = new XPath();
//        xpath.setPath(rpath);
//
//        List<AbstractNode> nodeList = (List<AbstractNode>) findNode.execute(user, xpath);
//        if (nodeList != null && !nodeList.isEmpty()) {
//            ret = nodeList.get(0);
//        }
//
//        return (ret);
//    }
    /**
     * Return (cached) list with the configured page classes
     * 
     * @return
     */
    public List<Class<? extends Page>> getPageClassList() {

        if (configuredPageClasses == null) {
            ConfigService config = ClickUtils.getConfigService(getContext().getServletContext());
            configuredPageClasses = config.getPageClassList();
        }
        return configuredPageClasses;

    }

    /**
     * Get edit page for the given structr node
     *
     * @param n
     * @return
     */
    public Class<? extends Page> getEditPageClass(final AbstractNode n) {
        Class<? extends Page> ret = getPageClass(n, "Edit");
        if (ret == null) {
            ret = Edit.class;
        }
        return ret;
    }

    /**
     * Get view page for the given structr node
     *
     * @param n
     * @return
     */
//    public Class<? extends Page> getViewPageClass(final AbstractNode n) {
//        Class<? extends Page> ret = getPageClass(n, "View");
//        if (ret == null) {
//            ret = DefaultView.class;
//        }
//        return ret;
//    }
    /**
     * Get page for the given prefix and node
     * 
     * @param n
     * @param prefix
     * @return
     */
    private Class<? extends Page> getPageClass(final AbstractNode n, String prefix) {

        AbstractNode nodeForPage;

        if (n != null) {

            if (n instanceof Link) {
                nodeForPage = ((Link) n).getStructrNode();
            } else {
                nodeForPage = n;
            }

            for (Class<? extends Page> c : getPageClassList()) {

                String nodeSimpleClassName = nodeForPage.getClass().getSimpleName();
                String pageSimpleClassName = c.getSimpleName();

                if (prefix.concat(nodeSimpleClassName).equals(pageSimpleClassName)) {
                    return c;
                }
            }
            logger.log(Level.FINE, "No admin GUI page found for {0}", n.getType());
        }
        return DefaultEdit.class;
    }

    /**
     * Return the matching redirect page for the given node
     *
     * @param node  node to get redirect page for
     * @return
     */
    protected Class<? extends Page> getRedirectPage(AbstractNode node) {
        // decide whether to use a view or an edit page
//        Class<? extends Page> c = DefaultView.class;
//
//        if (page instanceof DefaultView) {
//
//            // we are currently in view mode, so get the corresponding view page
//            c = getViewPageClass(node);
//
//        } else if (page instanceof Edit) {
//
//            // we are currently in edit mode, so get the corresponding edit page
//            c = getEditPageClass(node);
//
//        } else {
//            logger.log(Level.FINE, "No view or edit page found, falling back to default page");
//        }
        Class<? extends Page> c = Edit.class;
        c = getEditPageClass(node);
        return c;
    }

    protected AbstractNode getRootNode() {
        Command findNode = Services.command(FindNodeCommand.class);

        if (user != null && !(user instanceof SuperUser)) {
            rootNode = user.getRootNode();
        }

        if (rootNode == null) {
            // get reference (root) node
            rootNode = (AbstractNode) findNode.execute(user, new Long(0));
        }
        return rootNode;
    }

    /**
     * Return current user node
     *
     * @return
     */
    protected User getUserNode() {
        // don't try to find a user node if userName is null or is superadmin
        if (userName == null) {
            return null;
        }

        if (userName.equals(SUPERUSER_KEY)) {
            return new SuperUser();
        }

        Command findUser = Services.command(FindUserCommand.class);
        return ((User) findUser.execute(userName));
    }

    /**
     * Return href of return link of current page
     * @return
     */
    protected String getReturnUrl() {
        return getReturnLink().getHref();
    }

    /**
     * Return return link of current page
     * @return
     */
    protected PageLink getReturnLink() {
        if (node != null) {
            PageLink returnLink = new PageLink("Return Link", getClass());
            returnLink.setParameter(AbstractNode.NODE_ID_KEY, node.getId());
            return returnLink;
        } else {
            return new PageLink();
        }
    }

    /**
     * Return all users
     *
     * @return
     */
    protected List<User> getAllUsers() {
        Command findUser = Services.command(FindUserCommand.class);
        return ((List<User>) findUser.execute());
    }

    /**
     * General redirect after an edit action
     * 
     * @return
     */
    protected boolean redirect() {

        Map<String, String> parameters = new HashMap<String, String>();

        if (StringUtils.isNotEmpty(okMsg)) {
            parameters.put(OK_MSG_KEY, okMsg);
        }
        if (StringUtils.isNotEmpty(errorMsg)) {
            parameters.put(ERROR_MSG_KEY, errorMsg);
        }

        if (returnUrl != null) {
            setRedirect(returnUrl, parameters);
            setRedirect(getRedirect().concat("#properties-tab"));
        } else {
            // no return url: keep page and
            // set return url
            parameters.put(NODE_ID_KEY, getNodeId());
            setRedirect(getPath(), parameters);
        }

        return false;
    }
}
