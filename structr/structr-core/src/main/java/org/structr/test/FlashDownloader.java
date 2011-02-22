/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.test;

import org.structr.common.RelType;
import org.structr.common.StandaloneTestHelper;
import org.structr.core.Services;
import org.structr.core.entity.Folder;
import org.structr.core.entity.StructrNode;
import org.structr.core.entity.SuperUser;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.FindUserCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.SaveImagesFromFlashUrl;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 *
 * @author axel
 */
public class FlashDownloader {

    public static void main(String[] args) {

        StandaloneTestHelper.prepareStandaloneTest("/opt/structr/structr-tfs2");

        final StructrNode adminNode = (StructrNode) Services.command(FindUserCommand.class).execute("admin");
//        final String urlString = "http://www.inxire.com/public/download/Webseiten/flash_inxire_buehne_de.swf";
        final String urlString = "http://www.incahacienda.com/flash/photo_home.swf";
        final String flashObjectName = urlString.substring(urlString.lastIndexOf("/") + 1);

        Services.command(TransactionCommand.class).execute(new StructrTransaction() {
            @Override
            public Object execute() throws Throwable {

                Folder parentFolder = (Folder) Services.command(CreateNodeCommand.class).execute(new SuperUser(),
                        new NodeAttribute(StructrNode.NAME_KEY, "_ " + flashObjectName + "_" + System.currentTimeMillis()),
                        new NodeAttribute(StructrNode.TYPE_KEY, Folder.class.getSimpleName()),
                        false);

                Services.command(CreateRelationshipCommand.class).execute(adminNode, parentFolder, RelType.HAS_CHILD);
                
                return Services.command(SaveImagesFromFlashUrl.class).execute(new SuperUser(), urlString, parentFolder);
            }
        });


    }
}
