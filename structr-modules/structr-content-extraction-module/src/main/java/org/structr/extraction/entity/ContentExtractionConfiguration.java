/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.extraction.entity;

import org.structr.core.graph.NodeInterface;
import org.structr.web.entity.AbstractFile;

import java.util.Map;

/**
 * Configuration of automatic content extraction, attached to one Folder (for its whole subtree) or one
 * File.
 *
 * The nearest configuration on a file's path applies completely; options it leaves empty come from the
 * contentextraction.* settings, not from configurations further up.
 */
public interface ContentExtractionConfiguration extends NodeInterface {

	/**
	 * The File or Folder this configuration applies to.
	 */
	AbstractFile getTarget();

	/**
	 * Whether files below the target are extracted automatically.
	 */
	boolean isEnabled();

	/**
	 * The options this configuration sets, by option name (as accepted by processContent()). Options
	 * left empty are not contained.
	 */
	Map<String, Object> getOptionValues();
}
