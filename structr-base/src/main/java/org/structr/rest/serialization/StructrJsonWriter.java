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

import com.google.gson.stream.JsonWriter;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;

import java.io.IOException;
import java.io.Writer;

/**
 *
 *
 */
public class StructrJsonWriter implements RestWriter {

	private SecurityContext securityContext = null;
	private JsonWriter writer               = null;
	private Writer rawWriter                = null;
	private int pageSize                    = -1;
	private int page                        = 1;

	public StructrJsonWriter(final SecurityContext securityContext, final Writer writer) {

		this.securityContext = securityContext;
		this.writer          = new JsonWriter(writer);
		this.rawWriter       = writer;

		this.writer.setLenient(Settings.JsonLenient.getValue());
	}

	@Override
	public void setIndent(String indent) {
		writer.setIndent(indent);
	}

	@Override
	public SecurityContext getSecurityContext() {
		return securityContext;
	}

	@Override
	public RestWriter beginDocument(final String baseUrl, final String propertyView) throws IOException {
		return this;
	}

	@Override
	public RestWriter endDocument() throws IOException {
		return this;
	}

	@Override
	public RestWriter beginArray() throws IOException {
		writer.beginArray();
		return this;
	}

	@Override
	public RestWriter endArray() throws IOException {
		writer.endArray();
		return this;
	}

	@Override
	public RestWriter beginObject() throws IOException {
		return beginObject(null);
	}

	@Override
	public RestWriter beginObject(final GraphObject graphObject) throws IOException {
		increaseSerializationDepth();
		writer.beginObject();
		return this;
	}

	@Override
	public RestWriter endObject() throws IOException {
		return endObject(null);
	}

	@Override
	public RestWriter endObject(final GraphObject graphObject) throws IOException {
		decreaseSerializationDepth();
		writer.endObject();
		return this;
	}

	@Override
	public RestWriter name(String name) throws IOException {
		writer.name(name);
		return this;
	}

	@Override
	public RestWriter value(String value) throws IOException {
		writer.value(value);
		return this;
	}

	@Override
	public RestWriter nullValue() throws IOException {
		writer.nullValue();
		return this;
	}

	@Override
	public RestWriter value(boolean value) throws IOException {
		writer.value(value);
		return this;
	}

	@Override
	public RestWriter value(double value) throws IOException {
		writer.value(value);
		return this;
	}

	@Override
	public RestWriter value(long value) throws IOException {
		writer.value(value);
		return this;
	}

	@Override
	public RestWriter value(Number value) throws IOException {
		writer.value(value);
		return this;
	}

	@Override
	public void raw(final String data) throws IOException {
		writer.flush();
		rawWriter.append(data);
		rawWriter.flush();
	}

	@Override
	public void flush() throws IOException {
		writer.flush();
	}

	@Override
	public void setPageSize(final int pageSize) {
		this.pageSize = pageSize;
	}

	@Override
	public int getPageSize() {
		return pageSize;
	}

	@Override
	public void setPage(final int page) {
		this.page = page;
	}

	@Override
	public int getPage() { return page; }

}
