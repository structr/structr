package org.structr.common;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.neo4j.collections.rtree.RTreeRelationshipTypes;
import org.neo4j.gis.spatial.SpatialRelationshipTypes;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.helpers.Function;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.Iterables;
import org.structr.core.GraphObject;

/**
 *
 * @author Christian Morgner
 */
	
public class StructrAndSpatialPredicate implements Predicate<PropertyContainer> {

	private static final Set<String> spatialRelationshipTypes = new LinkedHashSet<>();
	private static final String idName                        = GraphObject.id.dbName();

	static {
		
		// collect spatial relationship types
		spatialRelationshipTypes.addAll(Iterables.toList(Iterables.map(new RelationshipName(), Arrays.asList(RTreeRelationshipTypes.values()))));
		spatialRelationshipTypes.addAll(Iterables.toList(Iterables.map(new RelationshipName(), Arrays.asList(SpatialRelationshipTypes.values()))));
	}
	
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

			if (isStructrEntity) {
				return includeStructr;
			}
			
			if (isSpatialEntity) {
				return includeSpatial;
			}
			
			return includeOther;

		} else if (container instanceof Relationship) {

			final boolean isSpatialEntity = spatialRelationshipTypes.contains(((Relationship)container).getType().name());

			if (isStructrEntity) {
				return includeStructr;
			}
			
			if (isSpatialEntity) {
				return includeSpatial;
			}
			
			return includeOther;
		}

		return true;
	}
	
	private static class RelationshipName implements Function<RelationshipType, String> {

		@Override
		public String apply(RelationshipType from) {
			return from.name();
		}
	}
}
