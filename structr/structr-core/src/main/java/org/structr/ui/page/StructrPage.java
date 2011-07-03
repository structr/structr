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



package org.structr.ui.page;

import org.apache.click.Page;
import org.apache.click.control.PageLink;
import org.apache.click.service.ConfigService;
import org.apache.click.util.Bindable;
import org.apache.click.util.ClickUtils;
import org.apache.commons.lang.StringUtils;

import org.structr.common.CurrentRequest;
import org.structr.common.CurrentSession;
import org.structr.common.TreeHelper;
import org.structr.context.SessionMonitor;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Group;
import org.structr.core.entity.Link;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
import org.structr.core.node.FindGroupCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.FindUserCommand;
import org.structr.ui.page.admin.DefaultEdit;
import org.structr.ui.page.admin.Edit;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpSession;

//~--- classes ----------------------------------------------------------------

/**
 * Basic page template for structr framework without security restrictions.
 *
 * Most constants are defined here.
 *
 * @author amorgner
 */
public class StructrPage extends Page {

	protected static final String EDIT_MODE        = "edit";
	protected final static String EDIT_NODE_ID_KEY = "editNodeId";
	protected final static String END_NODE_KEY     = "endNode";
	protected static final String ERROR_MSG_KEY    = "errorMsg";
	protected static final String INLINE_MODE      = "inline";
	protected final static String KEY_KEY          = "key";

	/** key for last visited node stored in session */
	protected final static String LAST_VISITED_NODE_KEY = "lastVisitedNode";
	protected static final String LOCAL_MODE            = "local";
	protected final static String NODE_ID_KEY           = "nodeId";
	protected static final String OK_MSG_KEY            = "okMsg";
	protected final static String PARENT_NODE_ID_KEY    = "parentNodeId";
	protected static final String PUBLIC_MODE           = "public";
	protected final static String RECURSIVE_KEY         = "recursive";
	protected final static String RELATIONSHIP_ID_KEY   = "relationshipId";
	protected final static String REL_ATTRS_KEY         = "properties";
	protected final static String REL_POSITION_KEY      = "relPosition";
	protected final static String REL_TYPE_KEY          = "relType";
	protected final static String RENDER_MODE_KEY       = "renderMode";
	protected final static String RETURN_URL_KEY        = "returnUrl";

//      protected final static String NEW_PARENT_NODE_ID_KEY = "newParentNodeId";
	protected final static String SOURCE_NODE_ID_KEY = "sourceNodeId";
	protected final static String START_NODE_ID_KEY  = "startNodeId";

//      protected final static String END_NODE_ID_KEY = "endNodeId";
	protected final static String START_NODE_KEY       = "startNode";
	protected final static String TARGET_NODE_ID_KEY   = "targetNodeId";
	protected final static String TARGET_SLOT_NAME_KEY = "targetSlotName";

	// protected final static String SUPERUSER_KEY = "superadmin";
	protected final static String USERNAME_KEY = "username";

	/** key for currently logged in users */
	protected final static String USER_LIST_KEY           = "userList";
	protected final static String VALUE_KEY               = "value";
	protected static final String WARN_MSG_KEY            = "warnMsg";
	private static final Logger logger                    = Logger.getLogger(StructrPage.class.getName());
	protected final static String SUPERADMIN_USERNAME_KEY = Services.getSuperuserUsername();
	protected final static String SUPERADMIN_PASSWORD_KEY = Services.getSuperuserPassword();
	@Bindable
	public static String contextPath;

	//~--- fields ---------------------------------------------------------

	@Bindable
	protected String errorMsg = "";

	// @Bindable
	// protected String infoMsg = "";
	@Bindable
	protected String okMsg   = "";
	@Bindable
	protected String warnMsg = "";

	// TODO: move to global configuration
	protected final String FILES_PATH;
	protected boolean accessControlAllowed;
	protected boolean addRelationshipAllowed;

	// @Bindable
	// protected Table pageListTable = new Table();
	// cached list with all avaliable page classes
	private List<Class<? extends Page>> configuredPageClasses;
	protected boolean createNodeAllowed;
	protected boolean deleteNodeAllowed;
	@Bindable
	protected Long editNodeId;
	protected boolean editPropertiesAllowed;
	protected boolean editVisibilityAllowed;
	@Bindable
	protected boolean isSuperUser;

	/** current node */
	@Bindable
	protected AbstractNode node;
	@Bindable
	protected String nodeId;

	/** id of parent node (needed for link deletion */
	@Bindable
	protected String parentNodeId;
	protected boolean readAllowed;
	protected boolean removeRelationshipAllowed;
	@Bindable
	protected String renderMode;
	@Bindable
	protected String returnUrl;

	/** root node */
	@Bindable
	protected AbstractNode rootNode;
	protected long sessionId;
	protected boolean showTreeAllowed;
	@Bindable
	protected String title;
	@Bindable
	protected User user;
	@Bindable
	protected String userName;
	protected boolean writeAllowed;

	//~--- constructors ---------------------------------------------------

