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

import javax.servlet.http.HttpServletResponse;
import org.structr.core.entity.AbstractNode;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.click.util.Bindable;
import org.apache.click.Page;
import org.structr.common.StructrOutputStream;

/**
 * 
 * @author amorgner
 */
public class View extends StructrPage
{
	private static final Logger logger = Logger.getLogger(View.class.getName());
	@Bindable
	protected StringBuilder output = new StringBuilder();

	public View()
	{
		super();

		// create a context path for local links, like e.g.
		// "/structr-webapp/view.htm?nodeId="
		contextPath = getContext().getRequest().getContextPath().concat(
			getContext().getPagePath(View.class).concat("?").concat(
			NODE_ID_KEY.concat("=")));

	}

	/**
	 * @see Page#onSecurityCheck()
	 */
	@Override
	public boolean onSecurityCheck()
	{
//		userName = getContext().getRequest().getRemoteUser();
		userName = getUsernameFromSession();
		return true;
	}

	/**
	 * Render current node to output.
	 * This method calls the @see#renderNode() method of the node's implementing class
	 *
	 * @see Page#onRender()
	 */
	@Override
	public void onRender()
	{
		AbstractNode nodeToRender = getNodeByIdOrPath(getNodeId());
		if(nodeToRender == null)
		{
			logger.log(Level.FINE, "Node {0} not found", getNodeId());

			// TODO: change to structr page (make independent from Click framework)
			getContext().getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
			setForward("/not-found.htm");

		} else
		{
			// Check visibility before access rights to assure that the
			// existance of hidden objects is not exposed

			if(!(nodeToRender.isVisible()))
			{
				logger.log(Level.FINE, "Hidden page requested ({0})", getNodeId());

				// TODO: change to structr page (make independent from Click framework)
				getContext().getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
				setForward("/not-found.htm");
				return;
			}

			// check read access right
			if(!(isSuperUser || nodeToRender.readAllowed()))
			{
				logger.log(Level.FINE, "Secure page requested ({0})", getNodeId());

				// TODO: change to structr page (make independent from Click framework)
				getContext().getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
				setForward("/not-authorized.html");

				return;
			}

			String editUrl = null;
			if(editNodeId != null)
			{
				AbstractNode editNode = getNodeByIdOrPath(editNodeId);
				editUrl = getContext().getRequest().getContextPath().concat(getContext().getPagePath(getEditPageClass(editNode))).concat("?").concat(NODE_ID_KEY).concat("=").concat(editNodeId.toString()).concat("&").concat(RENDER_MODE_KEY).concat("=").concat(INLINE_MODE);
			}

			try
			{
				// create output stream wrapper
				StructrOutputStream outputStream = new StructrOutputStream(getContext().getResponse());
				nodeToRender.renderNode(outputStream, nodeToRender, editUrl, editNodeId);

				// commit response
				getContext().getResponse().getOutputStream().flush();
				
				
			} catch(Throwable t)
			{
				logger.log(Level.WARNING, "Exception while rendering to output stream: {0}", t);
			}


/*
			// some nodes should be rendered directly to servlet response
			// note: HtmlSource is instanceof PlainText!
			if(nodeToRender instanceof File || nodeToRender instanceof Image || (nodeToRender instanceof PlainText && !("text/html".equals(contentType))))
			{

				// use Apache Click direct rendering
				HttpServletResponse response = getContext().getResponse();

				// Set response headers
				//response.setHeader("Content-Disposition", "attachment; filename=\"" + s.getName() + "\"");
				response.setContentType(contentType);
				//response.setHeader("Pragma", "no-cache");

//                            String appMode = getContext().getApplicationMode();

				// in production mode, enable caching by setting some header values
				//if (ConfigService.MODE_PRODUCTION.equals(appMode)) {

				// add some caching directives to header
				// see http://weblogs.java.net/blog/2007/08/08/expires-http-header-magic-number-yslow

				Calendar cal = new GregorianCalendar();
				int seconds = 7 * 24 * 60 * 60 + 1; // 7 days + 1 sec
				cal.add(Calendar.SECOND, seconds);
				response.addHeader("Cache-Control",
					"public, max-age=" + seconds
					+ ", s-maxage=" + seconds);
				//+ ", must-revalidate, proxy-revalidate"

				DateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
				httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
				response.addHeader("Expires", httpDateFormat.format(cal.getTime()));

				Date lastModified = nodeToRender.getLastModifiedDate();
				if(lastModified != null)
				{
					response.addHeader("Last-Modified", httpDateFormat.format(lastModified));
				}
				//}

				try
				{
					// clean response's output
					response.getOutputStream().flush();
					nodeToRender.renderNode(out,  nodeToRender, editUrl, editNodeId);

				} catch(IOException e)
				{
					logger.log(Level.SEVERE, "Error while rendering to output stream: ", e.getStackTrace());

				}

			} else
			{

				StringBuilder out = new StringBuilder();
				nodeToRender.renderNode(out, nodeToRender, editUrl, editNodeId);

				// enable outbound url rewriting rules
				output = new StringBuilder(getContext().getResponse().encodeURL(out.toString()));


			}
		*/
		}
 	}
}
