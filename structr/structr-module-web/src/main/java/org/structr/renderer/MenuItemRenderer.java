package org.structr.renderer;

import java.util.LinkedList;
import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.common.StructrOutputStream;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Link;
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
	 * @param currentMenuNode
	 */
	protected void renderMenuItems(final StructrOutputStream out, final AbstractNode currentNode, final AbstractNode startNode, final AbstractNode currentMenuNode, int currentDepth, int currentPos, int numberOfSubnodes, int maxDepth)
	{
		SecurityContext securityContext = out.getSecurityContext();
		AbstractNode menuItemNode = currentMenuNode;

		if(currentDepth > maxDepth)
		{
			return;
		}

		currentDepth++;

		List<AbstractNode> menuItems = new LinkedList<AbstractNode>();
		List<AbstractNode> allMenuItems = menuItemNode.getSortedMenuItems();

		for(AbstractNode n : allMenuItems)
		{
			if(securityContext.isVisible(n)) {

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
		if(menuItemNode != currentNode)
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

			if(menuItemNode.equals(startNode))
			{
				cssClass += " current";
			}

			if(securityContext.isVisible(menuItemNode)) {

				AbstractNode contextNode;

				if (startNode instanceof Link) {
					contextNode = ((Link) startNode).getStructrNode().getContextNode();
				} else {
					contextNode = startNode.getContextNode();
				}


				String relativeNodeURL = menuItemNode.getNodeURL(contextNode).replace("&", "%26");

				if(!(cssClass.isEmpty()))
				{
					cssClass = " class=\"" + cssClass + "\"";
				}

				out.append("<li").append(cssClass).append(">");
				out.append("<span><a href=\"").append(relativeNodeURL).append("\">");
				out.append(currentMenuNode.getTitleOrName());
				out.append("</a></span>");
			}
		}

		if(securityContext.isVisible(currentMenuNode)) {

			int sub = menuItems.size();
			int pos = 0;
			for(AbstractNode s : menuItems)
			{
				renderMenuItems(out, currentNode, startNode, s, currentDepth, pos, sub, maxDepth);
				pos++;
			}

			if(currentMenuNode != currentNode)
			{

				out.append("</li>");

				if(currentPos == numberOfSubnodes - 1)
				{
					cssClass = " last";
				}
			}

		}

		if(currentMenuNode != currentNode)
		{

			if(currentPos == numberOfSubnodes - 1)
			{
				out.append("</ul>");

			}
		}
	}
}
