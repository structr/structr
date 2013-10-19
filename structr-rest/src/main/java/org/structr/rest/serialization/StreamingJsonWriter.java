package org.structr.rest.serialization;

import java.io.Writer;
import org.structr.core.Value;

/**
 *
 * @author Christian Morgner
 */
public class StreamingJsonWriter extends StreamingWriter {

	public StreamingJsonWriter(final Value<String> propertyView, final boolean indent) {
		super(propertyView, indent);
	}
	
	@Override
	public RestWriter getRestWriter(Writer writer) {
		return new StructrJsonWriter(writer);
	}
}
