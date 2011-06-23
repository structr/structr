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
 * Notification interface (small information widgets in the admin UI).
 *
 * @author Christian Morgner
 */
public interface Notification {

	/**
	 * Returns the title of this notification.
	 *
	 * @return the title
	 */
	public String getTitle();

	/**
	 * Returns the text of this notification.
	 *
	 * @return the text
	 */
	public String getText();

	/**
	 * Whether this notification is expired. Notifications are removed from
	 * the NotificationService only after this method returns false.
	 *
	 * @return whether to discard this notification
	 */
	public boolean isExpired();
}
