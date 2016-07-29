/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.rdfs;

import java.util.Collections;
import java.util.Set;
import org.w3c.dom.Element;

/**
 *
 *
 */
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
