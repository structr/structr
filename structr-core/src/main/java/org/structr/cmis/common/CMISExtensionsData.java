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
package org.structr.cmis.common;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;

/**
 *
 *
 */
public class CMISExtensionsData implements ExtensionsData {

	private static final Logger logger = Logger.getLogger(CMISExtensionsData.class.getName());

	protected List<CmisExtensionElement> extensions = new LinkedList<>();

	@Override
	public List<CmisExtensionElement> getExtensions() {
		return extensions;
	}

	@Override
	public void setExtensions(final List<CmisExtensionElement> extensions) {
		logger.log(Level.INFO, "{0}", extensions);
		this.extensions.addAll(extensions);
	}
}
