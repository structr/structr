package org.structr.schema.export;

import java.util.Set;
import java.util.TreeSet;
import org.structr.schema.json.JsonProperty;
import org.structr.schema.json.JsonReferenceProperty;
import org.structr.schema.json.JsonType;

/**
 *
 * @author Christian Morgner
 */
public abstract class StructrReferenceProperty extends StructrPropertyDefinition implements JsonReferenceProperty {

	protected final Set<String> properties = new TreeSet<>();

	StructrReferenceProperty(final JsonType parent, final String name) {
		super(parent, name);
	}

	@Override
	public JsonReferenceProperty setProperties(String... propertyNames) {

		for (final String name : propertyNames) {
			properties.add(name);
		}

		return this;
	}

	@Override
	public Set<String> getProperties() {
		return properties;
	}

	@Override
	public int compareTo(final JsonProperty o) {
		return getName().compareTo(o.getName());
	}

	@Override
	public String getFormat() {
		return format;
	}

	@Override
	public String getDefaultValue() {
		return defaultValue;
	}

	@Override
	public boolean isRequired() {
		return required;
	}

	@Override
	public boolean isUnique() {
		return unique;
	}

	@Override
	public JsonProperty setFormat(String format) {

		this.format = format;
		return this;
	}

	@Override
	public JsonProperty setRequired(boolean isRequired) {

		this.required = isRequired;
		return this;
	}

	@Override
	public JsonProperty setUnique(boolean isUnique) {

		this.unique = isUnique;
		return this;
	}

	@Override
	public JsonProperty setDefaultValue(String defaultValue) {

		this.defaultValue = defaultValue;
		return this;
	}

	// ----- package methods -----
	@Override
	void initializeReferences() {
	}
}
