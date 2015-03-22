package org.structr.schema.json;

/**
 *
 * @author Christian Morgner
 */
public interface JsonScriptProperty extends JsonProperty {

	public JsonScriptProperty setSource(final String source);
	public String getSource();

	public JsonScriptProperty setContentType(final String contentType);
	public String getContentType();
}
