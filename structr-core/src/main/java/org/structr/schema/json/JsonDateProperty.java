package org.structr.schema.json;

/**
 *
 * @author Christian Morgner
 */
public interface JsonDateProperty extends JsonStringProperty {

	public JsonDateProperty setDatePattern(final String datePattern);
	public String getDatePattern();
}
