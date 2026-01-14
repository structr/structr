/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.autocomplete.keywords;

import org.structr.autocomplete.KeywordHint;
import org.structr.docs.Example;

import java.util.List;

public class ApplicationRootPathHint extends KeywordHint {

	@Override
	public String getName() {
		return "applicationRootPath";
	}

	@Override
	public String getShortDescription() {
		return "Refers to the root path of the Structr application.";
	}

	@Override
	public String getLongDescription() {
		return "The root path of the Structr application can be configured using the `application.root.path` setting in structr.conf, in case Structr is being run behind a reverse proxy. The default value of the setting is the empty string, i.e. no additional root path.";
	}
	@Override
	public List<Example> getExamples() {
		return null;
	}

	@Override
	public List<String> getNotes() {
		return null;
	}
}
