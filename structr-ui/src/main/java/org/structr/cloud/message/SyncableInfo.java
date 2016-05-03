/**
 * Copyright (C) 2010-2016 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.SyncCommand;
import org.structr.dynamic.File;

/**
 *
 *
 */
public class SyncableInfo {

	private boolean node                        = false;
	private String id                           = null;
	private String name                         = null;
	private String type                         = null;
	private boolean visibleToPublicUsers        = false;
	private boolean visibleToAuthenticatedUsers = false;
	private long size                           = 0L;
	private long lastModified                   = 0L;

	public SyncableInfo() {}

	public SyncableInfo(final GraphObject syncable) {

		if (syncable != null) {

			if (syncable.isNode()) {

				final NodeInterface node = syncable.getSyncNode();
				this.id           = node.getUuid();
				this.name         = node.getName();
				this.type         = node.getType();
				this.lastModified = node.getLastModifiedDate().getTime();
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
		return new Date(lastModified);
	}

	public boolean isVisibleToPublicUsers() {
		return visibleToPublicUsers;
	}

	public boolean isVisibleToAuthenticatedUsers() {
		return visibleToAuthenticatedUsers;
	}

	protected void deserializeFrom(final DataInputStream inputStream) throws IOException {

		this.node                        = (Boolean)SyncCommand.deserialize(inputStream);
		this.id                          = (String)SyncCommand.deserialize(inputStream);
		this.name                        = (String)SyncCommand.deserialize(inputStream);
		this.type                        = (String)SyncCommand.deserialize(inputStream);
		this.visibleToPublicUsers        = (Boolean)SyncCommand.deserialize(inputStream);
		this.visibleToAuthenticatedUsers = (Boolean)SyncCommand.deserialize(inputStream);
		this.size                        = (Long)SyncCommand.deserialize(inputStream);
		this.lastModified                = (Long)SyncCommand.deserialize(inputStream);
	}

	protected void serializeTo(final DataOutputStream outputStream) throws IOException {

		SyncCommand.serialize(outputStream, node);
		SyncCommand.serialize(outputStream, id);
		SyncCommand.serialize(outputStream, name);
		SyncCommand.serialize(outputStream, type);
		SyncCommand.serialize(outputStream, visibleToPublicUsers);
		SyncCommand.serialize(outputStream, visibleToAuthenticatedUsers);
		SyncCommand.serialize(outputStream, size);
		SyncCommand.serialize(outputStream, lastModified);
	}
}
