package org.structr.core.renderer;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.structr.common.AbstractNodeComparator;
import org.structr.common.CurrentRequest;
import org.structr.common.RelType;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.Filterable;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Template;

/**
 *
 * @author Christian Morgner
 */
public class NodeListRenderer extends NodeViewRenderer
{
	private static final Logger logger = Logger.getLogger(NodeListRenderer.class.getName());
	private static final String PAGE_NO_PARAMETER_NAME_KEY = "pageNoParameterName";
	private static final String PAGE_SIZE_PARAMETER_NAME_KEY = "pageSizeParameterName";
	private static final String SORT_KEY_PARAMETER_NAME_KEY = "sortKeyParameterName";
	private static final String SORT_ORDER_PARAMETER_NAME_KEY = "sortOrderParameterName";
	// defaults
	private String sortKeyParameterName = "sortKey";
	private String sortOrderParameterName = "sortOrder";
	private String pageNoParameterName = "pageNo";
	private String pageSizeParameterName = "pageSize";
	private String sortKey = "name";
	private String sortOrder = "";
	private int pageNo = 1;
	private int pageSize = 10;
	private int lastPage = -1;

	@Override
	public void renderNode(StructrOutputStream out, AbstractNode currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		if(currentNode.isVisible())
		{
			if(currentNode.hasTemplate())
			{
				Template template = currentNode.getTemplate();
				String html = template.getContent();

				if(StringUtils.isNotBlank(html))
				{
					init(currentNode);

					List<AbstractNode> nodesToRender = new LinkedList<AbstractNode>();

					// iterate over children following the DATA relationship and collect all nodes
					for(AbstractNode container : currentNode.getSortedDirectChildren(RelType.DATA))
					{
						Iterable<AbstractNode> iterable = container.getFilterSource(RelType.DATA, null);

						for(AbstractNode node : iterable) {
							nodesToRender.add(node);
						}
					}

					Collections.sort(nodesToRender, new AbstractNodeComparator(AbstractNode.toGetter(sortKey), sortOrder));

					int toIndex = Math.min(pageNo * pageSize, nodesToRender.size());
					int fromIndex = Math.min(Math.max(pageNo - 1, 0) * pageSize, toIndex);

					logger.log(Level.INFO, "Showing list elements from {0} to {1}", new Object[]
						{
							fromIndex, toIndex
						});

					// iterate over direct children of the given node
					for(AbstractNode n : nodesToRender.subList(fromIndex, toIndex))
					{

						doRendering(out, currentNode, n, editUrl, editNodeId);
					}

				} else
				{
					logger.log(Level.WARNING, "No template!");
				}
			}

		} else
		{
			logger.log(Level.WARNING, "Node not visible");
		}
	}

	@Override
	public String getContentType(AbstractNode node)
	{
		return ("text/html");
	}

	private void init(AbstractNode node)
	{

		sortKey = getStringParameterValue(node, SORT_KEY_PARAMETER_NAME_KEY, sortKeyParameterName, sortKey);
		sortOrder = getStringParameterValue(node, SORT_ORDER_PARAMETER_NAME_KEY, sortOrderParameterName, sortOrder);
		pageNo = getIntParameterValue(node, PAGE_NO_PARAMETER_NAME_KEY, pageNoParameterName, pageNo);
		if(pageNo < 1)
		{
			pageNo = 1;
		}
		if(pageSize < 1)
		{
			pageSize = 1;
		}

		pageSize = getIntParameterValue(node, PAGE_SIZE_PARAMETER_NAME_KEY, pageSizeParameterName, pageSize);

		lastPage = Math.abs(getSize(node) / pageSize);
		if(getSize(node) % pageSize > 0)
		{
			lastPage++;
		}
	}

	private String getStringParameterValue(final AbstractNode node, final String namePropertyKey, final String defaultParameterName, final String defaultValue)
	{
		String nameValue = defaultParameterName;
		String propertyValue = node.getStringProperty(namePropertyKey);
		if(StringUtils.isNotEmpty(propertyValue))
		{
			nameValue = propertyValue;
		}
		String value = defaultValue;
		if(StringUtils.isNotEmpty(nameValue))
		{
			String parameterValue = CurrentRequest.getRequest().getParameter(nameValue);
			if(StringUtils.isNotEmpty(parameterValue))
			{
				value = parameterValue;
			}
		}
		return value;
	}

	private int getIntParameterValue(final AbstractNode node, final String namePropertyKey, final String defaultParameterName, final int defaultValue)
	{
		String nameValue = defaultParameterName;
		String propertyValue = node.getStringProperty(namePropertyKey);
		if(StringUtils.isNotEmpty(propertyValue))
		{
			nameValue = propertyValue;
		}
		int value = defaultValue;
		if(StringUtils.isNotEmpty(nameValue))
		{
			String parameterValue = CurrentRequest.getRequest().getParameter(nameValue);
			if(StringUtils.isNotEmpty(parameterValue))
			{
				value = Integer.parseInt(parameterValue);
			}
		}
		return value;
	}

	public int getLastPageNo(AbstractNode node)
	{
		if(lastPage == -1)
		{
			init(node);
		}
		return lastPage;
	}

	private int getSize(final AbstractNode node)
	{
		int size = 0;

		// iterate over children following the DATA relationship
		for(AbstractNode container : node.getSortedDirectChildren(RelType.DATA))
		{
			List<AbstractNode> nodes = container.getDirectChildNodes();
			size += nodes.size();
		}
		return size;
	}

	private String getPager(AbstractNode node)
	{
		StringBuilder out = new StringBuilder();
		init(node);

		out.append("<ul>");

		if(pageNo > 1)
		{
			out.append("<li><a href=\"?pageSize=").append(pageSize).append("&pageNo=").append(pageNo - 1).append("&sortKey=").append(sortKey).append("&sortOrder=").append(sortOrder).append("\">").append(" &lt; previous (").append(pageNo - 1).append(")").append("</a></li>");
		}

		for(int i = 1; i <= lastPage; i++)
		{

			// if we have more than 10 pages, skip some pages
			if(lastPage > 10
				&& (i < pageNo - 5 || i > pageNo + 5)
				&& (i < lastPage - 5 && i > 5))
			{
				continue;
			}

			out.append("<li");
			if(i == pageNo)
			{
				out.append(" class=\"current\"");
			}
			out.append("><a href=\"?pageSize=").append(pageSize).append("&pageNo=").append(i).append("&sortKey=").append(sortKey).append("&sortOrder=").append(sortOrder).append("\">").append(i).append("</a></li>");

		}


		if(pageNo < lastPage)
		{
			out.append("<li><a href=\"?pageSize=").append(pageSize).append("&pageNo=").append(pageNo + 1).append("&sortKey=").append(sortKey).append("&sortOrder=").append(sortOrder).append("\">").append("next (").append(pageNo + 1).append(") &gt;").append("</a></li>");
		}

		out.append("</ul>");

		return out.toString();

	}
}
