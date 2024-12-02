/*
 * Copyright (C) 2010-2024 Structr GmbH
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
public class TextNode extends Tag {

	private String textContent = null;

	public TextNode(final Tag parent, final String textContent) {
		super(parent, null, false, false);

		this.textContent = textContent;
	}

	protected void render(final PrintWriter writer, final int level) throws IOException {

		if (textContent != null) {
			writer.print(textContent);
		}
	}
}
