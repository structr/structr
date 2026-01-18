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
package org.structr.autocomplete.keywords;

import org.structr.autocomplete.KeywordHint;
import org.structr.docs.Example;

import java.util.List;

public class BaseUrlHint extends KeywordHint {

	@Override
	public String getName() {
		return "baseUrl";
	}

	@Override
	public String getShortDescription() {
		return "Refers to the base URL for this Structr application.";
	}

	@Override
	public String getLongDescription() {
		return """
		The value is assembled from the protocol, hostname and port of the server instance Structr is running on.
	
		It produces `http(s)://<host>(:<port>)` depending on the configuration.";
		""";
	}

	@Override
	public List<Example> getExamples() {
		return null;
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"If `application.baseurl.override` is set in structr.conf, the value of that setting will be returned.",
			"If HTTPS is enabled, the result string will always begin with https://",
			"If this keyword is used in a script called without a request (e.g. a CRON job), the configuration keys `application.host` and `application.http.port` (or `application.https.port`) are returned. If a request object is available, the information will be taken from there."

		);
	}
}
