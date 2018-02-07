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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class Choice extends RDFItem<Choice> {

	private final Map<String, String> translations = new LinkedHashMap<>();
	private String identifier                      = null;
	private int position                           = 0;

	public Choice(final String originalString, final String identifier) {
		this(originalString, identifier, 0);
	}

	public Choice(final String originalString, final String identifier, final int position) {

		super(null);

		this.identifier = identifier;
		this.position   = position;

		// original string is german translation
		translations.put("de", originalString);
	}

	public Map<String, String> getTranslations() {
		return translations;
	}

	public void setTranslations(final NodeList nodeList) {

		if (nodeList != null) {

			for (int i=0; i<nodeList.getLength(); i++) {

				final Element node    = (Element)nodeList.item(i);
				final String language = getAttribute(node, "xml:lang");
				final String value    = getValue(node);

				translations.put(language, value);
			}
		}
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	@Override
	protected Set<String> getInheritanceIdentifiers() {
		return Collections.emptySet();
	}
}
