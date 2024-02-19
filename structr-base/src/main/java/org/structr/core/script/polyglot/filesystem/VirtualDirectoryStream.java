/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.script.polyglot.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Folder;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class VirtualDirectoryStream implements DirectoryStream<Path> {
    private static final Logger logger = LoggerFactory.getLogger(VirtualDirectoryStream.class);
    private final Path root;
    private final DirectoryStream.Filter<? super Path> filter;
    private List<Path> virtualPaths = new LinkedList<>();

    public VirtualDirectoryStream(final Path root, final DirectoryStream.Filter<? super Path> filter) {
        this.root = root;
        this.filter = filter;
    }
    @Override
    public Iterator<Path> iterator() {

        findPaths(this.root);
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return !virtualPaths.isEmpty();
            }

            @Override
            public Path next() {
                Path next = virtualPaths.get(0);
                virtualPaths.remove(0);
                return next;
            }
        };
    }

    @Override
    public void close() throws IOException {
    }

    private boolean applyFilter(final Path p) {
        try {

            return filter == null || filter.accept(p);
        } catch (IOException ex) {

            logger.error("Error while trying to filter found paths.", ex);
            return false;
        }
    }
    private void findPaths(Path root) {
        App app = StructrApp.getInstance();

        try (final Tx tx = app.tx()) {

            PropertyKey<Folder> parentKey = StructrApp.key(AbstractFile.class, "parent");

            if (!(root.toString().equals("/"))) {
                PropertyKey<String> path = StructrApp.key(AbstractFile.class, "path");
                Folder rootFolder = app.nodeQuery(Folder.class).and(path, root.toString()).getFirst();

                if (rootFolder != null) {
                    app.nodeQuery(AbstractFile.class).and(parentKey, rootFolder)
                            .getAsList()
                            .stream()
                            .map(f -> Path.of(f.getPath()))
                            .filter(this::applyFilter)
                            .forEach(p -> virtualPaths.add(p));
                }

            } else {

                app.nodeQuery(AbstractFile.class).and(parentKey, null)
                        .getAsList()
                        .stream()
                        .map(f -> Path.of(f.getPath()))
                        .filter(this::applyFilter)
                        .forEach(p -> virtualPaths.add(p));
            }

            tx.success();
        } catch (FrameworkException ex) {

            logger.error("Could not find paths for VirtualDirectoryStream.", ex);
        }

    }
}
