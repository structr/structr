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

import org.structr.common.RelType;
import org.structr.common.StandaloneTestHelper;
import org.structr.core.Services;
import org.structr.core.entity.Folder;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SuperUser;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.FindUserCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.SaveImagesFromWebsiteUrl;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 *
 * @author axel
 */
public class WebsiteImageExtractor {

    public static void main(String[] args) {

        StandaloneTestHelper.prepareStandaloneTest("/opt/structr/structr-tfs2");

        final AbstractNode adminNode = (AbstractNode) Services.command(FindUserCommand.class).execute("admin");
        final String urlString = "http://www.inxire.com/public/browse/Webseiten";
        final String websiteName = urlString.substring(urlString.lastIndexOf("http://") + 7);

        Services.command(TransactionCommand.class).execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {

                Folder parentFolder = (Folder) Services.command(CreateNodeCommand.class).execute(new SuperUser(),
                        new NodeAttribute(AbstractNode.NAME_KEY, "_" + websiteName + "_" + System.currentTimeMillis()),
                        new NodeAttribute(AbstractNode.TYPE_KEY, Folder.class.getSimpleName()),
                        false);

                Services.command(CreateRelationshipCommand.class).execute(adminNode, parentFolder, RelType.HAS_CHILD);

                return Services.command(SaveImagesFromWebsiteUrl.class).execute(new SuperUser(), urlString, parentFolder);
            }
        });

        StandaloneTestHelper.finishStandaloneTest();

    }
}
