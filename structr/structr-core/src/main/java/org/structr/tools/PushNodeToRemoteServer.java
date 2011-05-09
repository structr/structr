/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.tools;

import org.structr.common.StandaloneTestHelper;
import org.structr.core.Services;
import org.structr.core.cloud.PushNodes;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.FindUserCommand;

/**
 *
 * @author axel
 */
public class PushNodeToRemoteServer {

    public static void main(String[] args) {

        StandaloneTestHelper.prepareStandaloneTest("/opt/structr/structr-tfs2");

        final AbstractNode adminNode = (AbstractNode) Services.command(FindUserCommand.class).execute("admin");
        final String remoteServer = "localhost";


        Services.command(PushNodes.class).execute(adminNode, remoteServer);

        StandaloneTestHelper.finishStandaloneTest();

    }
}
