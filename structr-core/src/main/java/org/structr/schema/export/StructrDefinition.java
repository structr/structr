package org.structr.schema.export;

/**
 *
 * @author Christian Morgner
 */


public interface StructrDefinition {

	public StructrDefinition resolveJsonPointerKey(final String key);
}
