/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.rdfs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 *
 */
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
