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
 * Adds a notification to the global list of notifications when executed. The
 * notification will be visible for <b>all</b> users that are currently logged
 * in to the structr admin UI.
 *
 * @author Christian Morgner
 */
public class AddGlobalNotificationCommand extends NotificationServiceCommand {

	@Override
	public Object execute(Object... parameters)
	{
		Notification notification = null;

		switch(parameters.length) {

			case 1:
				if(parameters[0] instanceof Notification) {

					notification = (Notification)parameters[0];
				}
		}

		// if everything is ok, add notification
		if(notification != null) {

			NotificationService service = (NotificationService)getArgument("service");
			service.addNotification(null, notification);
		}

		return(null);
	}
}
