package org.structr.common;

import java.util.Properties;

/**
 *
 * @author Christian Morgner
 */
public class StructrConf extends Properties {

	public StructrConf(final Properties defaults) {
		super(defaults);
	}
	
	public StructrConf() {
		super();
	}
	
	// TODO: implement addValue() and removeValue() methods
}
