/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.common;

import freemarker.template.TemplateModelListSequence;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateSequenceModel;
import java.util.ArrayList;
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


        List<StructrTemplateNodeModel> childNodeList = new ArrayList<StructrTemplateNodeModel>();

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
