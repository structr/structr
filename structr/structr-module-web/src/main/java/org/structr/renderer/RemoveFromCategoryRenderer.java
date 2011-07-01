package org.structr.renderer;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;
import org.structr.common.CurrentRequest;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.entity.web.RemoveFromCategory;
import org.structr.core.entity.web.WebNode;

/**
 *
 * @author Christian Morgner
 */
public class RemoveFromCategoryRenderer implements NodeRenderer<RemoveFromCategory>
{
	private static final Logger logger = Logger.getLogger(RemoveFromCategoryRenderer.class.getName());

	protected final static String defaultCategoryParameterName = "category";

	@Override
	public void renderNode(StructrOutputStream out, RemoveFromCategory currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
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
		Boolean loggedIn = usernameFromSession != null;

		if(!loggedIn)
		{
			// Return silently when not logged in
			logger.log(Level.WARNING, "Not logged in");
			return;
		}

		Boolean sessionBlocked = (Boolean)session.getAttribute(WebNode.SESSION_BLOCKED);

		if(Boolean.TRUE.equals(sessionBlocked))
		{
			// Return silently when not user is blocked
			logger.log(Level.WARNING, "Session blocked");
			return;
		}

		// Get values from config page, or defaults
		String categoryParameterName = currentNode.getCategoryParameterName() != null ? currentNode.getCategoryParameterName() : defaultCategoryParameterName;

		String categoryName = request.getParameter(categoryParameterName);
		String objectId = request.getParameter("id");

		if(StringUtils.isEmpty(categoryName))
		{
			// Don't process form if no category name was given
			logger.log(Level.WARNING, "No category given");
			return;
		}

		if(StringUtils.isEmpty(objectId))
		{
			// Don't process form if no object id was given
			logger.log(Level.WARNING, "No object id given");
			return;
		}

		User user = CurrentRequest.getCurrentUser();
		if(user != null)
		{
			user.removeFromCategory(categoryName, objectId);
		}

		// TODO: Give some feedback?
	}

	@Override
	public String getContentType(RemoveFromCategory currentNode)
	{
		return ("text/html");
	}
}
