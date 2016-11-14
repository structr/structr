/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.api.config;

/**
 *
 */
public interface Structr {

	public static final String DATABASE_DRIVER               = "database.driver";
	public static final String DATABASE_DRIVER_MODE          = "database.driver.mode";
	public static final String DATABASE_CONNECTION_URL       = "database.connection.url";
	public static final String DATABASE_CONNECTION_USERNAME  = "database.connection.username";
	public static final String DATABASE_CONNECTION_PASSWORD  = "database.connection.password";

	public static final String DATABASE_PATH                 = "database.path";
	public static final String RELATIONSHIP_CACHE_SIZE       = "database.cache.relationship.size";
	public static final String NODE_CACHE_SIZE               = "database.cache.node.size";
	public static final String QUERY_CACHE_SIZE              = "database.cache.query.size";

	public static final String LOG_CYPHER_DEBUG              = "log.cypher.debug";

	public static final String DEFAULT_DATABASE_URL          = "bolt://localhost:7688";
	public static final String TEST_DATABASE_URL             = "bolt://localhost:7689";
}
