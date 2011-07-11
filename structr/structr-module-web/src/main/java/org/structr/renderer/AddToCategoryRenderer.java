package org.structr.renderer;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;
import org.structr.common.CurrentRequest;
import org.structr.common.CurrentSession;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.entity.web.AddToCategory;
import org.structr.core.entity.web.WebNode;

/**
 *
 * @author Christian Morgner
 */
public class AddToCategoryRenderer implements NodeRenderer<AddToCategory>
{
	private static final Logger logger = Logger.getLogger(AddToCategoryRenderer.class.getName());
	private final static String defaultCategoryParameterName = "category";

	@Override
	public void renderNode(StructrOutputStream out, AddToCategory currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
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

		String[] ids = StringUtils.split(objectId, ",");

		for(String id : ids)
		{

			User user = CurrentSession.getUser();
			AbstractNode addedObject = user.addToCategory(categoryName, id);

			if(addedObject != null)
			{
				out.append("<div class=\"okMsg\">").append(addedObject.getName()).append(" successfully added to ").append(categoryName).append("</div>");
			} else
			{
				out.append("<div class=\"errorMsg\">").append("Could not add to category ").append(categoryName).append("</div>");
			}
		}
	}

	@Override
	public String getContentType(AddToCategory currentNode)
	{
		return ("text/html");
	}
}
