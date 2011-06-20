package org.structr.core.renderer;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.app.HtmlNode;

/**
 *
 * @author Christian Morgner
 */
public class HtmlRenderer implements NodeRenderer<HtmlNode>
{
	// private attributes
	protected static final String LineSeparator = "\n";
	private Map<String, Set> attributes = null;
	protected boolean forceClosingTag = false;
	protected String tag = null;
	protected String id = null;

	public HtmlRenderer(String tag, String id)
	{
		this.tag = tag;
		this.id = id;
	}

	public HtmlRenderer(String tag)
	{
		this.attributes = new LinkedHashMap<String, Set>();

		this.tag = tag;
	}

	public HtmlRenderer()
	{
		this(null);
	}

	@Override
	public void renderNode(StructrOutputStream out, HtmlNode currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		currentNode.doBeforeRendering(this, out, startNode, editUrl, editNodeId);

		// notify component of rendering
		boolean hasContent = currentNode.hasContent(this, out, startNode, editUrl, editNodeId);

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
			currentNode.renderContent(this, out, startNode, editUrl, editNodeId);
		}

		if(tag != null && (forceClosingTag || hasContent))
		{
			out.append("</");
			out.append(tag);
			out.append(">");
		}
	}

	@Override
	public String getContentType(HtmlNode node)
	{
		return("text/html");
	}

	// ----- public methods -----
	public HtmlRenderer addCssClass(Object cssClass)
	{
		Set cssClasses = getAttributes("class");
		cssClasses.add(cssClass);

		return(this);
	}

	public HtmlRenderer addAttribute(String key, Object value)
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

	public HtmlRenderer setId(String id)
	{
		this.id = id;

		return(this);
	}

	public HtmlRenderer setTag(String tag)
	{
		this.tag = tag;

		return(this);
	}

	public HtmlRenderer forceClosingTag(boolean forceClosingTag)
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


}
