/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.ThreadLocalMatcher;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.ValueToken;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.ConstantBooleanProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.schema.SchemaService;
import org.structr.web.importer.Importer;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.relation.ImageWidget;
import org.structr.web.property.UiNotion;

/**
 *
 *
 */
public class Widget extends AbstractNode implements Taggable {

	private static final ThreadLocalMatcher threadLocalTemplateMatcher = new ThreadLocalMatcher("\\[[a-zA-Z:,]+\\]");

	public static final Property<String>      source      = new StringProperty("source").cmis();
	public static final Property<String>      description = new StringProperty("description").cmis();
	public static final Property<String>      treePath    = new StringProperty("treePath").cmis().indexed();
	public static final Property<List<Image>> pictures    = new EndNodes<>("pictures", ImageWidget.class, new UiNotion());
	public static final Property<Boolean>     isWidget    = new ConstantBooleanProperty("isWidget", true);

	public static final org.structr.common.View uiView = new org.structr.common.View(Widget.class, PropertyView.Ui,
		type, name, source, description, pictures, tags, treePath, isWidget
	);

	public static final org.structr.common.View publicView = new org.structr.common.View(Widget.class, PropertyView.Public,
		type, name, source, description, pictures, tags, treePath, isWidget
	);

	// register this type as an overridden builtin type
	static {
		SchemaService.registerBuiltinTypeOverride("Widget", Widget.class.getName());
	}

	public static void expandWidget(SecurityContext securityContext, Page page, DOMNode parent, String baseUrl, Map<String, Object> parameters) throws FrameworkException {

		String _source          = (String)parameters.get("source");
		ErrorBuffer errorBuffer = new ErrorBuffer();

		if (_source == null) {

			errorBuffer.add(new EmptyPropertyToken(Widget.class.getSimpleName(), source));

		} else {

			// check source for mandatory parameters
			Matcher matcher  = threadLocalTemplateMatcher.get();

			// initialize with source
			matcher.reset(_source);

			while (matcher.find()) {

				String group              = matcher.group();
				String source             = group.substring(1, group.length() - 1);
				ReplacementInfo info      = new ReplacementInfo(source);
				ArrayList<String> options = info.getOptions();
				String key                = info.getKey();

				Object value  = parameters.get(key);

				if (value == null) {

					if (!options.isEmpty()) {

						errorBuffer.add(new ValueToken(Widget.class.getSimpleName(), new StringProperty(key), options.toArray()));

					} else {

						errorBuffer.add(new EmptyPropertyToken(Widget.class.getSimpleName(), new StringProperty(key)));
					}

				} else {

					// replace and restart matching process
					_source = _source.replace(group, value.toString());
					matcher.reset(_source);
				}

			}

		}

		if (!errorBuffer.hasError()) {

			Importer importer = new Importer(securityContext, _source, baseUrl, null, false, false);

			importer.parse(true);
			importer.createChildNodes(parent, page, true);

		} else {

			// report error to ui
			throw new FrameworkException(422, "Unable to import the given source code", errorBuffer);
		}
	}

	/**
	 * Unused yet
	 */
	public static class ReplacementInfo {

		private ArrayList<String> options = new ArrayList<>();
		private String key                = null;
		private boolean hasOptions        = false;

		public ReplacementInfo(final String value) {

			this.key = value;

			if (value.contains(":")) {

				final String[] parts = value.split("[:]+");
				this.key = parts[0];

				if (parts[1].contains(",")) {

					final String[] opts = parts[1].split("[,]+");
					final int count     = opts.length;

					for (int i=0; i<count; i++) {

						final String trimmedPart = opts[i].trim();
						if (!trimmedPart.isEmpty()) {

							options.add(trimmedPart);
						}
					}

					hasOptions = true;
				}
			}
		}

		public String getKey() {
			return key;
		}

		public ArrayList<String> getOptions() {
			return options;
		}

		public boolean hasOptions() {
			return hasOptions;
		}
	}
}
