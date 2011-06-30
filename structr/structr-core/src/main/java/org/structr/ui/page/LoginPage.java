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

import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.click.Page;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Panel;
import org.apache.click.control.PasswordField;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;
import org.apache.click.extras.tree.TreeNode;
import org.apache.commons.lang.StringUtils;
import org.structr.common.CurrentRequest;
import org.structr.common.CurrentSession;
import org.structr.context.SessionMonitor;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.FindUserCommand;
import org.structr.ui.page.admin.Admin;

/**
 *
 * @author amorgner
 */
public class LoginPage extends Admin {

    private static final Logger logger = Logger.getLogger(LoginPage.class.getName());
    //private final static String DOMAIN_KEY = "domain";
    private final static String PASSWORD_KEY = "password";
    protected Panel loginPanel = new Panel("loginPanel", "/panel/login-panel.htm");
    protected Form loginForm = new Form("loginForm");

    // use template for backend pages
    @Override
    public String getTemplate() {
        return "/login.htm";
    }

    public LoginPage() {

        super();

        title = "Login";

        //loginForm.add(new TextField(DOMAIN_KEY, true));
        loginForm.add(new TextField(USERNAME_KEY, "Username", 20, true));
        loginForm.add(new PasswordField(PASSWORD_KEY, "Password", 20, true));
        loginForm.add(new Submit("login", "Login Now!", this, "onLogin"));
        addControl(loginForm);
        addControl(loginPanel);

    }

    @Override
    public void onInit() {
        super.onInit();
        loginForm.add(new HiddenField(RETURN_URL_KEY, returnUrl != null ? returnUrl : ""));

    }

    /**
     * @see Page#onSecurityCheck()
     */
    @Override
    public boolean onSecurityCheck() {
        //userName = (String) getContext().getRequest().getSession().getAttribute(USERNAME_KEY);
        userName = CurrentSession.getGlobalUsername();

        if (userName != null) {
            initFirstPage();
            return false;
        } else {
            return true;
        }
    }

    public boolean onLogin() {

        if (loginForm.isValid()) {

            //String domainValue = loginForm.getFieldValue(DOMAIN_KEY);
            String userValue = loginForm.getFieldValue(USERNAME_KEY);
            returnUrl = loginForm.getFieldValue(RETURN_URL_KEY);
            String passwordValue = loginForm.getFieldValue(PASSWORD_KEY);

            if (SUPERADMIN_USERNAME_KEY.equals(userValue) && SUPERADMIN_PASSWORD_KEY.equals(passwordValue)) {

                logger.log(Level.INFO, "############# Logged in as superadmin! ############");
                userName = SUPERADMIN_USERNAME_KEY;
                isSuperUser = true;

                user = new SuperUser();
                CurrentRequest.setCurrentUser(user) ;
                //getContext().getRequest().getSession().setAttribute(USERNAME_KEY, userValue);
                CurrentSession.setGlobalUsername(userValue);

                Services.initialize();

                // redirect superuser to maintenance
                setRedirect("/admin/dashboard.htm");

            } else {

                Services.initialize();

                Command findUser = Services.command(FindUserCommand.class);

                user = (User) findUser.execute(userValue);//, domainValue);

//                if (domainValue == null) {
//                    logger.log(Level.INFO, "No domain at login");
//                    errorMsg = "No domain";
//                    return true;
//                }

                if (user == null) {
                    logger.log(Level.INFO, "No user found for name {0}", user);
                    errorMsg = "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";
                    return true;
                }

                if (user.isBlocked()) {
                    logger.log(Level.INFO, "User {0} is blocked", user);
                    errorMsg = "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";
                    return true;
                }

                if (passwordValue == null) {
                    logger.log(Level.INFO, "Password for user {0} is null", user);
                    errorMsg = "You should enter a password.";
                    return true;
                }

                String encryptedPasswordValue = DigestUtils.sha512Hex(passwordValue);

                if (!encryptedPasswordValue.equals(user.getEncryptedPassword())) {
                    logger.log(Level.INFO, "Wrong password for user {0}", user);
                    errorMsg = "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";
                    return true;
                }

                // username and password are both valid
                userName = userValue;
                CurrentRequest.setCurrentUser(user) ;
                CurrentSession.setGlobalUsername(userValue);
                //getContext().getRequest().getSession().setAttribute(USERNAME_KEY, userValue);

                initFirstPage();

            }
            
            // Register user with internal session management
            sessionId = SessionMonitor.registerUserSession(getContext().getSession());
            SessionMonitor.logActivity(sessionId, "Login");

            // Mark this session with the internal session id
            //getContext().getRequest().getSession().setAttribute(SessionMonitor.SESSION_ID, sessionId);
            CurrentSession.setAttribute(SessionMonitor.SESSION_ID, sessionId);

            return false;

        }

        return true;
    }

    private void initFirstPage() {

        // if a return URL is present, use it
        if (returnUrl != null && StringUtils.isNotBlank(returnUrl)) {

            setRedirect(returnUrl);

        } else {

            String startNodeId = getNodeId();
            if (startNodeId == null) {
                startNodeId = restoreLastVisitedNodeFromUserProfile();
                nodeId = startNodeId;
            }

            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put(NODE_ID_KEY, String.valueOf(getNodeId()));

            // default after login is edit mode
            Class<? extends Page> editPage = getEditPageClass(getNodeByIdOrPath(nodeId));
            setRedirect(editPage, parameters);
        }

        long[] expandedNodesArray = getExpandedNodesFromUserProfile();
        if (expandedNodesArray != null && expandedNodesArray.length > 0) {

            openNodes = new LinkedList<TreeNode>();

            Command findNode = Services.command(FindNodeCommand.class);
            for (Long s : expandedNodesArray) {

                AbstractNode n = (AbstractNode) findNode.execute(user, s);
                if (n != null) {
                    //openNodes.add(new TreeNode(String.valueOf(n.getId())));
                    openNodes.add(new TreeNode(n, String.valueOf(n.getId())));
                }

            }
            // fill session
            getContext().getSession().setAttribute(EXPANDED_NODES_KEY, openNodes);
        }
    }
}
