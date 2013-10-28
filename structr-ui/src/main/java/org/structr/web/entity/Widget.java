/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.web.entity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import org.neo4j.graphdb.Direction;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.web.Importer;
import org.structr.web.common.RelType;
import org.structr.web.common.ThreadLocalMatcher;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.property.UiNotion;

/**
 *
 * @author Christian Morgner
 */
public class Widget extends AbstractNode implements Taggable {

	private static final ThreadLocalMatcher		threadLocalTemplateMatcher	= new ThreadLocalMatcher("\\[[a-zA-Z]+\\]");
	public static final Property<String>		source				= new StringProperty("source");
	public static final Property<String>		description			= new StringProperty("description");
	public static final EndNodes<Image>	pictures				= new EndNodes<>("pictures", Image.class, RelType.PICTURE_OF, Direction.INCOMING, new UiNotion(), true);
	
	public static final org.structr.common.View uiView = new org.structr.common.View(User.class, PropertyView.Ui,
		type, name, source, description, pictures, tags
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(User.class, PropertyView.Public,
		type, name, source, description, pictures, tags
	);
	
	public static void expandWidget(SecurityContext securityContext, Page page, DOMNode parent, String baseUrl, Map<String, Object> parameters) throws FrameworkException {
	
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
					_source = _source.replace(group, value.toString());
					matcher.reset(_source);
				}
				
			}
			
		}
		
		if (!errorBuffer.hasError()) {

			Importer importer = new Importer(securityContext, _source, baseUrl, null, 1, true, true);

			importer.parse(true);
			importer.createChildNodes(parent, page, baseUrl);
			
		} else {
			
			// report error to ui
			throw new FrameworkException(422, errorBuffer);
		}
	}
}
