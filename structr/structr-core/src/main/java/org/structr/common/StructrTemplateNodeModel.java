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

import freemarker.template.TemplateModelListSequence;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateSequenceModel;
import java.util.LinkedList;
import java.util.List;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author axel
 */
public class StructrTemplateNodeModel implements TemplateNodeModel {

    AbstractNode templateNode;
    AbstractNode startNode;
    String editUrl;
    Long editNodeId;
    User user;

    public StructrTemplateNodeModel() {
    }

    public StructrTemplateNodeModel(final AbstractNode templateNode) {
        setTemplateNode(templateNode);
    }

    public StructrTemplateNodeModel(final AbstractNode templateNode, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user) {
        this.templateNode = templateNode;
        this.startNode = startNode;
        this.editUrl = editUrl;
        this.editNodeId = editNodeId;
        this.user = user;
    }

    @Override
    public TemplateSequenceModel getChildNodes() {


        List<StructrTemplateNodeModel> childNodeList = new LinkedList<StructrTemplateNodeModel>();

        for (AbstractNode s : templateNode.getDirectChildNodes(user)) {

            StructrTemplateNodeModel m = new StructrTemplateNodeModel(s);
            childNodeList.add(m);


        }

        TemplateModelListSequence list = new TemplateModelListSequence(childNodeList);

        return list;

    }

    @Override
    public TemplateNodeModel getParentNode() {
        return new StructrTemplateNodeModel(templateNode.getParentNode(user));
    }

    @Override
    public String getNodeName() {
        return templateNode.getType();
    }

    @Override
    public String getNodeType() {
        return templateNode.getType();
    }

    @Override
    public String getNodeNamespace() {
        // return null
        // this sets "no namespace",
        // see http://freemarker.sourceforge.net/docs/ref_directive_visit.html#ref.directive.recurse
        return null;
    }

    public String getPosition() {
        return String.valueOf(templateNode.getPosition());
    }

    // private methods follow
    private void setTemplateNode(final AbstractNode node) {
        this.templateNode = node;
    }

    private void setStartNode(final AbstractNode startNode) {
        this.startNode = startNode;
    }

    private void setEditUrl(final String editUrl) {
        this.editUrl = editUrl;
    }

    private void setEditNodeId(final Long editNodeId) {
        this.editNodeId = editNodeId;
    }
}
