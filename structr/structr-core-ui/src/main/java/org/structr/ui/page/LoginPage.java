/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page;

import java.util.ArrayList;
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
    private final static String SUPERADMIN_PASSWORD_KEY = "sehrgeheim";
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
        userName = (String) getContext().getRequest().getSession().getAttribute(USERNAME_KEY);
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

            // TODO: remove superadmin login!!
            if (SUPERUSER_KEY.equals(userValue) && SUPERADMIN_PASSWORD_KEY.equals(passwordValue)) {

                logger.log(Level.INFO, "############# Logged in as superadmin! ############");
                userName = SUPERUSER_KEY;
                isSuperUser = true;

                user = new SuperUser();
                getContext().getRequest().getSession().setAttribute(USERNAME_KEY, userValue);

                Services.initialize();

                // redirect superuser to maintenance
                setRedirect("/admin/maintenance.htm");

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

                if (!encryptedPasswordValue.equals(user.getProperty(PASSWORD_KEY))) {
                    logger.log(Level.INFO, "Wrong password for user {0}", user);
                    errorMsg = "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";
                    return true;
                }

                // username and password are both valid
                userName = userValue;
                getContext().getRequest().getSession().setAttribute(USERNAME_KEY, userValue);

                initFirstPage();

            }
            
            // Register user with internal session management
            sessionId = SessionMonitor.registerUser(user, getContext().getSession());
            SessionMonitor.logActivity(user, sessionId, "Login");

            // Mark this session with the internal session id
            getContext().getRequest().getSession().setAttribute(SessionMonitor.SESSION_ID, sessionId);

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

            openNodes = new ArrayList<TreeNode>();

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
