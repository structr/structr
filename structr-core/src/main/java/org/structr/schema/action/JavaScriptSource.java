package org.structr.schema.action;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.Property;

/**
 *
 * @author Christian Morgner
 */
public interface JavaScriptSource extends NodeInterface {

	public static final Property<Boolean> useAsJavascriptLibrary = new BooleanProperty("useAsJavascriptLibrary").indexed();
	public static final View uiView = new View(JavaScriptSource.class, PropertyView.Ui, useAsJavascriptLibrary);

	public String getJavascriptLibraryCode();
	public String getContentType();
}
