package org.structr.web.common;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.core.property.StringProperty;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

/**
 *
 * @author Christian Morgner
 */
public class HtmlProperty extends StringProperty {
	
	public HtmlProperty(String name) {
		super(PropertyView.Html.concat(name));
	}
	
	@Override
	public String typeName() {
		return "String";
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}
}
