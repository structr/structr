package org.structr.renderer;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.structr.common.CurrentRequest;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.context.SessionMonitor;
import org.structr.core.NodeRenderer;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.entity.web.LoginCheck;
import org.structr.core.entity.web.LoginForm;
import org.structr.core.entity.web.WebNode;
import org.structr.core.node.FindUserCommand;

/**
 *
 * @author Christian Morgner
 */
public class LoginCheckRenderer implements NodeRenderer<LoginCheck>
{
	private static final Logger logger = Logger.getLogger(LoginCheckRenderer.class.getName());
	private final static String NUMBER_OF_LOGIN_ATTEMPTS = "numberOfLoginAttempts";
	private final static String LOGIN_FAILURE_TEXT = "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";
	public final static String defaultSubmitButtonName = LoginForm.defaultSubmitButtonName;
	public final static String defaultAntiRobotFieldName = LoginForm.defaultAntiRobotFieldName;
	public final static String defaultUsernameFieldName = LoginForm.defaultUsernameFieldName;
	public final static String defaultPasswordFieldName = LoginForm.defaultPasswordFieldName;
	public final static int defaultMaxErrors = 1000;
	public final static int defaultDelayThreshold = 100;
	public final static int defaultDelayTime = 3;

	@Override
	public void renderNode(StructrOutputStream out, LoginCheck currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		String errorMsg;

		HttpServletRequest request = CurrentRequest.getRequest();

		if(request == null)
		{
			return;
		}

		HttpSession session = request.getSession();

		if(session == null)
		{
			return;
		}

		String usernameFromSession = (String)session.getAttribute(WebNode.Key.username.name());
//            String usernameFromSession = CurrentSession.getGlobalUsername();
		Boolean alreadyLoggedIn = usernameFromSession != null;

		if(alreadyLoggedIn)
		{
//                out.append("<div class=\"errorMsg\">").append("Your are logged in as ").append(usernameFromSession).append("</div>");
			return;
		}

		Boolean sessionBlocked = (Boolean)session.getAttribute(WebNode.Key.sessionBlocked.name());

		if(Boolean.TRUE.equals(sessionBlocked))
		{
			out.append("<div class=\"errorMsg\">").append("Too many login attempts, session is blocked for login").append("</div>");
			return;
		}

		// Get values from config page, or defaults
		String submitButtonName = currentNode.getSubmitButtonName() != null ? currentNode.getSubmitButtonName() : defaultSubmitButtonName;
		String antiRobotFieldName = currentNode.getAntiRobotFieldName() != null ? currentNode.getAntiRobotFieldName() : defaultAntiRobotFieldName;
		String usernameFieldName = currentNode.getUsernameFieldName() != null ? currentNode.getUsernameFieldName() : defaultUsernameFieldName;
		String passwordFieldName = currentNode.getPasswordFieldName() != null ? currentNode.getPasswordFieldName() : defaultPasswordFieldName;
		int maxRetries = currentNode.getMaxErrors() > 0 ? currentNode.getMaxErrors() : defaultMaxErrors;
		int delayThreshold = currentNode.getDelayThreshold() > 0 ? currentNode.getDelayThreshold() : defaultDelayThreshold;
		int delayTime = currentNode.getDelayTime() > 0 ? currentNode.getDelayTime() : defaultDelayTime;

		String username = request.getParameter(usernameFieldName);
		String password = request.getParameter(passwordFieldName);
		String submitButton = request.getParameter(submitButtonName);
		String antiRobot = request.getParameter(antiRobotFieldName);

		if(StringUtils.isEmpty(submitButton))
		{
			// Don't process form at all if submit button was not pressed
			return;
		}

		if(StringUtils.isNotEmpty(antiRobot))
		{
			// Don't process form if someone has filled the anti-robot field
			return;
		}

		if(StringUtils.isEmpty(username))
		{
			out.append("<div class=\"errorMsg\">").append("You must enter a username").append("</div>");
			countLoginFailure(out, session, maxRetries, delayThreshold, delayTime);
			return;
		}

		if(StringUtils.isEmpty(password))
		{
			out.append("<div class=\"errorMsg\">").append("You must enter a password").append("</div>");
			countLoginFailure(out, session, maxRetries, delayThreshold, delayTime);
			return;
		}

		// Session is not blocked, and we have a username and a password

		// First, check if we have a user with this name
		User loginUser = (User)Services.command(FindUserCommand.class).execute(username);

		// No matter what reason to deny login, always show the same error message to
		// avoid giving hints
		errorMsg = LOGIN_FAILURE_TEXT;

		if(loginUser == null)
		{
			logger.log(Level.INFO, "No user found for name {0}", loginUser);
			out.append("<div class=\"errorMsg\">").append(errorMsg).append("</div>");
			countLoginFailure(out, session, maxRetries, delayThreshold, delayTime);
			return;
		}

		// From here, we have a valid user

		if(loginUser.isBlocked())
		{
			logger.log(Level.INFO, "User {0} is blocked", loginUser);
			out.append("<div class=\"errorMsg\">").append(errorMsg).append("</div>");
			countLoginFailure(out, session, maxRetries, delayThreshold, delayTime);
			return;
		}

		// Check password

		String encryptedPasswordValue = DigestUtils.sha512Hex(password);

		if(!encryptedPasswordValue.equals(loginUser.getEncryptedPassword()))
		{
			logger.log(Level.INFO, "Wrong password for user {0}", loginUser);
			out.append("<div class=\"errorMsg\">").append(errorMsg).append("</div>");
			countLoginFailure(out, session, maxRetries, delayThreshold, delayTime);
			return;
		}

		// Username and password are both valid
		session.setAttribute(WebNode.Key.username.name(), loginUser.getName());
//            CurrentSession.setGlobalUsername(loginUser.getName());

		// Register user with internal session management
		long sessionId = SessionMonitor.registerUserSession(session);
		SessionMonitor.logActivity(sessionId, "Login");

		// Mark this session with the internal session id
		session.setAttribute(SessionMonitor.SESSION_ID, sessionId);

		// Clear all blocking stuff
		session.removeAttribute(WebNode.Key.sessionBlocked.name());
		session.removeAttribute(NUMBER_OF_LOGIN_ATTEMPTS);

		out.append("<div class=\"okMsg\">").append("Login successful. Welcome ").append(loginUser.getRealName()).append("!").append("</div>");
	}

