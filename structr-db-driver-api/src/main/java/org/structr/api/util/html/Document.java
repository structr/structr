/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 *
 */
public class Document extends Tag {

	private PrintWriter writer = null;

	public Document(final PrintWriter writer) {
		super(null, "html", false, true);

		this.writer = writer;
	}

	public void render() throws IOException {

		writer.println("<!DOCTYPE html>");

		render(writer, 0);
	}
}
