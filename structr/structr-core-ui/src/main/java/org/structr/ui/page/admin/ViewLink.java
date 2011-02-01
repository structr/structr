/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import org.structr.core.node.FindNodeCommand;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.node.LinkNodeFactoryCommand;
import org.structr.core.entity.Link;
import org.structr.core.entity.StructrNode;

/**
 * View page node.
 * 
 * @author amorgner
 */
public class ViewLink extends DefaultView {

//    @Bindable
//    protected ActionLink deleteRelationshipLink = new ActionLink("deleteRelationshipLink",
//            "Delete Relationship", this, "onDeleteRelationshipClick");

    @Override
    public void onRender() {

        super.onRender();

        Command findNode = Services.createCommand(FindNodeCommand.class);
        Command nodeFactory = Services.createCommand(LinkNodeFactoryCommand.class);

        node = (Link) nodeFactory.execute((StructrNode) findNode.execute(user, getNodeId()));

//        Column column = new Column("Action");
//        column.setTextAlign("center");
//        //AbstractLink[] links = new AbstractLink[]{deleteRelationshipLink};
//        //column.setDecorator(new LinkDecorator(relationshipsTable, links, RELATIONSHIP_ID_KEY));
//        column.setSortable(false);
//        relationshipsTable.addColumn(column);

        externalViewUrl = node.getNodeURL(user, contextPath);
        //localViewUrl = getContext().getResponse().encodeURL(viewLink.getHref());
        localViewUrl = getContext().getRequest().getContextPath().concat(
                "/view".concat(node.getNodePath(user)));

        StructrNode linkedNode = ((Link) node).getStructrNode();

        // render node's default view
        StringBuilder out = new StringBuilder();
        linkedNode.renderView(out, node, null, null, user);

        rendition = out.toString();

    }
//
//    /**
//     * Delete a relationship
//     *
//     * @return
//     */
//    public boolean onDeleteRelationshipClick() {
//        final String relId = deleteRelationshipLink.getValue();
//
//        Command transactionCommand = Services.createCommand(TransactionCommand.class);
//        transactionCommand.execute(new StructrTransaction() {
//
//            public Object execute() throws Throwable {
//                Command deleteRelationship = Services.createCommand(DeleteRelationshipCommand.class);
//
//                if (relId != null) {
//                    long id = Long.parseLong(relId);
//
//                    deleteRelationship.execute(id);
//                }
//
//                return (null);
//            }
//        });
//
//        okMsg = "Relationship to node " + relId + " has been deleted.";
//
//        //setRedirect(ViewLink.class);
//
//        return false;
//    }
}
