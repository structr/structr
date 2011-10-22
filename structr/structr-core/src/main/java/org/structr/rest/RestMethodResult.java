/*
 *  Copyright (C) 2011 Axel Morgner
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

package org.structr.rest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;

/**
 * Encapsulates the result of a REST HTTP method call, i.e.
 * headers, response code etc.
 *
 * @author Christian Morgner
 */
public class RestMethodResult {

	private static final Logger logger = Logger.getLogger(RestMethodResult.class.getName());

	private Map<String, String> headers = null;
	private int responseCode = 0;

	public RestMethodResult(int responseCode) {
		headers = new LinkedHashMap<String, String>();
		this.responseCode = responseCode;
	}

	public void addHeader(String key, String value) {
		headers.put(key, value);
	}

	public void commitResponse(HttpServletResponse response) {

		// set headers
		for(Entry<String, String> header : headers.entrySet()) {
			response.setHeader(header.getKey(), header.getValue());
		}

		// set  response code
		response.setStatus(responseCode);

		try {

			// commit response
			response.getOutputStream().flush();
			response.getOutputStream().close();

		} catch(Throwable t) {

			logger.log(Level.WARNING, "Unable to commit HttpServletResponse", t);
		}
	}
}
