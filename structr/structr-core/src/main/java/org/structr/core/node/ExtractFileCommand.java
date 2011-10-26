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
package org.structr.core.node;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.MimetypesFileTypeMap;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.common.RelType;
import org.structr.core.entity.File;
import org.structr.core.entity.Image;
import org.structr.core.entity.User;

/**
 * Extract a file and create subnodes
 *
 *
 * @author amorgner
 */
public class ExtractFileCommand extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(ExtractFileCommand.class.getName());
    private static final String PATH_SEPARATOR = "/";

    /**
     *
     * @param parameters
     * @return
     */
    @Override
    public Object execute(Object... parameters) {
        AbstractNode node = null;
        AbstractNode targetNode = null;
        User user = null;

        Command findNode = Services.command(securityContext, FindNodeCommand.class);

        switch (parameters.length) {

            case 3:

                if (parameters[0] instanceof Long) {
                    long id = ((Long) parameters[0]).longValue();
                    node = (AbstractNode) findNode.execute(id);

                } else if (parameters[0] instanceof AbstractNode) {
                    node = (AbstractNode) parameters[0];

                } else if (parameters[0] instanceof String) {
                    long id = Long.parseLong((String) parameters[0]);
                    node = (AbstractNode) findNode.execute(id);
                }

                if (parameters[1] instanceof Long) {
                    long id = ((Long) parameters[1]).longValue();
                    targetNode = (AbstractNode) findNode.execute(id);

                } else if (parameters[1] instanceof AbstractNode) {
                    targetNode = (AbstractNode) parameters[1];

                } else if (parameters[1] instanceof String) {
                    long id = Long.parseLong((String) parameters[1]);
                    targetNode = (AbstractNode) findNode.execute(id);
                }

                if (parameters[2] instanceof User) {
                    user = (User) parameters[2];
                }

                break;

            default:
                break;

        }

        doExtractFileNode(node, targetNode, user);

        return null;
    }

    private void doExtractFileNode(AbstractNode node, AbstractNode targetNode, User user) {

        if (node != null) {

            if (!(node instanceof File)) {
                setExitCode(Command.exitCode.FAILURE);
                setErrorMessage("Could not extract content of node" + node.getId());
                logger.log(Level.WARNING, getErrorMessage());
                return;
            }

            File archiveNode = (File) node;

            BufferedInputStream in = new BufferedInputStream(archiveNode.getInputStream());
            ArchiveInputStream input = null;
            try {

                input = new ArchiveStreamFactory().createArchiveInputStream(in);

                if (input != null) {

                    Command createNode = Services.command(securityContext, CreateNodeCommand.class);
                    Command createRel = Services.command(securityContext, CreateRelationshipCommand.class);

                    Map<String, AbstractNode> createdPaths = new HashMap<String, AbstractNode>();

                    try {

                        ArchiveEntry ae = input.getNextEntry();

                        do {

                            String name = ae.getName();
                            long size = ae.getSize();
                            boolean isDirectory = ae.isDirectory();
                            //Date lastModDate = ae.getLastModifiedDate();


                            if (!isDirectory) {

                                // check path for subfolders, f.e. like "abc/xyc/123"
                                if (name.indexOf("/") > 0) {

                                    AbstractNode parentNode = targetNode;

                                    String[] pathElements = name.split(PATH_SEPARATOR);
                                    int count = 0;
                                    for (String p : pathElements) {

                                        List<NodeAttribute> attrs = new LinkedList<NodeAttribute>();
                                        if (count < pathElements.length - 1) {
                                            attrs.add(new NodeAttribute(AbstractNode.Key.type.name(), "Folder"));
                                        } else {
                                            // last path element is the file

                                            // Detect content type from filename (extension)
                                            String contentType = getContentTypeFromFilename(ae.getName());
                                            attrs.add(new NodeAttribute(File.Key.contentType.name(), contentType));

                                            if (contentType != null && contentType.startsWith("image")) {
                                                // If it seems to be an image, use Image type
                                                attrs.add(new NodeAttribute(AbstractNode.Key.type.name(), Image.class.getSimpleName()));
                                            } else {
                                                // Default is File type
                                                attrs.add(new NodeAttribute(AbstractNode.Key.type.name(), File.class.getSimpleName()));
                                            }
                                        }

                                        // check if node with this path was already created
                                        // and if yes, skip this one
                                        StringBuilder path = new StringBuilder();
                                        for (int i = 0; i <= count; i++) {
                                            path.append(pathElements[i]).append(PATH_SEPARATOR);
                                        }

                                        if (!(createdPaths.containsKey(path.toString()))) {

                                            attrs.add(new NodeAttribute(AbstractNode.Key.name.name(), p));

                                            // create the node
                                            AbstractNode childNode = (AbstractNode) createNode.execute(attrs, user, true);

                                            // write the file
                                            if (childNode instanceof org.structr.core.entity.File) {
                                                writeFile(childNode, input);
                                            }

                                            // link file node to parent node
                                            createRel.execute(parentNode, childNode, RelType.HAS_CHILD);


                                            parentNode = childNode;

                                            // save this path and the corresponding node
                                            createdPaths.put(path.toString(), parentNode);

                                        } else {
                                            // set existing folder node as new parent node, so
                                            // the relationship will set correctly in next loop
                                            parentNode = (AbstractNode) createdPaths.get(path.toString());

                                        }
                                        count++;

                                    }


                                } else {

                                    // create plain file (no sub directory)
                                    NodeAttribute typeAttr = new NodeAttribute(AbstractNode.Key.type.name(), "File");
                                    NodeAttribute sizeAttr = new NodeAttribute(File.Key.size.name(), size);
                                    NodeAttribute nameAttr = new NodeAttribute(AbstractNode.Key.name.name(), name);

                                    AbstractNode fileNode = (AbstractNode) createNode.execute(nameAttr, typeAttr, sizeAttr, user);
                                    createRel.execute(targetNode, fileNode, RelType.HAS_CHILD);

                                    writeFile(fileNode, input);

                                }

                            }
                            // don't create plain folders (?)
//                            } else {
//                                // create folder
//                                NodeAttribute typeAttr = new NodeAttribute(AbstractNode.Key.type.name(), "Folder");
//                                NodeAttribute sizeAttr = new NodeAttribute(File.SIZE_KEY, size);
//                                NodeAttribute nameAttr = new NodeAttribute(AbstractNode.Key.name.name(), name);
//
//                                AbstractNode folderNode = (AbstractNode) createNode.execute(nameAttr, typeAttr, sizeAttr);
//
//                                createRel.execute(RelType.HAS_CHILD, targetNode, folderNode);
//                            }

                            ae = input.getNextEntry();

                        } while (ae != null);

                    } catch (IOException e) {
                        setErrorMessage("Could not read from archive stream");
                        setExitCode(Command.exitCode.FAILURE);
                        logger.log(Level.WARNING, getErrorMessage().concat(": {0}"), e.getMessage());
                        return;
                    }
                }

            } catch (ArchiveException e) {
                setErrorMessage("Unknown Archive format");
                setExitCode(Command.exitCode.FAILURE);
                logger.log(Level.WARNING, getErrorMessage().concat(": {0}"), e.getMessage());
                return;
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Exception while closing input stream: " + e.getMessage());
                }

            }

        } else {
            setErrorMessage("Node to extract was null");
            setExitCode(Command.exitCode.FAILURE);
            logger.log(Level.WARNING, getErrorMessage());
            return;
        }

        // everything is fine
        setExitCode(Command.exitCode.SUCCESS);

    }

    private void writeFile(final AbstractNode fileNode, InputStream input) {

        // write file
        String path = fileNode.getId() + "_" + System.currentTimeMillis();

        OutputStream out;
        try {
            out = new FileOutputStream(new java.io.File(Services.getFilesPath(), path));
            IOUtils.copy(input, out);
            out.close();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception while writing file: {0}", e.getMessage());
        }

        fileNode.setProperty(File.Key.relativeFilePath.name(), path);

    }

    private String getContentTypeFromFilename(final String filename) {
          return new MimetypesFileTypeMap().getContentType(filename);
    }

}
