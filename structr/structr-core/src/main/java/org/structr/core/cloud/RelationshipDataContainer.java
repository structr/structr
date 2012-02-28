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

import org.structr.core.entity.AbstractRelationship;

/**
 * Serializable data container for a relationship to be transported over network.
 *
 * To be initialized with {@link AbstractRelationship} in constructor.
 *
 * @author axel
 */
public class RelationshipDataContainer extends DataContainer implements Comparable<RelationshipDataContainer> {

    protected long sourceStartNodeId;
    protected long sourceEndNodeId;
    protected String name;

    public RelationshipDataContainer() {};

    public RelationshipDataContainer(final AbstractRelationship relationship) {

        name = relationship.getRelType().name();
        properties.putAll(relationship.getProperties());
        sourceStartNodeId = relationship.getStartNode().getId();
        sourceEndNodeId = relationship.getEndNode().getId();

    }

    /**
     * Return name
     * 
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Return id of start node in source instance
     *
     * @return
     */
    public long getSourceStartNodeId() {
        return sourceStartNodeId;
    }

    /**
     * Return id of end node in source instance
     *
     * @return
     */
    public long getSourceEndNodeId() {
        return sourceEndNodeId;
    }

    @Override
    public int compareTo(RelationshipDataContainer t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean equals(final Object t) {

        RelationshipDataContainer other = (RelationshipDataContainer) t;

        return (this.getName().equals(other.getName()) && this.getSourceStartNodeId() == other.getSourceStartNodeId() && this.getSourceEndNodeId() == other.getSourceEndNodeId());

    }

}
