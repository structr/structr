/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity;

import org.structr.common.SecurityContext;
import org.structr.common.ThreadLocalMatcher;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.importer.Importer;
import org.structr.web.maintenance.deploy.DeploymentCommentHandler;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;

public interface Widget extends NodeInterface {

	ThreadLocalMatcher threadLocalTemplateMatcher = new ThreadLocalMatcher("\\[[^\\]]+\\]");

	String getSource();
	String getDescription();
	boolean isWidget();
	String getTreePath();
	String getConfiguration();
	boolean isPageTemplate();
	String[] getSelectors();

	static void expandWidget(final SecurityContext securityContext, final Page page, final DOMNode parent, final String baseUrl, final Map<String, Object> parameters, final boolean processDeploymentInfo) throws FrameworkException {

		String _source = (String) parameters.get("source");
		ErrorBuffer errorBuffer = new ErrorBuffer();

		if (_source == null) {

			errorBuffer.add(new EmptyPropertyToken(Widget.class.getSimpleName(), "source"));

		} else {

			// check source for mandatory parameters
			Matcher matcher = threadLocalTemplateMatcher.get();

			// initialize with source
			matcher.reset(_source);

			while (matcher.find()) {

				final String group         = matcher.group();
				final String source        = group.substring(1, group.length() - 1);
				final ReplacementInfo info = new ReplacementInfo(source);
				final String key           = info.getKey();
				final Object value         = parameters.get(key);

				if (value != null) {

					// replace and restart matching process
					_source = _source.replace(group, value.toString());
					matcher.reset(_source);
				}
			}
		}

		if (!errorBuffer.hasError()) {

			Importer importer = new Importer(securityContext, _source, baseUrl, null, false, false, false, false);

			if (processDeploymentInfo) {
				importer.setIsDeployment(true);
				importer.setCommentHandler(new DeploymentCommentHandler());
			}

			// test: insert widget into Page object directly
			if (parent.equals(page)) {
				importer.setIsDeployment(true);
			}

			//importer.setIsWidgetImport(true);

			if (importer.parse(true)) {

				importer.createChildNodes(parent, page, true);

				final String tableChildElement = importer.getTableChildElement();
				if (tableChildElement != null) {

					for (final NodeInterface child : parent.getAllChildNodes()) {

						if (child.getType().toLowerCase().equals(tableChildElement)) {

							parent.appendChild(child.as(DOMNode.class));

						} else {
							StructrApp.getInstance().delete(child);
						}
					}
				}
			}

		} else {

			// report error to ui
			throw new FrameworkException(422, "Unable to import the given source code", errorBuffer);
		}
	}

	class ReplacementInfo {

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
