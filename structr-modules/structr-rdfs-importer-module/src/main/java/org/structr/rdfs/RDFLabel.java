/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.rdfs;

import java.util.Collections;
import java.util.Set;
import org.w3c.dom.Element;


public class RDFLabel extends RDFItem<RDFLabel> {

	private String language = null;
	private String value    = null;

	public RDFLabel(final Element element) {

		super(element);

		this.language = getAttribute(element, "xml:lang");
		this.value    = getValue(element);
	}

	public String getLanguage() {

		if (language != null) {
			return language;
		}

		return "de";
	}

	public String getValue() {
		return value;
	}

	// ----- protected methods -----

	@Override
	protected Set<String> getInheritanceIdentifiers() {
		return Collections.emptySet();
	}
}
