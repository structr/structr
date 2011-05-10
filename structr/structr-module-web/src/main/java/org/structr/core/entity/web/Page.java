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
package org.structr.core.entity.web;

import org.structr.core.entity.Template;
import java.util.*;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.DeleteRelationshipCommand;
import org.structr.core.node.FindNodeCommand;

/**
 * 
 * @author amorgner
 * 
 */
public class Page extends WebNode {

    private final static String ICON_SRC = "/images/page.png";
    
    public final static String TEMPLATE_KEY = "template";
    //public final static String TEMPLATES_KEY = "templates";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    /**
     * Render view
     * 
     * @param out
     * @param startNode
     * @param editUrl
     * @param editNodeId
     */
    @Override
    public void renderView(StringBuilder out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId, final User user) {

        // if this page is requested to be edited, render edit frame
        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

            // otherwise, render subnodes in edit mode
        } else {

            if (hasTemplate(user)) {

                // propagate request
//                template.setRequest(getRequest());

                template.setCallingNode(this);
                template.renderView(out, startNode, editUrl, editNodeId, user);
            } else {

                List<AbstractNode> subnodes = getSortedDirectChildAndLinkNodes(user);

                // render subnodes in correct order
                for (AbstractNode s : subnodes) {

                    // propagate request
//                    s.setRequest(getRequest());
                    s.renderView(out, startNode, editUrl, editNodeId, user);
                }
            }
        }
    }

    public Long getTemplate() {
        Template n = getTemplate(new SuperUser());
        return (n != null ? n.getId() : null);
    }

    public void setTemplate(final Long value) {

        // find template node
        Command findNode = Services.command(FindNodeCommand.class);
        Template templateNode = (Template) findNode.execute(new SuperUser(), value);

        // delete existing template relationships
        List<StructrRelationship> templateRels = this.getOutgoingRelationships(RelType.USE_TEMPLATE);
        Command delRel = Services.command(DeleteRelationshipCommand.class);
        if (templateRels != null) {
            for (StructrRelationship r : templateRels) {
                delRel.execute(r);
            }
        }

        // create new link target relationship
        Command createRel = Services.command(CreateRelationshipCommand.class);
        createRel.execute(this, templateNode, RelType.USE_TEMPLATE);
    }
//
//    public List<Template> getTemplateNodes() {
//        List<Template> result = new LinkedList<Template>();
//        List<StructrRelationship> rels = getRelationships(RelType.USE_TEMPLATE, Direction.OUTGOING);
//        for (StructrRelationship r : rels) {
//            AbstractNode n = r.getEndNode();
//            if (n instanceof Template) {
//                result.add((Template) n);
//            }
//        }
//        return result;
//    }
//
//    public List<String> getTemplate() {
//        List<String> ids = new LinkedList<String>();
//        List<Template> templateNodes = getTemplateNodes();
//        if (templateNodes != null && !(templateNodes.isEmpty())) {
//            for (Template p : templateNodes) {
//                ids.add(Long.toString(p.getId()));
//            }
//
//        }
//        return ids;
//    }
}
