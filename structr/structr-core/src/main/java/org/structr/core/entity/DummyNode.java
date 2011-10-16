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
package org.structr.core.entity;

import org.neo4j.graphdb.Node;

/**
 * Dummy node with no connection to database node
 *
 * Any property getter will return null
 *
 * @author amorgner
 * 
 */
public class DummyNode extends AbstractNode {

    private final static String ICON_SRC = "/images/error.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    public DummyNode() {
    }

    public DummyNode(Node dbNode) {
    }

    @Override
    public long getId() {
        return -1;
    }

    @Override
    public Object getProperty(final String key) {
        return null;
    }
}
