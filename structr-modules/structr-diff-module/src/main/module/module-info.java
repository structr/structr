/*
 * Copyright (C) 2010-2026 Structr GmbH
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
module structr.diff.module {

	// gson and slf4j arrive as 'requires transitive' on structr.base, so they are not named again
	requires structr.base;

	// StructrWebSocket instantiates the command reflectively, which JPMS refuses without an export
	exports org.structr.diff.websocket;

	/* The module-path half of discovery. META-INF/services/org.structr.module.StructrModule must
	   carry the same entry: tests run on the CLASS path, where only that file is read. */
	provides org.structr.module.StructrModule with
		org.structr.diff.DiffModule;
}