	private void countLoginFailure(final StructrOutputStream out, final HttpSession session, final int maxRetries, final int delayThreshold, final int delayTime)
	{

		Integer retries = (Integer)session.getAttribute(NUMBER_OF_LOGIN_ATTEMPTS);

		if(retries != null && retries > maxRetries)
		{

			logger.log(Level.SEVERE, "More than {0} login failures, session {1} is blocked", new Object[]
				{
					maxRetries, session.getId()
				});
			session.setAttribute(WebNode.Key.sessionBlocked.name(), true);
			out.append("<div class=\"errorMsg\">").append("Too many unsuccessful login attempts, your session is blocked for login!").append("</div>");
			return;

		} else if(retries != null && retries > delayThreshold)
		{

			logger.log(Level.INFO, "More than {0} login failures, execution delayed by {1} seconds", new Object[]
				{
					delayThreshold, delayTime
				});

			try
			{
				Thread.sleep(delayTime * 1000);
			} catch(InterruptedException ex)
			{
				logger.log(Level.SEVERE, null, ex);
			}

			out.append("<div class=\"errorMsg\">").append("You had more than ").append(delayThreshold).append(" unsuccessful login attempts, execution was delayed by ").append(delayTime).append(" seconds.").append("</div>");

		} else if(retries != null)
		{

			session.setAttribute(NUMBER_OF_LOGIN_ATTEMPTS, retries + 1);

		} else
		{

			session.setAttribute(NUMBER_OF_LOGIN_ATTEMPTS, 1);

		}

	}

	@Override
	public String getContentType(LoginCheck currentNode)
	{
		return ("text/html");
	}
}
