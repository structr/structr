/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.api.util.html;

public class DatabaseSelectField extends SelectField {

	public DatabaseSelectField(final Tag parent, final String id) {

		this(parent, id, null);
	}

	public DatabaseSelectField(Tag parent, String id, final String value) {

		super(parent, id, value);

		addOption("Neo4j Remote (Bolt)", "org.structr.bolt.BoltDatabaseService");
		addOption("Neo4j Embedded", "org.structr.embedded.EmbeddedDatabaseService");
		addOption("In-Memory (non-persistent, all data will be erased on shutdown!)", "org.structr.memory.MemoryDatabaseService");
	}
}
