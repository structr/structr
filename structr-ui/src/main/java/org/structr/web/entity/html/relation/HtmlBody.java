package org.structr.web.entity.html.relation;

import org.structr.core.entity.OneToOne;
import org.structr.web.entity.html.Body;
import org.structr.web.entity.html.Html;

/**
 *
 * @author Christian Morgner
 */
public class HtmlBody extends OneToOne<Html, Body> {

	@Override
	public Class<Html> getSourceType() {
		return Html.class;
	}

	@Override
	public Class<Body> getTargetType() {
		return Body.class;
	}

	@Override
	public String name() {
		return "CONTAINS";
	}
}
