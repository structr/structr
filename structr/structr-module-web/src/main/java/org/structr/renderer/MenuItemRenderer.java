package org.structr.renderer;

import java.util.LinkedList;
import java.util.List;
import org.structr.common.StructrOutputStream;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.web.MenuItem;
import org.structr.core.entity.web.Page;

/**
 *
 * @author Christian Morgner
 */
public abstract class MenuItemRenderer
{
	/**
	 * Render a menu item recursively. Stop at the given maximum depth
	 *
	 * @param out
	 * @param currentNode
	 */
	protected void renderMenuItems(final StructrOutputStream out, final AbstractNode currentNode, final AbstractNode startNode, int currentDepth, int currentPos, int numberOfSubnodes, int maxDepth)
	{

		AbstractNode menuItemNode = startNode;

		if(currentDepth > maxDepth)
		{
			return;
		}

		currentDepth++;

		List<AbstractNode> menuItems = new LinkedList<AbstractNode>();
		List<AbstractNode> allMenuItems = menuItemNode.getSortedMenuItems();

		for(AbstractNode n : allMenuItems)
		{
			if(n.isVisible())
			{
				menuItems.add(n);
			}
		}

		// sort by position (not needed - they are already sorted)
//        Collections.sort(menuItems, new Comparator<AbstractNode>() {
//
//            @Override
//            public int compare(AbstractNode nodeOne, AbstractNode nodeTwo) {
//                return nodeOne.getPosition().compareTo(nodeTwo.getPosition());
//            }
//        });

		String cssClass = "";

		// don't render menu node itself
		if(menuItemNode != startNode)
		{

			if(currentPos == 0)
			{
				cssClass = " first";
				out.append("<ul>");

			}

			if(currentPos == numberOfSubnodes - 1)
			{
				cssClass = " last";
			}

			if(menuItemNode instanceof MenuItem)
			{

				Page linkedPage = ((MenuItem)menuItemNode).getLinkedPage();

				// check if this node has a PAGE_LINK relationship
				// if yes, use linked page node instead of menu item node itself
				if(linkedPage != null)
				{
					menuItemNode = linkedPage;
				}
			}

			if(menuItemNode.equals(currentNode))
			{
				cssClass += " current";
			}


			if(menuItemNode.isVisible())
			{

				String relativeNodePath = menuItemNode.getNodePath(currentNode).replace("&", "%26");

				if(!(cssClass.isEmpty()))
				{
					cssClass = " class=\"" + cssClass + "\"";
				}

				out.append("<li").append(cssClass).append(">");
				out.append("<span>" + "<a href=\"").append(relativeNodePath).append("\">");
				out.append(startNode.getTitleOrName());
			}
		}

		if(startNode.isVisible())
		{

			int sub = menuItems.size();
			int pos = 0;
			for(AbstractNode s : menuItems)
			{
				renderMenuItems(out, currentNode, s, currentDepth, pos, sub, maxDepth);
				pos++;
			}

			if(startNode != currentNode)
			{
				out.append("</a>").append("</span>\n");
				out.append("</li>");

				if(currentPos == numberOfSubnodes - 1)
				{
					cssClass = " last";
				}
			}

		}

		if(startNode != currentNode)
		{

			if(currentPos == numberOfSubnodes - 1)
			{
				out.append("</ul>");

			}
		}

	}
}
