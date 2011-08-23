/*
 *  Copyright (C) 2011 Axel Morgner
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

package org.structr.help;

import org.structr.common.AbstractComponent;

/**
 *
 * @author Christian Morgner
 */
public class HelpLink extends AbstractComponent {

	private String helpTarget = null;
	private String content = null;

	public HelpLink(String targetType) {

		this.helpTarget = targetType;
		this.content = targetType;
	}

	@Override
	public void initComponents() {
	}

	public void setHref(String href) {

		if(href != null) {

			StringBuilder buf = new StringBuilder();
			buf.append(href);
			buf.append("&amp;helpTarget=");
			buf.append(helpTarget);
			buf.append("#help-tab");

			addAttribute("href", buf.toString());
		}

		add(new Content(content));
	}
}
