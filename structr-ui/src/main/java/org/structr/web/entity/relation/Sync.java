package org.structr.web.entity.relation;

import org.structr.core.entity.ManyToMany;
import org.structr.web.entity.dom.DOMElement;

/**
 *
 * @author Christian Morgner
 */
public class Sync extends ManyToMany<DOMElement, DOMElement> {

	@Override
	public Class<DOMElement> getSourceType() {
		return DOMElement.class;
	}

	@Override
	public Class<DOMElement> getTargetType() {
		return DOMElement.class;
	}

	@Override
	public String name() {
		return "SYNC";
	}
}
