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
package org.structr.rest.serialization;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;

import java.io.IOException;

/**
 *
 *
 */
public interface RestWriter {

	void setIndent(final String indent);
	SecurityContext getSecurityContext();
	int getPageSize();
	void setPageSize(final int pageSize);
	int getPage();
	void setPage(final int page);

	RestWriter beginDocument(final String baseUrl, final String propertyView) throws IOException;
	RestWriter endDocument() throws IOException;
	RestWriter beginArray() throws IOException;
	RestWriter endArray() throws IOException;
	RestWriter beginObject() throws IOException;
	RestWriter beginObject(final GraphObject graphObject) throws IOException;
	RestWriter endObject() throws IOException;
	RestWriter endObject(final GraphObject graphObject) throws IOException;
	RestWriter name(final String name) throws IOException;
	RestWriter value(final String value) throws IOException;
	RestWriter nullValue() throws IOException;
	RestWriter value(final boolean value) throws IOException;
	RestWriter value(final double value) throws IOException;
	RestWriter value(final long value) throws IOException;
	RestWriter value(final Number value) throws IOException;

	void raw(final String data) throws IOException;
	void flush() throws IOException;

	default void increaseSerializationDepth() {

		getSecurityContext().increaseSerializationDepth();
	}

	default void decreaseSerializationDepth() {

		getSecurityContext().decreaseSerializationDepth();
	}
}
