package org.structr.schema.json;

import java.util.Set;

/**
 *
 * @author Christian Morgner
 */
public interface JsonEnumProperty extends JsonStringProperty {

	public JsonEnumProperty setEnums(final String... values);
	public Set<String> getEnums();
}
