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

import org.structr.core.Command;
import org.structr.core.SingletonService;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

//~--- classes ----------------------------------------------------------------

/**
 * The structr notification service. This service provides commands to get and
 * set notifications in the structr admin UI. Notifications are small
 * information widgets that are displayed in the lower right corner of the
 * admin UI.
 *
 * <p>This service provides the following commands:</p>
 * <ul>
 *  <li>{@see org.structr.core.notification.AddNotificationCommand}</li>
 *  <li>{@see org.structr.core.notification.GetNotificationsCommand}</li>
 *  <li>{@see org.structr.core.notification.AddGlobalNotificationCommand}</li>
 * </ul>
 *
 * @author Christian Morgner
 */
public class NotificationService implements SingletonService {

	public static final Map<String, Queue<Notification>> globalNotificationMap =
		Collections.synchronizedMap(new HashMap<String, Queue<Notification>>());
	public static final Queue<Notification> globalNotifications = new ConcurrentLinkedQueue<Notification>();

	//~--- methods --------------------------------------------------------

	public boolean addNotification(Notification notification) {
		return (addNotification(null, notification));
	}

	public boolean addNotification(String sessionId, Notification notification) {

		if (sessionId != null) {

			Queue<Notification> notifications = getNotificationsInternal(sessionId);
			boolean ret                       = false;

			if (notifications == null) {

				notifications = new ConcurrentLinkedQueue<Notification>();

				synchronized (globalNotificationMap) {
					globalNotificationMap.put(sessionId, notifications);
				}
			}

			synchronized (notifications) {

				// store in list
				ret = notifications.add(notification);
			}

			return (ret);

		} else {

			globalNotifications.add(notification);

			return (true);
		}
	}

	// ----- interface SingletonService -----
	@Override
	public void injectArguments(Command command) {
		command.setArgument("service", this);
	}

	@Override
	public void initialize(Map<String, String> context) {}

	@Override
	public void shutdown() {}

	//~--- get methods ----------------------------------------------------

	public List<Notification> getNotifications(String sessionId) {

		List<Notification> ret            = new LinkedList<Notification>();
		Queue<Notification> notifications = getNotificationsInternal(sessionId);

		if (notifications != null) {
			ret.addAll(notifications);
		}

		getGlobalNotifications();

		if (globalNotifications != null) {
			ret.addAll(globalNotifications);
		}

		return (ret);
	}

	private Queue<Notification> getNotificationsInternal(String sessionId) {

		Queue<Notification> ret = null;

		synchronized (globalNotificationMap) {

			ret = globalNotificationMap.get(sessionId);

			if (ret != null) {

				Notification head = ret.peek();

				if ((head != null) && head.isExpired()) {
					ret.remove();
				}
			}
		}

		return (ret);
	}

	private Queue<Notification> getGlobalNotifications() {

		Notification head = globalNotifications.peek();

		if ((head != null) && head.isExpired()) {
			globalNotifications.remove();
		}

		return (globalNotifications);
	}

	@Override
	public String getName() {
		return (this.getClass().getSimpleName());
	}

	@Override
	public boolean isRunning() {
		return (true);
	}
}
