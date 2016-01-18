package org.structr.api.graph;

/**
 *
 */
public interface RelationshipType {

	String name();

	public static RelationshipType forName(final String name) {

		return new RelationshipType() {

			@Override
			public String name() {
				return name;
			}
		};
	}
}
