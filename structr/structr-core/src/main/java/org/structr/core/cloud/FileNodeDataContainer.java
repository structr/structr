/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.cloud;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.File;

/**
 * Transport data container for file nodes
 * 
 * @author axel
 */
public class FileNodeDataContainer extends NodeDataContainer {

    private static final Logger logger = Logger.getLogger(FileNodeDataContainer.class.getName());

    protected byte[] binaryContent;

    public FileNodeDataContainer(final AbstractNode node) {

        super(node);

        if (node instanceof File) {

            File fileNode = (File) node;

            estimatedSize += fileNode.getSize();

            try {
                
                binaryContent = IOUtils.toByteArray(fileNode.getInputStream());

            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Could not read file", ex);
            }

        }
    }

    public byte[] getBinaryContent() {
        return binaryContent;
    }
}
