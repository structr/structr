package org.structr.renderer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;
import org.structr.common.CurrentRequest;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.web.LoginForm;
import org.structr.core.entity.web.RegistrationForm;
import org.structr.core.entity.web.WebNode;

/**
 *
 * @author Christian Morgner
 */
public class LoginFormRenderer extends FormRenderer implements NodeRenderer<LoginForm>
{
	@Override
	public void renderNode(StructrOutputStream out, LoginForm currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
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

		String usernameFromSession = (String)session.getAttribute(WebNode.USERNAME_KEY);
//            String usernameFromSession = CurrentSession.getGlobalUsername();
		Boolean alreadyLoggedIn = usernameFromSession != null;

		if(alreadyLoggedIn)
		{
			out.append("<div class=\"okMsg\">").append("Your are logged in as ").append(usernameFromSession).append("</div>");
			return;
		}

		Boolean sessionBlocked = (Boolean)session.getAttribute(WebNode.SESSION_BLOCKED);

		if(Boolean.TRUE.equals(sessionBlocked))
		{
			out.append("<div class=\"errorMsg\">").append("Too many login attempts, session is blocked for login").append("</div>");
			return;
		}

		// Get values from config page, or defaults
		String action = currentNode.getAction() != null ? currentNode.getAction() : FormRenderer.defaultAction;
		String submitButtonName = currentNode.getSubmitButtonName() != null ? currentNode.getSubmitButtonName() : LoginForm.defaultSubmitButtonName;
		String antiRobotFieldName = currentNode.getAntiRobotFieldName() != null ? currentNode.getAntiRobotFieldName() : LoginForm.defaultAntiRobotFieldName;
		String usernameFieldName = currentNode.getUsernameFieldName() != null ? currentNode.getUsernameFieldName() : LoginForm.defaultUsernameFieldName;
		String passwordFieldName = currentNode.getPasswordFieldName() != null ? currentNode.getPasswordFieldName() : LoginForm.defaultPasswordFieldName;
		String cssClass = currentNode.getCssClass() != null ? currentNode.getCssClass() : FormRenderer.defaultCssClass;
		String label = currentNode.getLabel() != null ? currentNode.getLabel() : FormRenderer.defaultLabel;

		String username = StringUtils.trimToEmpty(param(usernameFieldName));
		String password = StringUtils.trimToEmpty(param(passwordFieldName));

		out.append("<form name=\"").append(currentNode.getName()).append("\" action=\"").append(action).append("\" method=\"post\">");
		out.append("<input type=\"hidden\" name=\"").append(antiRobotFieldName).append("\" value=\"\">");
		out.append("<table class=\"").append(cssClass).append("\">");
		//out.append("<tr><th><span class=\"heading\">").append(label).append("</span></th><th></th></tr>");
		out.append("<tr><td class=\"label\">Username</td></tr>");
		out.append("<tr><td class=\"field\"><input type=\"text\" name=\"").append(usernameFieldName).append("\" value=\"").append(username).append("\" size=\"30\"></td></tr>");
		out.append("<tr><td class=\"label\">Password</td></tr>");
		out.append("<tr><td class=\"field\"><input type=\"password\" name=\"").append(passwordFieldName).append("\" value=\"").append(password).append("\" size=\"30\"></td></tr>");
		out.append("<tr><td class=\"button\"><input type=\"submit\" name=\"").append(submitButtonName).append("\" value=\"Submit\"></td></tr>");
		out.append("</table>");
		out.append("</form>");
	}

	@Override
	public String getContentType(LoginForm currentNode)
	{
		return ("text/html");
	}
}
