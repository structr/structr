package org.structr.web.entity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.web.Importer;
import org.structr.web.common.ThreadLocalMatcher;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

/**
 *
 * @author Christian Morgner
 */
public class Widget extends AbstractNode {

	private static final ThreadLocalMatcher threadLocalTemplateMatcher = new ThreadLocalMatcher("\\[[a-zA-Z]+\\]");
	public static final Property<String> source                        = new StringProperty("source");
	
	public static final org.structr.common.View uiView = new org.structr.common.View(User.class, PropertyView.Ui,
		type, name, source
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(User.class, PropertyView.Public,
		type, name, source
	);
	
	public static void expandWidget(SecurityContext securityContext, Page page, DOMNode parent, Map<String, Object> parameters) throws FrameworkException {
	
		String _source          = (String)parameters.get("source");
		ErrorBuffer errorBuffer = new ErrorBuffer();
		
		if (_source == null) {
			
			errorBuffer.add(Widget.class.getSimpleName(), new EmptyPropertyToken(source));
			
		} else {
	
			// check source for mandatory parameters
			Matcher matcher  = threadLocalTemplateMatcher.get();
			Set<String> keys = new HashSet<>();

			// initialize with source
			matcher.reset(_source);

			while (matcher.find()) {

				String group  = matcher.group();
				String key    = group.substring(1, group.length() - 1);
				Object value  = parameters.get(key);
				
				if (value == null) {
					
					errorBuffer.add(Widget.class.getSimpleName(), new EmptyPropertyToken(new StringProperty(key)));
					
				} else {
					
					// replace and restart matching process
					_source = _source.replaceAll(group, value.toString());
					matcher.reset(_source);
				}
				
			}
			
		}
		
		if (!errorBuffer.hasError()) {

			Importer importer = new Importer(securityContext, _source, null, null, 1, true, true);

			importer.parse();
			importer.createChildNodes(parent, page);
			
		} else {
			
			// report error to ui
			throw new FrameworkException(422, errorBuffer);
		}
	}
}
