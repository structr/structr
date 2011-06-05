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
package org.structr.core.cloud;

import java.io.IOException;
import java.io.InputStream;
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

    public FileNodeDataContainer() {
    }

    public FileNodeDataContainer(final AbstractNode node) {

        super(node);

        if (node instanceof File) {

            File fileNode = (File) node;

            estimatedSize += fileNode.getSize();

            try {

                InputStream in = fileNode.getInputStream();

                if (in != null) {
                    binaryContent = IOUtils.toByteArray(in);
                }

            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Could not read file", ex);
            }

        }
    }

    public byte[] getBinaryContent() {
        return binaryContent;
    }
}
