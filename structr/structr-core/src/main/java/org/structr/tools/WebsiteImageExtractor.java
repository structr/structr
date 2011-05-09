/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
