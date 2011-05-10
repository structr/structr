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
