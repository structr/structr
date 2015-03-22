package org.structr.schema.json;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/**
 *
 * @author Christian Morgner
 */
public interface JsonSchema {

	public static final String SCHEMA_ID       = "https://structr.org/schema#";

	public static final String KEY_SCHEMA       = "$schema";
	public static final String KEY_SIZE_OF      = "$size";
	public static final String KEY_REFERENCE    = "$ref";
	public static final String KEY_MAP          = "$map";

	public static final String KEY_TYPE         = "type";
	public static final String KEY_TITLE        = "title";
	public static final String KEY_DESCRIPTION  = "description";
	public static final String KEY_ENUM         = "enum";
	public static final String KEY_FORMAT       = "format";
	public static final String KEY_ITEMS        = "items";
	public static final String KEY_DEFINITIONS  = "definitions";
	public static final String KEY_PROPERTIES   = "properties";
	public static final String KEY_VIEWS        = "views";
	public static final String KEY_METHODS      = "methods";
	public static final String KEY_REQUIRED     = "required";
	public static final String KEY_MIN_ITEMS    = "minItems";
	public static final String KEY_MAX_ITEMS    = "maxItems";
	public static final String KEY_SOURCE       = "source";
	public static final String KEY_CONTENT_TYPE = "contentType";
	public static final String KEY_RELATIONSHIP = "rel";
	public static final String KEY_DIRECTION    = "direction";
	public static final String KEY_UNIQUE       = "unique";
	public static final String KEY_DEFAULT      = "default";

	public URI getId();

	public Set<JsonType> getTypes();

	public String getTitle();
	public void setTitle(final String title);

	public String getDescription();
	public void setDescription(final String description);

	public JsonType addType(final String name) throws URISyntaxException;
}
