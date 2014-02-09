/**
 * Copyright (C) 2010-2014 Structr, c/o Morgner UG (haftungsbeschränkt) <structr@structr.org>
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity;

import org.structr.web.entity.dom.Page;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import org.apache.commons.lang.StringUtils;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMElement;

//~--- JDK imports ------------------------------------------------------------

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import org.structr.web.entity.dom.relationship.DOMChildren;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class SearchResultView extends View {

	private static final Logger logger  = Logger.getLogger(SearchResultView.class.getName());
	private DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	
	public static final org.structr.common.View uiView     = new org.structr.common.View(SearchResultView.class, PropertyView.Ui, type, name, query);
	public static final org.structr.common.View publicView = new org.structr.common.View(SearchResultView.class, PropertyView.Public, type, name, query);

	//~--- get methods ----------------------------------------------------

	@Override
	public void render(SecurityContext securityContext, RenderContext renderContext, int depth) throws FrameworkException {

		HttpServletRequest request   = renderContext.getRequest();
		Page page                    = renderContext.getPage();
		double startSearchResultView = System.nanoTime();
		String searchString          = (String) request.getParameter("search");

		if (StringUtils.isNotBlank(searchString)) {

			for (Page resultPage : getResultPages(securityContext, page)) {

				// recursively render children
				for (DOMChildren rel : resultPage.getChildRelationships()) {

					DOMElement subNode = (DOMElement) rel.getTargetNode();

					if (subNode.isNotDeleted() && subNode.isNotDeleted()) {

						subNode.render(securityContext, renderContext, depth + 1);
					}

				}
			}

		}

		double endSearchResultView = System.nanoTime();

		logger.log(Level.FINE, "Get graph objects for search {0} in {1} seconds", new java.lang.Object[] { searchString,
			decimalFormat.format((endSearchResultView - startSearchResultView) / 1000000000.0) });

	}

}
