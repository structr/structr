/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import javax.servlet.http.HttpServletRequest;
import org.structr.common.StructrContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author Christian Morgner
 */
public class TextField extends HtmlNode implements InteractiveNode
{
	private static final String TEXTFIELD_ICON_SRC =	"/images/textfield.png";
	private String mappedName = null;

	public TextField()
	{
		super("input");
	}

	@Override
	public String getIconSrc()
	{
		return(TEXTFIELD_ICON_SRC);
	}

	@Override
	public void doBeforeRendering(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		String name = getName();
		Object value = getValue();

		// add attributes here, do not access in constructor
		addAttribute("type", "text");
		addAttribute("name", name);
		addAttribute("value", value != null ? value : "");
	}

	@Override
	public void renderContent(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		// noop
	}

	@Override
	public boolean hasContent(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		return(true);
	}

	// ----- interface InteractiveNode -----
	@Override
	public String getValue()
	{
		HttpServletRequest request = StructrContext.getRequest();
		String name = getName();
		String ret = null;

		if(request != null)
		{
			ret = request.getParameter(name);
		}

		return(ret);
	}

	@Override
	public Class getParameterType()
	{
		return(String.class);
	}

	@Override
	public void setMappedName(String mappedName)
	{
		this.mappedName = mappedName;
	}

	@Override
	public String getMappedName()
	{
		if(this.mappedName != null)
		{
			return(mappedName);
		}

		return(getName());
	}
}
