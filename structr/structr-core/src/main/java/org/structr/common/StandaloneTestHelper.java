/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.common;

import org.structr.core.Services;

//~--- JDK imports ------------------------------------------------------------

import java.util.Hashtable;
import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author chrisi
 */
public class StandaloneTestHelper {

	public static void prepareStandaloneTest(String databasePath) {
		Services.initialize(prepareStandaloneContext(databasePath));
	}

	public static void finishStandaloneTest() {
		Services.shutdown();
	}

	/**
	 * Encapsulates Thread.sleep() in a try-catch-block.
	 *
	 * @param millis
	 */
	public static void sleep(long millis) {

		try {
			Thread.sleep(millis);
		} catch (Throwable t) {

			// ignore
		}
	}

	// ----- private methods -----
	private static Map<String, String> prepareStandaloneContext(String databasePath) {

		Map<String, String> context = new Hashtable<String, String>();

		context.put(Services.DATABASE_PATH, databasePath);

		try {
			Class.forName("javax.servlet.ServletContext");
		} catch (Throwable t) {
			t.printStackTrace();
		}

		// add synthetic ServletContext
		context.put(Services.SERVLET_REAL_ROOT_PATH, "/temp/");

		return context;
	}
}
