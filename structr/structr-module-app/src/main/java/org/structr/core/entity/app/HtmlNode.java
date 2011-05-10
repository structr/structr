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

package org.structr.core.entity.app;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author Christian Morgner
 */
public abstract class HtmlNode extends AbstractNode
{
	private static final Logger logger = Logger.getLogger(HtmlNode.class.getName());
	private static final String ICON_SRC = "/images/tag.png";

	// private attributes
	protected static final String LineSeparator = "\n";
	private Map<String, Set> attributes = null;
	protected boolean forceClosingTag = false;
	protected String tag = null;
	protected String id = null;

	// ----- abstract methods -----
	public abstract void doBeforeRendering(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user);
	public abstract void renderContent(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user);
	public abstract boolean hasContent(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user);

	@Override
	public String getIconSrc()
	{
		return(ICON_SRC);
	}

	public HtmlNode(String tag, String id)
	{
		this.tag = tag;
		this.id = id;
	}

	public HtmlNode(String tag)
	{
		this.attributes = new LinkedHashMap<String, Set>();

		this.tag = tag;
	}

	public HtmlNode()
	{
		this(null);
	}

	// ----- public methods -----
	public HtmlNode addCssClass(Object cssClass)
	{
		Set cssClasses = getAttributes("class");
		cssClasses.add(cssClass);

		return(this);
	}

	public HtmlNode addAttribute(String key, Object value)
	{
		// for now: forbid inline styles
		if("style".equals(key))
		{
			throw new IllegalStateException("Inline style definition not allowed, use CSS instead!");
		}

		Set values = getAttributes(key);
		values.add(value);

		return(this);
	}

	@Override
	public void renderView(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user)
	{
		// notify component of rendering
		doBeforeRendering(out, startNode, editUrl, editNodeId, user);

		boolean hasContent = hasContent(out, startNode, editUrl, editNodeId, user);

		// only render if tag is set!
		if(tag != null)
		{
			out.append("<");
			out.append(tag);

			if(id != null)
			{
				out.append(" id=\"").append(id).append("\"");
			}

			for(Entry<String, Set> attribute : attributes.entrySet())
			{
				out.append(" ");
				out.append(attribute.getKey());
				out.append("=\"");

				for(Iterator it = attribute.getValue().iterator(); it.hasNext();)
				{
					Object attributeValue = it.next();

					out.append(attributeValue);
					if(it.hasNext()) out.append(" ");
				}

				out.append("\"");
			}

			if(!hasContent && !forceClosingTag)
			{
				out.append(" />");

			} else
			{
				out.append(">");
			}
		}

		if(hasContent)
		{
			renderContent(out, startNode, editUrl, editNodeId, user);
		}

		if(tag != null && (forceClosingTag || hasContent))
		{
			out.append("</");
			out.append(tag);
			out.append(">");
		}
	}

	public HtmlNode setId(String id)
	{
		this.id = id;

		return(this);
	}

	public HtmlNode forceClosingTag(boolean forceClosingTag)
	{
		this.forceClosingTag = forceClosingTag;

		return(this);
	}

	// <editor-fold defaultstate="collapsed" desc="private methods">
	private Set getAttributes(String key)
	{
		Set ret = attributes.get(key);

		if(ret == null)
		{
			ret = new LinkedHashSet();
			attributes.put(key, ret);
		}

		return(ret);
	}
	// </editor-fold>


    @Override
    public void onNodeCreation()
    {
    }

    @Override
    public void onNodeInstantiation()
    {
    }
}
