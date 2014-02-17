package org.structr.common;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.neo4j.collections.rtree.RTreeRelationshipTypes;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.helpers.Predicate;
import org.structr.core.GraphObject;

/**
 *
 * @author Christian Morgner
 */
	
public class StructrAndSpatialPredicate implements Predicate<PropertyContainer> {

	private static final Set<? extends RelationshipType> spatialRelationshipTypes = new LinkedHashSet<>(Arrays.asList(RTreeRelationshipTypes.values()));
	private static final String idName                                            = GraphObject.id.dbName();

	private boolean includeStructr = false;
	private boolean includeSpatial = false;
	private boolean includeOther   = false;
	
	public StructrAndSpatialPredicate(final boolean includeStructrEntities, final boolean includeSpatialEntities, final boolean includeOtherNodes) {
		this.includeStructr = includeStructrEntities;
		this.includeSpatial = includeSpatialEntities;
		this.includeOther   = includeOtherNodes;
	}
	
	@Override
	public boolean accept(PropertyContainer container) {

		final boolean isStructrEntity = container.hasProperty(idName) && (container.getProperty(idName) instanceof String);

		if (container instanceof Node) {

			final boolean isSpatialEntity = ((Node)container).hasRelationship(RTreeRelationshipTypes.values());

			if (includeStructr && isStructrEntity) {
				return true;
			}
			
			if (includeSpatial && isSpatialEntity) {
				return true;
			}
			
			return includeOther;

		} else if (container instanceof Relationship) {

			final boolean isSpatialEntity = spatialRelationshipTypes.contains(((Relationship)container).getType());

			if (includeStructr && isStructrEntity) {
				return true;
			}
			
			if (includeSpatial && isSpatialEntity) {
				return true;
			}
			
			return includeOther;
		}

		return true;
	}
}
