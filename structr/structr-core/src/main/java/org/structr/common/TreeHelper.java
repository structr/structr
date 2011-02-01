/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.common;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.structr.core.entity.StructrNode;
import org.structr.core.entity.User;

/**
 *
 * @author axel
 */
public abstract class TreeHelper {

    private static final Logger logger = Logger.getLogger(TreeHelper.class.getName());

    public static StructrNode getNodeByPath(final StructrNode startNode,
            final String path, final boolean includeLinks, final User user) {
        StructrNode currentNode = startNode;
        String[] names = StringUtils.split(path, "/");
        for (String name : names) {

            if ("..".equals(name)) {
                currentNode = currentNode.getParentNode(user);
                continue;
            }

            if ("*".equals(name)) {
            }

            boolean foundName = false;

            if (currentNode == null) {
                logger.log(Level.WARNING, "Node not found at {0}", path);
                return null;
            }

            List<StructrNode> children = currentNode.getDirectChildNodes(user);
            if (includeLinks) {
                List<StructrNode> links = currentNode.getLinkedNodes(user);
                if (links != null) {
                    children.addAll(links);
                }
            }

            for (StructrNode child : children) {
                if (name.equals(child.getName())) {
                    currentNode = child;
                    foundName = true;
                    break;
                }
            }
            if (!foundName) {
                logger.log(Level.WARNING, "Node not found at {0}", path);
                return null;
            }
        }
        return currentNode;
    }

    public static Iterable<StructrNode> getNodesByPath(final StructrNode startNode,
            final String path, final boolean includeLinks, final User user) {

        StructrNode currentNode = startNode;
        String[] names = StringUtils.split(path, "/");

        List<StructrNode> children = null;

        for (String name : names) {

            if ("..".equals(name)) {
                currentNode = currentNode.getParentNode(user);
                continue;
            }


            boolean foundName = false;
            children = currentNode.getDirectChildNodes(user);
            if (includeLinks) {
                List<StructrNode> links = currentNode.getLinkedNodes(user);
                if (links != null) {
                    children.addAll(links);
                }
            }

            if ("*".equals(name)) {
                return children;
            }

            for (StructrNode child : children) {
                if (name.equals(child.getName())) {
                    currentNode = child;
                    foundName = true;
                    break;
                }
            }
            if (!foundName) {
                logger.log(Level.WARNING, "Node not found at {0}", path);
                return null;
            }
        }

        return null;
    }
}
