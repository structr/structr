package org.structr.schema.importer;

/**
 *
 * @author Christian Morgner
 */
public class RelationshipInfo {

	private String startNodeType = null;
	private String endNodeType   = null;
	private String relType       = null;

	public RelationshipInfo(final String startNodeType, final String endNodeType, final String relType) {
		this.startNodeType = startNodeType;
		this.endNodeType   = endNodeType;
		this.relType       = relType;
	}

	public String getStartNodeType() {
		return startNodeType;
	}

	public String getEndNodeType() {
		return endNodeType;
	}

	public String getRelType() {
		return relType;
	}

	@Override
	public int hashCode() {
		return startNodeType.concat(relType).concat(endNodeType).hashCode();
	}

	@Override
	public boolean equals(final Object o) {

		if (o instanceof RelationshipInfo) {
			return ((RelationshipInfo)o).hashCode() == hashCode();
		}

		return false;
	}
	
	@Override
	public String toString() {
		
		return startNodeType + "-[:" + relType + "]->" + endNodeType;
	}
}
