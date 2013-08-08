package org.structr.web.entity;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.web.Importer;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

/**
 *
 * @author Christian Morgner
 */
public class Widget extends AbstractNode {

	public static final Property<String> source = new StringProperty("source");
	
	public static final org.structr.common.View uiView = new org.structr.common.View(User.class, PropertyView.Ui,
		type, name, source
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(User.class, PropertyView.Public,
		type, name, source
	);
	
	public void expandWidget(SecurityContext securityContext, Page page, DOMNode parent) throws FrameworkException {
		new Importer(securityContext, getProperty(source), null, null, 1, true, true).createChildNodes(parent, page);
	}
}
