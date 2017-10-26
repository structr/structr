/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.files.external;

import java.nio.file.Path;

/**
 */
public class LoggingWatchEventListener implements WatchEventListener {

	@Override
	public void onDiscover(final Path path) {

		System.out.println("DISCOVER: " + path);
	}

	@Override
	public void onCreate(final Path path) {

		System.out.println("CREATE: " + path);
	}

	@Override
	public void onModify(final Path path) {

		System.out.println("MODIFY: " + path);
	}

	@Override
	public void onDelete(final Path path) {

		System.out.println("DELETE: " + path);
	}
}
