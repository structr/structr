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
package org.structr.tools;

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
import org.structr.core.node.FindUserCommand;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchNodeCommand;

/**
 *
 * @author axel
 */
public class PushMissingNodesToRemoteServer {

    public static void main(String[] args) {

        StandaloneTestHelper.prepareStandaloneTest("/opt/structr/structr-tfs2");

        final AbstractNode adminNode = (AbstractNode) Services.command(FindUserCommand.class).execute("admin");

        final String remoteServer = "localhost";
        //Services.command(PushNodes.class).execute(adminNode, remoteServer);

        List<AbstractNode> searchResult = (List<AbstractNode>) Services.command(SearchNodeCommand.class).execute(null, null, false, false, Search.andExactType("ContactPerson"));

        System.out.println("Found " + searchResult.size() + " contact persons");

        int i = 0;

        Command pushNodes = Services.command(PushNodes.class);

        String remoteHostValue = "true5stars.com";
        Integer tcpPort = 54555;
        Integer udpPort = 57555;
        boolean rec = false;



        for (AbstractNode node : searchResult) {

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
            Logger.getLogger(PushMissingNodesToRemoteServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        StandaloneTestHelper.finishStandaloneTest();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            Logger.getLogger(PushMissingNodesToRemoteServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
