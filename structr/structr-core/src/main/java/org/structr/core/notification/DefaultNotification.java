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
package org.structr.core.notification;

/**
 * A notification that displays a title and a text, and stays visible for
 * 3 seconds. The delay can be configured via the {@see #setLifespan} method.
 *
 * @author Christian Morgner
 */
public class DefaultNotification implements Notification {

    protected long creationTime = 0L;
    protected long lifespan = 0L;
    protected String title = null;
    protected String text = null;

    /**
     * Constructs a new instance of this notification with the given title
     * and text.
     *
     * @param title the title to display
     * @param text the text to display
     */
    public DefaultNotification(String title, String text) {
	this(title, text, 3000);
    }

    /**
     * Constructs a new instance of this notification with the given title,
     * text and lifespan.
     *
     * @param title the title to display
     * @param text the text to display
     * @param lifespan the lifespan in milliseconds
     */
    public DefaultNotification(String title, String text, long lifespan) {
	this.creationTime = System.currentTimeMillis();
	this.lifespan = lifespan;

	this.title = title;
	this.text = text;
    }

    @Override
    public String getTitle() {
	return (title);
    }

    @Override
    public String getText() {
	return (text);
    }

    @Override
    public String getContainerCss() {
	return("notification");
    }

    @Override
    public String getTitleCss() {
	return("notificationTitle");
    }

    @Override
    public String getTextCss() {
	return("notificationText");
    }

    /**
     * Sets the lifespan of this notification to the given value (ms).
     *
     * @param lifespan the lifespan in milliseconds
     */
    public void setLifespan(long lifespan) {
	this.lifespan = lifespan;
    }

    @Override
    public boolean isExpired() {
	return (System.currentTimeMillis() > this.creationTime + lifespan);
    }
}
