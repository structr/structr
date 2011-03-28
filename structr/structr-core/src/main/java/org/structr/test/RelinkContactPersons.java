/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.test;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.StandaloneTestHelper;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.cloud.PushNodes;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.DeleteNodeCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.FindUserCommand;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchNodeCommand;

/**
 *
 * @author axel
 */
public class RelinkContactPersons {

    public static void main(String[] args) {

        StandaloneTestHelper.prepareStandaloneTest("/opt/structr/structr-tfs2");

        final AbstractNode adminNode = (AbstractNode) Services.command(FindUserCommand.class).execute("admin");

        final String remoteServer = "localhost";
        //Services.command(PushNodes.class).execute(adminNode, remoteServer);

        List<AbstractNode> searchResult = (List<AbstractNode>) Services.command(SearchNodeCommand.class).execute(null, null, false, false, Search.andExactType("ContactPerson"));

        System.out.println("Found " + searchResult.size() + " contact persons");

        int i = 0;

        Command pushNodes = Services.command(PushNodes.class);
        Command findNode = Services.command(FindNodeCommand.class);

        String remoteHostValue = "true5stars.com";
        Integer tcpPort = 54555;
        Integer udpPort = 57555;
        boolean rec = false;
        
        Command createRel = Services.command(CreateRelationshipCommand.class);
        
        for (AbstractNode node : searchResult) {

            Object hotelGroupId = node.getProperty("tmpHotelGroupId");

            if (hotelGroupId != null) {

//                System.out.println("Connect this node to " + hotelGroupId);
//                createRel.execute(node, findNode.execute(null, hotelGroupId), RelType.LINK);
//                createRel.execute(findNode.execute(null, 712), node, RelType.HAS_CHILD);
                Services.command(DeleteNodeCommand.class).execute(hotelGroupId, findNode.execute(null, 0), true, new SuperUser());
                System.out.println("Node " + hotelGroupId + " deleted");

            }

            List<StructrRelationship> relsIn = node.getIncomingChildRelationships();
            if (relsIn == null || relsIn.isEmpty()) {
                System.out.println("Found node without incoming CHILD rel: " + node.getName() + ", id: " + node.getId());
                i++;



                List<StructrRelationship> rels = node.getOutgoingRelationships();
                for (StructrRelationship r : rels) {
                    AbstractNode s = r.getStartNode();
                    AbstractNode e = r.getEndNode();
                    System.out.println("Found outgoing relationship: " + s.getName() + ", id: " + s.getId() + " ------------> " + e.getName() + ", id: " + e.getId());

                    node.setProperty("tmpHotelGroupId", e.getId());
                    node.setProperty("tmpHotelGroupName", e.getName());

                    pushNodes.execute(new SuperUser(), node, remoteHostValue, tcpPort, udpPort, rec);

                }

            }

        }


        System.out.println(i + " contact persons without CHILD relationship");

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            Logger.getLogger(RelinkContactPersons.class.getName()).log(Level.SEVERE, null, ex);
        }

        StandaloneTestHelper.finishStandaloneTest();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            Logger.getLogger(RelinkContactPersons.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
