/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud.message;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Date;
import org.structr.common.Syncable;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.SyncCommand;
import org.structr.dynamic.File;

/**
 *
 * @author Christian Morgner
 */
public class SyncableInfo {

	private boolean node      = false;
	private String id         = null;
	private String name       = null;
	private String type       = null;
	private Long size         = null;
	private Date lastModified = null;

	public SyncableInfo() {}

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

	protected void deserializeFrom(final Reader reader) throws IOException {

		this.node         = (Boolean)SyncCommand.deserialize(reader);
		this.id           = (String)SyncCommand.deserialize(reader);
		this.name         = (String)SyncCommand.deserialize(reader);
		this.type         = (String)SyncCommand.deserialize(reader);
		this.size         = (Long)SyncCommand.deserialize(reader);
		this.lastModified = (Date)SyncCommand.deserialize(reader);
	}

	protected void serializeTo(final Writer writer) throws IOException {

		SyncCommand.serialize(writer, node);
		SyncCommand.serialize(writer, id);
		SyncCommand.serialize(writer, name);
		SyncCommand.serialize(writer, type);
		SyncCommand.serialize(writer, size);
		SyncCommand.serialize(writer, lastModified);
	}
}
