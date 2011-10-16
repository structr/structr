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
package org.structr.core.entity.web;

import java.util.Map;
import org.structr.common.RenderMode;
import org.structr.core.NodeRenderer;
import org.structr.renderer.LogoutRenderer;

/**
 * Logs current user out
 *
 * @author axel
 */
public class Logout extends WebNode {

	/** Page to forward to after logout */
	public static final String FORWARD_PAGE_KEY = "forwardPage";

	public String getForwardPage() {
		return getStringProperty(FORWARD_PAGE_KEY);
	}

	public void setForwardPage(final String value) {
		setProperty(FORWARD_PAGE_KEY, value);
	}

	@Override
	public String getIconSrc() {
		return ("/images/door_out.png");
	}

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {
		renderers.put(RenderMode.Default, new LogoutRenderer());
	}
}
