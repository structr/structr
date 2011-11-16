/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.ui.page.admin;

import org.apache.click.control.Panel;
import org.apache.click.util.ClickUtils;

import org.structr.common.StructrOutputStream;
import org.structr.core.entity.web.Page;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import org.apache.click.Context;

//~--- classes ----------------------------------------------------------------

/**
 * Edit a page
 *
 * @author amorgner
 */
public class EditPage extends DefaultEdit {

	private final static String PREVIEW_PARAMETER_PREFIX = "___";

	//~--- fields ---------------------------------------------------------

	protected Page page;

	//~--- constructors ---------------------------------------------------

	public EditPage() {
		super();
	}

	//~--- methods --------------------------------------------------------

	@Override
	public void onInit() {

		super.onInit();

		if (node instanceof Page) {

			Context context = getContext();
			page            = (Page) node;
			externalViewUrl = page.getNodeURL(context.getRequest(), contextPath);
			HttpServletRequest request = context.getRequest();

			// localViewUrl = getContext().getResponse().encodeURL(viewLink.getHref());
			localViewUrl = request.getContextPath().concat("/view".concat(page.getNodePath().replace("&", "%26"))).concat(previewParameters(request));

			// render node's default view
			StructrOutputStream out = new StructrOutputStream(context.getRequest(), securityContext);

			node.renderNode(out, page, null, null);
			rendition = out.toString();

			// provide rendition's source
			source         = ClickUtils.escapeHtml(rendition);
			renditionPanel = new Panel("renditionPanel", "/panel/rendition-panel.htm");
		}
	}

	/**
	 * Append any preview parameters found in the current request.
	 *
	 * Preview parameters are the URL parameters of the page which is
	 * currently displayed in the preview rendition iframe.
	 *
	 * When clicking on a link in the preview iframe, or submitting a form,
	 * the backend URL will change accordingly. This method helps to take
	 * the request parameters into account, too.
	 *
	 * @param url
	 * @return
	 */
	private String previewParameters(HttpServletRequest request) {

		StringBuilder ret                = new StringBuilder();
		Map<String, Object> parameterMap = request.getParameterMap();

		for (String name : parameterMap.keySet()) {

			if (name.startsWith(PREVIEW_PARAMETER_PREFIX)) {

				String newName = name.substring(PREVIEW_PARAMETER_PREFIX.length());

				ret.append((ret.length() > 0)
					   ? "&"
					   : "?").append(newName).append("=").append(request.getParameter(name));
			}
		}

		return ret.toString();
	}
}
