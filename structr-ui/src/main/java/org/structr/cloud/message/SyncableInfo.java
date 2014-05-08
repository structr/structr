package org.structr.cloud.message;

import java.io.Serializable;
import org.structr.common.Syncable;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;

/**
 *
 * @author Christian Morgner
 */
public class SyncableInfo implements Serializable {

	private String id    = null;
	private String name = null;
	private String type = null;

	public SyncableInfo(final Syncable syncable) {

		if (syncable != null) {

			if (syncable.isNode()) {

				final NodeInterface node = syncable.getSyncNode();
				this.id   = node.getUuid();
				this.name = node.getName();
				this.type = node.getType();

			} else {

				final RelationshipInterface rel = syncable.getSyncRelationship();
				this.id   = rel.getUuid();
				this.name = rel.getRelType().name();
				this.type = rel.getType();

			}
		}
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}


}
