package org.structr.cloud.message;

import java.io.Serializable;
import java.util.Date;
import org.structr.common.Syncable;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.web.entity.File;

/**
 *
 * @author Christian Morgner
 */
public class SyncableInfo implements Serializable {

	private boolean node      = false;
	private String id         = null;
	private String name       = null;
	private String type       = null;
	private Long size         = null;
	private Date lastModified = null;

	public SyncableInfo(final Syncable syncable) {

		if (syncable != null) {

			if (syncable.isNode()) {

				final NodeInterface node = syncable.getSyncNode();
				this.id           = node.getUuid();
				this.name         = node.getName();
				this.type         = node.getType();
				this.lastModified = node.getLastModifiedDate();
				this.node         = true;

				if (node instanceof File) {
					this.size = ((File)node).getSize();
				}

			} else {

				final RelationshipInterface rel = syncable.getSyncRelationship();
				this.id           = rel.getUuid();
				this.name         = rel.getRelType().name();
				this.type         = rel.getClass().getSimpleName();
				this.node         = false;
			}
		}
	}

	public boolean isNode() {
		return node;
	}

	public boolean isRelationship() {
		return !isNode();
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

	public Long getSize() {
		return size;
	}

	public Date getLastModified() {
		return lastModified;
	}
}
