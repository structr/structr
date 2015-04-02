package org.structr.schema.json;

import java.util.Set;

/**
 *
 * @author Christian Morgner
 */

public interface JsonReferenceProperty extends JsonProperty {

	public JsonReferenceProperty setProperties(final String... propertyNames);
	public Set<String> getProperties();
}
