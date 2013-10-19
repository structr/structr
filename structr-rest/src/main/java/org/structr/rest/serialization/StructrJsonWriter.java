package org.structr.rest.serialization;

import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Writer;
import org.structr.core.GraphObject;

/**
 *
 * @author Christian Morgner
 */
public class StructrJsonWriter implements RestWriter {

	private JsonWriter writer = null;
	
	public StructrJsonWriter(final Writer writer) {
		this.writer = new JsonWriter(writer);
	}
	
	@Override
	public void setIndent(String indent) {
		writer.setIndent(indent);
	}

	@Override
	public void flush() throws IOException {
		writer.flush();
	}

	@Override
	public RestWriter beginDocument() throws IOException {
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
		writer.beginObject();
		return this;
	}

	@Override
	public RestWriter endObject() throws IOException {
		return endObject(null);
	}

	@Override
	public RestWriter endObject(final GraphObject graphObject) throws IOException {
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
}
