/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.rest.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.rest.serialization.html.Document;
import org.structr.rest.serialization.html.Tag;

/**
 *
 * @author Christian Morgner
 */
public class ConfigServlet extends HttpServlet {

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		// no trailing semicolon so we dont trip MimeTypes.getContentTypeWithoutCharset
		response.setContentType("text/html; charset=utf-8");

		try (final PrintWriter writer = new PrintWriter(response.getWriter())) {

			final Document doc = createConfigDocument(writer);
			doc.render();

			writer.append("\n");    // useful newline
			writer.flush();

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
	}

	// ----- private methods -----
	private Document createConfigDocument(final PrintWriter writer) {

		final Document doc = new Document(writer);

		doc.block("head").block("title").text("Structr Configuration Editor");

		final Tag body = doc.block("body");

		body.block("h1").text("Test");


		return doc;
	}
}