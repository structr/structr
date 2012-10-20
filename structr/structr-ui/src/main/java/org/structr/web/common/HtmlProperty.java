package org.structr.web.common;

import org.structr.common.Property;
import org.structr.common.PropertyView;

/**
 *
 * @author Christian Morgner
 */
public class HtmlProperty extends Property<String> {
	
	public HtmlProperty(String name) {
		super(PropertyView.Html.concat(name));
	}
}
