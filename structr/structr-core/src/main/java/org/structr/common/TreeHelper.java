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
package org.structr.common;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author axel
 */
public abstract class TreeHelper {

    private static final Logger logger = Logger.getLogger(TreeHelper.class.getName());

    public static AbstractNode getNodeByPath(final AbstractNode startNode,
            final String path, final boolean includeLinks, final User user) {
        AbstractNode currentNode = startNode;
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
                logger.log(Level.FINE, "Node not found at {0}", path);
                return null;
            }

            List<AbstractNode> children = currentNode.getDirectChildNodes(user);
            if (includeLinks) {
                List<AbstractNode> links = currentNode.getLinkedNodes(user);
                if (links != null) {
                    children.addAll(links);
                }
            }

            for (AbstractNode child : children) {
                if (name.equals(child.getName())) {
                    currentNode = child;
                    foundName = true;
                    break;
                }
            }
            if (!foundName) {
                logger.log(Level.FINE, "Node not found at {0}", path);
                return null;
            }
        }
        return currentNode;
    }

    public static Iterable<AbstractNode> getNodesByPath(final AbstractNode startNode,
            final String path, final boolean includeLinks, final User user) {

        AbstractNode currentNode = startNode;
        String[] names = StringUtils.split(path, "/");

        List<AbstractNode> children = null;

        for (String name : names) {

            if ("..".equals(name)) {
                currentNode = currentNode.getParentNode(user);
                continue;
            }


            boolean foundName = false;
            children = currentNode.getDirectChildNodes(user);
            if (includeLinks) {
                List<AbstractNode> links = currentNode.getLinkedNodes(user);
                if (links != null) {
                    children.addAll(links);
                }
            }

            if ("*".equals(name)) {
                return children;
            }

            for (AbstractNode child : children) {
                if (name.equals(child.getName())) {
                    currentNode = child;
                    foundName = true;
                    break;
                }
            }
            if (!foundName) {
                logger.log(Level.FINE, "Node not found at {0}", path);
                return null;
            }
        }

        return null;
    }
}
