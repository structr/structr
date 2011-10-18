package org.structr.renderer;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.structr.common.RenderMode;
import org.structr.common.SecurityContext;
import org.structr.common.StructrOutputStream;
import org.structr.context.SessionMonitor;
import org.structr.core.NodeRenderer;
import org.structr.core.Services;
import org.structr.core.auth.AuthenticationException;
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
		SecurityContext securityContext = out.getSecurityContext();
		HttpServletRequest request = out.getRequest();

		if(request == null)
		{
			return;
		}

		HttpSession session = request.getSession();

		if(session == null)
		{
			return;
		}

		String usernameFromSession = (String)session.getAttribute(WebNode.USERNAME_KEY);
//            String usernameFromSession = CurrentSession.getGlobalUsername();
		Boolean alreadyLoggedIn = usernameFromSession != null;

		if(alreadyLoggedIn)
		{
//                out.append("<div class=\"errorMsg\">").append("Your are logged in as ").append(usernameFromSession).append("</div>");
			return;
		}

		Boolean sessionBlocked = (Boolean)session.getAttribute(WebNode.SESSION_BLOCKED);

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

		try {
			securityContext.doLogin(username, password);

		} catch(AuthenticationException aex) {

			out.append("<div class=\"errorMsg\">").append(aex.getMessage()).append("</div>");
			countLoginFailure(out, session, maxRetries, delayThreshold, delayTime);
			return;

		}

		// Clear all blocking stuff
		session.removeAttribute(WebNode.SESSION_BLOCKED);
		session.removeAttribute(NUMBER_OF_LOGIN_ATTEMPTS);

		out.append("<div class=\"okMsg\">").append("Login successful. Welcome ").append(securityContext.getUser().getRealName()).append("!").append("</div>");
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
			session.setAttribute(WebNode.SESSION_BLOCKED, true);
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
