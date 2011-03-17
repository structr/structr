/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.cloud;

import org.structr.core.entity.StructrRelationship;

/**
 * Serializable data container for a relationship to be transported over network.
 *
 * To be initialized with {@link StructrRelationship} in constructor.
 *
 * @author axel
 */
public class RelationshipDataContainer extends DataContainer implements Comparable<RelationshipDataContainer> {

    protected long sourceStartNodeId;
    protected long sourceEndNodeId;
    protected String name;

    public RelationshipDataContainer() {};

    public RelationshipDataContainer(final StructrRelationship relationship) {

        name = relationship.getRelType().name();
        properties = relationship.getProperties();
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
