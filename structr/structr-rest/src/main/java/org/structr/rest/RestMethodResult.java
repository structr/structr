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

import com.google.gson.Gson;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.structr.core.GraphObject;
import org.structr.rest.resource.Result;

/**
 * Encapsulates the result of a REST HTTP method call, i.e.
 * headers, response code etc.
 *
 * @author Christian Morgner
 */
public class RestMethodResult {

	private static final Logger logger = Logger.getLogger(RestMethodResult.class.getName());

	private List<GraphObject> content = null;
	private Map<String, String> headers = null;
	private int responseCode = 0;

	public RestMethodResult(int responseCode) {
		headers = new LinkedHashMap<String, String>();
		this.responseCode = responseCode;
	}

	public void addHeader(String key, String value) {
		headers.put(key, value);
	}

	public void addContent(GraphObject graphObject) {

		if(this.content == null) {
			this.content = new LinkedList<GraphObject>();
		}

		this.content.add(graphObject);
	}

	public void commitResponse(Gson gson, HttpServletResponse response) {

		// set headers
		for(Entry<String, String> header : headers.entrySet()) {
			response.setHeader(header.getKey(), header.getValue());
		}

		// set  response code
		response.setStatus(responseCode);

		try {

			Writer writer = response.getWriter();
			if(content != null) {

				// create result set
				Result result = new Result(this.content, this.content.size() > 1, false);

				// serialize result set
				gson.toJson(result, writer);
			}


			writer.flush();
			writer.close();


		} catch(Throwable t) {

			logger.log(Level.WARNING, "Unable to commit HttpServletResponse", t);
		}
	}
}
