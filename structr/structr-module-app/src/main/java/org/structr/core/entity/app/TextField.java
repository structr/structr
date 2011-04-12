/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author Christian Morgner
 */
public class TextField extends HtmlNode
{
	private static final String ICON_SRC =	"/images/textfield.png";

	public TextField(String id, String name, Object value)
	{
		super("input", id);

		addAttribute("type", "text");
		addAttribute("value", value);
	}

	@Override
	public String getIconSrc()
	{
		return(ICON_SRC);
	}

	@Override
	public void doBeforeRendering(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		// noop, parse value from request here?
	}

	@Override
	public void renderContent(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		// noop
	}

	@Override
	public boolean hasContent(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		return(false);
	}
}
