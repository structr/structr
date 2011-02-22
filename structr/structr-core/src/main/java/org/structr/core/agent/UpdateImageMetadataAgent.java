/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.agent;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.Image;
import org.structr.core.entity.StructrNode;
import org.structr.core.node.ExtractAndSetImageDimensionsAndFormat;
import org.structr.core.node.GetAllNodes;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 *
 * @author amorgner
 */
public class UpdateImageMetadataAgent extends Agent {

    private static final Logger logger = Logger.getLogger(UpdateImageMetadataAgent.class.getName());

    public UpdateImageMetadataAgent() {
        setName("UpdateImageMetadataAgent");
    }

    @Override
    public Class getSupportedTaskType() {
        return (UpdateImageMetadataTask.class);
    }

    @Override
    public ReturnValue processTask(Task task) {

        if (task instanceof UpdateImageMetadataTask) {

            long t0 = System.currentTimeMillis();
            logger.log(Level.INFO, "Starting update metadata of all images ...");

            long nodes = updateImageMetadata();

            long t1 = System.currentTimeMillis();
            logger.log(Level.INFO, "Update image metadata finished, {0} nodes processed in {1} s", new Object[]{nodes, (t1 - t0) / 1000});

        }

        return (ReturnValue.Success);
    }

    private long updateImageMetadata() {

        Command transactionCommand = Services.command(TransactionCommand.class);
        Long numberOfImages = (Long) transactionCommand.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {

                List<Image> images = new LinkedList<Image>();

                List<StructrNode> allNodes = (List<StructrNode>) Services.command(GetAllNodes.class).execute();
                for (StructrNode s : allNodes) {
                    if (s instanceof Image) {
                        images.add((Image) s);
                    }
                }

                Services.command(ExtractAndSetImageDimensionsAndFormat.class).execute(images);

                return images.size();

            }
        });

        return numberOfImages;
    }
}
