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
module structr.neo4j.bolt.driver {

	requires structr.db.driver.api;
	requires org.neo4j.driver;
	requires com.google.gson;
	requires org.apache.commons.collections4;
	requires org.apache.commons.lang3;
	requires org.reactivestreams;
	requires reactor.core;
	requires org.slf4j;

	// the driver is consumed only through the DatabaseService SPI; no packages are exported
	provides org.structr.api.DatabaseService with org.structr.bolt.BoltDatabaseService;
}
