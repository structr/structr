package org.structr.rest.serialization;

import java.io.IOException;
import org.structr.core.GraphObject;

/**
 *
 * @author Christian Morgner
 */
public interface RestWriter {

	public void setIndent(final String indent);
	
	public void flush() throws IOException;

	public RestWriter beginDocument() throws IOException;
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
}
