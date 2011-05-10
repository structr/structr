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
package org.structr.core.entity;

/**
 * 
 * @author amorgner
 * 
 */
public class Link extends AbstractNode {

    private final static String ICON_SRC = "/images/linked.png";

    @Override
    public String getIconSrc() {
        return iconSrc != null ? iconSrc : ICON_SRC;
    }
    private AbstractNode structrNode;
    private String iconSrc;

    /**
     * Special constructor for wrapper node
     *
     * @param node
     */
    public void init(AbstractNode node) {
        super.init(node.dbNode);
        setStructrNode(node);
    }

    /**
     * Get structr node
     *
     * @return
     */
    public AbstractNode getStructrNode() {
        return structrNode;
    }

    /**
     * Set structr node
     */
    public void setStructrNode(final AbstractNode structrNode) {
        this.structrNode = structrNode;
    }

    /**
     * Render edit view
     *
     * @param out
     * @param startNode
     * @param editUrl
     * @param editNodeId
     */
    @Override
    public void renderView(StringBuilder out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId, final User user) {

        AbstractNode node = getStructrNode();

        node.setTemplate(getTemplate(user));
//        node.setRequest(getRequest());

        // if this page is requested to be edited, render edit frame
        if (editNodeId != null && getId() == editNodeId.longValue()) {

            node.renderView(out, startNode, editUrl, editNodeId, user);

        } else {
            if (isVisible(user)) {
                getStructrNode().renderView(out, startNode, editUrl, editNodeId, user);
            }
        }
    }

    public void setIconSrc(String iconSrc) {
        this.iconSrc = iconSrc;
    }

    @Override
    public void onNodeCreation()
    {
    }

    @Override
    public void onNodeInstantiation()
    {
    }
}
