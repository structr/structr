/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common;

import java.util.Iterator;
import java.util.Properties;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 *
 *
 */
public class StructrConf extends Properties {

	public StructrConf(final Properties defaults) {
		super(defaults);
	}
	
	public StructrConf() {
		super();
	}
	
	public void load(final PropertiesConfiguration config) {
		
		final Iterator<String> keys = config.getKeys();
		
		while (keys.hasNext()) {
			final String key = keys.next();
			this.setProperty(key, config.getString(key));
		}
	}

	// TODO: implement addValue() and removeValue() methods
}
