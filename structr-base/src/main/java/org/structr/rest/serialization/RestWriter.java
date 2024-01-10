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
package org.structr.rest.serialization;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;

import java.io.IOException;

/**
 *
 *
 */
public interface RestWriter {

	public void setIndent(final String indent);
	public SecurityContext getSecurityContext();
	public int getPageSize();
	public void setPageSize(final int pageSize);
	public int getPage();
	public void setPage(final int page);

	public RestWriter beginDocument(final String baseUrl, final String propertyView) throws IOException;
	public RestWriter endDocument() throws IOException;
	public RestWriter beginArray() throws IOException;
	public RestWriter endArray() throws IOException;
	public RestWriter beginObject() throws IOException;
	public RestWriter beginObject(final GraphObject graphObject) throws IOException;
	public RestWriter endObject() throws IOException;
	public RestWriter endObject(final GraphObject graphObject) throws IOException;
	public RestWriter name(final String name) throws IOException;
	public RestWriter value(final String value) throws IOException;
	public RestWriter nullValue() throws IOException;
	public RestWriter value(final boolean value) throws IOException;
	public RestWriter value(final double value) throws IOException;
	public RestWriter value(final long value) throws IOException;
	public RestWriter value(final Number value) throws IOException;

	public void raw(final String data) throws IOException;
	public void flush() throws IOException;

	default public void increaseSerializationDepth() {

		getSecurityContext().increaseSerializationDepth();
	}

	default public void decreaseSerializationDepth() {

		getSecurityContext().decreaseSerializationDepth();
	}
}
