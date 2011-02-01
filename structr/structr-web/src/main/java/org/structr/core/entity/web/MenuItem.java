package org.structr.core.entity.web;

//import java.util.Collections;
//import java.util.Comparator;
import java.util.List;
import org.neo4j.graphdb.Direction;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.StructrNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.DeleteRelationshipCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.NodeFactoryCommand;

/**
 * 
 * @author amorgner
 * 
 */
public class MenuItem extends WebNode {

    private final static String ICON_SRC = "/images/page_link.png";
    public final static String LINK_TARGET_KEY = "linkTarget";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }
//
//    public String getLinkTarget() {
//        return (String) getProperty(LINK_TARGET_KEY);
//    }
//
//    public void setLinkTarget(final String linkTarget) {
//        setProperty(LINK_TARGET_KEY, linkTarget);
//    }

    public Page getLinkedPage() {
        if (hasRelationship(RelType.PAGE_LINK, Direction.OUTGOING)) {
            Command nodeFactory = Services.createCommand(NodeFactoryCommand.class);
            return ((Page) nodeFactory.execute(getRelationships(RelType.PAGE_LINK, Direction.OUTGOING).get(0).getEndNode()));
        } else {
            return null;
        }
    }

    public Long getLinkTarget() {
        Page n = getLinkedPage();
        return (n != null ? n.getId() : null);
    }

    public void setLinkTarget(final Long value) {

        // find link target node
        Command findNode = Services.createCommand(FindNodeCommand.class);
        StructrNode linkTargetNode = (StructrNode) findNode.execute(new SuperUser(), value);

        // delete existing link target relationships
        List<StructrRelationship> pageLinkRels = getRelationships(RelType.PAGE_LINK, Direction.OUTGOING);
        Command delRel = Services.createCommand(DeleteRelationshipCommand.class);
        if (pageLinkRels != null) {
            for (StructrRelationship r : pageLinkRels) {
                delRel.execute(r);
            }
        }

        // create new link target relationship
        Command createRel = Services.createCommand(CreateRelationshipCommand.class);
        createRel.execute(this, linkTargetNode, RelType.PAGE_LINK);
    }

    /**
     * Render a menu item recursively. Stop at the given maximum depth
     *
     * @param out
     * @param startNode
     */
    protected void renderMenuItems(StringBuilder out, final StructrNode startNode, final StructrNode currentNode, int currentDepth, int currentPos, int numberOfSubnodes, int maxDepth, final User user) {

        StructrNode menuItemNode = currentNode;

        if (currentDepth > maxDepth) {
            return;
        }

        currentDepth++;

        List<StructrNode> menuItems = menuItemNode.getSortedMenuItems(user);

        // sort by position (not needed - they are already sorted)
//        Collections.sort(menuItems, new Comparator<StructrNode>() {
//
//            @Override
//            public int compare(StructrNode nodeOne, StructrNode nodeTwo) {
//                return nodeOne.getPosition().compareTo(nodeTwo.getPosition());
//            }
//        });

        String cssClass = "";

        // don't render menu node itself
        if (menuItemNode != this) {

            if (currentPos == 0) {
                cssClass = " first";
                out.append("<ul>");

            }

            if (currentPos == numberOfSubnodes - 1) {
                cssClass = " last";
            }

            if (menuItemNode instanceof MenuItem) {

                Page linkedPage = ((MenuItem) menuItemNode).getLinkedPage();

                // check if this node has a PAGE_LINK relationship
                // if yes, use linked page node instead of menu item node itself
                if (linkedPage != null) {
                    menuItemNode = linkedPage;
                }
            }

            if (menuItemNode.equals(startNode)) {
                cssClass += " current";
            }


            if (menuItemNode.isVisible()) {

                String relativeNodePath = menuItemNode.getNodePath(user, startNode).replace("&", "%26");

                if (!(cssClass.isEmpty())) {
                    cssClass = " class=\"" + cssClass + "\"";
                }

                out.append("<li").append(cssClass).append(">");
                out.append("<span>" + "<a href=\"").append(relativeNodePath).append("\">");
                out.append(currentNode.getName());
            }
        }

        if (currentNode.isVisible()) {

            int sub = menuItems.size();
            int pos = 0;
            for (StructrNode s : menuItems) {
                renderMenuItems(out, startNode, s, currentDepth, pos, sub, maxDepth, user);
                pos++;
            }

            if (currentNode != this) {
                out.append("</a>").append("</span>\n");
                out.append("</li>");

                if (currentPos == numberOfSubnodes - 1) {
                    cssClass = " last";
                }
            }

        }

        if (currentNode != this) {

            if (currentPos == numberOfSubnodes - 1) {
                out.append("</ul>");

            }
        }

    }
}
