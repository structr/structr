package org.structr.web.entity.html.relation;

import org.structr.core.entity.OneToOne;
import org.structr.web.entity.html.Head;
import org.structr.web.entity.html.Html;

/**
 *
 * @author Christian Morgner
 */
public class HtmlHead extends OneToOne<Html, Head> {

	@Override
	public Class<Html> getSourceType() {
		return Html.class;
	}

	@Override
	public Class<Head> getTargetType() {
		return Head.class;
	}

	@Override
	public String name() {
		return "CONTAINS";
	}
}
