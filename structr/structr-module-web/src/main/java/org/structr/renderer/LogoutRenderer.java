package org.structr.renderer;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.structr.common.RenderMode;
import org.structr.common.SecurityContext;
import org.structr.common.StructrOutputStream;
import org.structr.context.SessionMonitor;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.web.Logout;
import org.structr.core.entity.web.WebNode;

/**
 *
 * @author Christian Morgner
 */
public class LogoutRenderer implements NodeRenderer<Logout>
{
	private static final Logger logger = Logger.getLogger(LogoutRenderer.class.getName());

	@Override
	public void renderNode(StructrOutputStream out, Logout currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
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
			logout(out.getSecurityContext(), session);
			out.append("<div class=\"okMsg\">").append("User ").append(usernameFromSession).append(" logged out").append("</div>");
			logger.log(Level.INFO, "User {0} logged out", usernameFromSession);
			return;
		}
	}

	@Override
	public String getContentType(Logout currentNode)
	{
		return ("text/html");
	}

	private void logout(SecurityContext securityContext, HttpSession session)
	{
		securityContext.doLogout();

		/*
		String sessionId = session.getId();

		// Clear username in session
		CurrentSession.setGlobalUsername(null);
		session.setAttribute(WebNode.USERNAME_KEY, null);

		// Invalidate (destroy) session
		session.invalidate();

		SessionMonitor.logActivity(SessionMonitor.getSessionByUId(sessionId), "Logout");
		*/
	}
}
