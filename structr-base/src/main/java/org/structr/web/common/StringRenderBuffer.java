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
package org.structr.web.common;

/**
 * This class serves as a wrapper around the {@link AsyncBuffer} to be able to
 * get the output of the render method as a String.
 *
 *
 */
public class StringRenderBuffer extends AsyncBuffer {

	private StringBuilder buf = new StringBuilder();

	@Override
	public AsyncBuffer append(final String s) {
		buf.append(s);
		return this;
	}

	public StringBuilder getBuffer() {
		return buf;
	}
}