	public StructrPage() {

		super();

		// prepare global structr request context for this request and this thread
		CurrentRequest.setRequest(getContext().getRequest());
		CurrentRequest.setResponse(getContext().getResponse());
		contextPath = getContext().getRequest().getContextPath();
		FILES_PATH  = Services.getFilesPath();

		// Command graphDbCommand = Services.command(GraphDatabaseCommand.class);
		// graphDb = (GraphDatabaseService)graphDbCommand.execute();
		// userName = getContext().getRequest().getRemoteUser();
		// userName = (String) getContext().getRequest().getSession().getAttribute(USERNAME_KEY);
		userName = getUsernameFromSession();
        user     = getUserNode();

        if (user != null) {

			CurrentRequest.setCurrentUser(user);

			// sessionId = (Long) getContext().getRequest().getSession().getAttribute(SessionMonitor.SESSION_ID);
			sessionId = (Long) CurrentSession.getAttribute(SessionMonitor.SESSION_ID);
			SessionMonitor.logPageRequest(sessionId, "Page Request", getContext().getRequest());
		}


		if ((userName != null) && userName.equals(SUPERADMIN_USERNAME_KEY)) {
			isSuperUser = true;
		}

//              pageListTable.addColumn(new Column("canonicalName"));
//              pageListTable.setDataProvider(new DataProvider() {
//
//                  @Override
//                  public List<Class<? extends Page>> getData() {
//                      return getPageClassList();
//                  }
//              });
	}

	//~--- methods --------------------------------------------------------

	@Override
	public void onInit() {

		super.onInit();
		CurrentRequest.setCurrentNodePath(nodeId);

		// Catch both, id and path
		node = getNodeByIdOrPath(nodeId);

		if (node == null) {
			return;
		}

		// Internally, use node ids
		nodeId = node.getIdString();

		if (isSuperUser) {

			readAllowed               = true;
			showTreeAllowed           = true;
			writeAllowed              = true;
			accessControlAllowed      = true;
			createNodeAllowed         = true;
			deleteNodeAllowed         = true;
			editPropertiesAllowed     = true;
			editVisibilityAllowed     = true;
			addRelationshipAllowed    = true;
			removeRelationshipAllowed = true;

		} else if ((user != null) && (node != null)) {

			readAllowed               = node.readAllowed();
			showTreeAllowed           = node.showTreeAllowed();
			writeAllowed              = node.writeAllowed();
			accessControlAllowed      = node.accessControlAllowed();
			createNodeAllowed         = node.createSubnodeAllowed();
			deleteNodeAllowed         = node.deleteNodeAllowed();
			editPropertiesAllowed     = node.editPropertiesAllowed();
			editVisibilityAllowed     = node.editPropertiesAllowed();    // TODO: add access rights for visibility
			addRelationshipAllowed    = node.addRelationshipAllowed();
			removeRelationshipAllowed = node.removeRelationshipAllowed();
		}

		// call request cycle listener
		CurrentRequest.onRequestStart();
	}

	@Override
	public void onDestroy() {
		CurrentRequest.onRequestEnd();
	}

	/**
	 * @see Page#onSecurityCheck()
	 */
	@Override
	public boolean onSecurityCheck() {

		// userName = getContext().getRequest().getRemoteUser();
		if (userName != null) {
			return true;
		} else {

			Map<String, String> parameters = new HashMap<String, String>();
			PageLink returnLink            = new PageLink("Return Link", getClass());

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

		if ((user != null) &&!(user instanceof SuperUser)) {

			String lastVisitedNodeId = (String) user.getProperty(LAST_VISITED_NODE_KEY);

			return ((lastVisitedNodeId == null)
				? "0"
				: lastVisitedNodeId);
		}

		return "0";
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

	//~--- get methods ----------------------------------------------------

	protected String getNodeId() {
		return nodeId;
	}

	/**
	 * Locates and returns the node with the given id or path.
	 *
	 * @param nodeIdOrPath
	 * @return
	 */
	protected final AbstractNode getNodeByIdOrPath(Object nodeIdOrPath) {

		if (nodeIdOrPath != null) {

			if (nodeIdOrPath instanceof String) {

				String nodeIdString = (String) nodeIdOrPath;

				try {

					if (nodeIdString.startsWith("/")) {

						AbstractNode byPathNode = TreeHelper.getNodeByPath(getRootNode(),
										  nodeIdString, true);

						if (byPathNode != null) {
							return byPathNode;
						}

						// If node not found by path, try to parse the path string as numerical id
						String byNumber           = StringUtils.remove(nodeIdString, "/");
						AbstractNode byNumberNode = getNodeById(Long.parseLong(byNumber));

						if (byNumberNode != null) {
							return byNumberNode;
						}
					}

					return getNodeById(Long.parseLong((String) nodeIdOrPath));

				} catch (Exception e) {

					logger.log(Level.SEVERE, "Could not handle nodeId {0}", nodeIdOrPath);

					return null;
				}

			} else if (nodeIdOrPath instanceof Long) {
				return getNodeById((Long) nodeIdOrPath);
			} else {

				logger.log(Level.SEVERE, "Node requested by unknown object: {0}", nodeIdOrPath);

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

		Class<? extends Page> c = Edit.class;

		c = getEditPageClass(node);

		return c;
	}

	protected AbstractNode getRootNode() {

		Command findNode = Services.command(FindNodeCommand.class);

		if ((user != null) &&!(user instanceof SuperUser)) {
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

		if (userName.equals(SUPERADMIN_USERNAME_KEY)) {
			return new SuperUser();
		}

		return (User) Services.command(FindUserCommand.class).execute(userName);
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
	 * Return all groups
	 *
	 * @return
	 */
	protected List<Group> getAllGroups() {

		Command findGroup = Services.command(FindGroupCommand.class);

		return ((List<Group>) findGroup.execute());
	}

	/**
	 * Return user session
	 *
	 * @return
	 */
	private HttpSession getSession() {
		return getContext().getSession();
	}

	/**
	 * Return username saved in session
	 *
	 * @return
	 */
	protected String getUsernameFromSession() {
		return (String) getSession().getAttribute(USERNAME_KEY);
	}

	//~--- set methods ----------------------------------------------------

	/**
	 * Save given username in session
	 *
	 * @param username
	 */
	protected void setUsernameInSession(final String username) {
		getSession().setAttribute(USERNAME_KEY, username);
	}
}
