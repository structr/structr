package org.structr.dynamic;

import org.structr.web.entity.FileBase;
import org.structr.schema.SchemaService;

/**
 *
 * @author Christian Morgner
 */
public class File extends FileBase {

	// register this type as an overridden builtin type
	static {

		SchemaService.registerBuiltinTypeOverride("File", FileBase.class.getName());
	}
}
