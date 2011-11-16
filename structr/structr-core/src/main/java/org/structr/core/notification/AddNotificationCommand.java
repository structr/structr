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

import javax.servlet.http.HttpSession;

/**
 * Adds a notification to the list of notificatons for the current session when
 * executed. This command takes one or two parameters, the first is the
 * notification to be added, the second is optional and specifies the session
 * ID for the session in which the notification should be displayed. If no
 * session ID is given to this command, the command first tries to obtain a
 * session ID from the {@see org.structr.core.common.CurrentRequest} object
 * (this works when the command is executed in the same thread as the original
 * HttpServletRequest). If no session was found, the command uses the <b>name of
 * the current thread</b> for its session ID key. (<i>If you execute this command
 * from a separate thread, make sure the name of the newly created thread
 * is the same as the session ID of the target session for this
 * notification.</i>)
 *
 * @author Christian Morgner
 */
public class AddNotificationCommand extends NotificationServiceCommand {

	@Override
	public Object execute(Object... parameters)
	{
		Notification notification = null;
		String sessionKey = null;

		switch(parameters.length) {

			case 2:
				if(parameters[1] instanceof String) {

					sessionKey = (String)parameters[1];
				}

			case 1:
				if(parameters[0] instanceof Notification) {

					notification = (Notification)parameters[0];
				}
		}

		// if session was not sent, try to
		// use the session from this thread
		if(sessionKey == null) {

			HttpSession session = securityContext.getSession();
			if(session != null) {
				
				sessionKey = session.getId();

			} else
			{
				// take name of current thread as session key,
				sessionKey = Thread.currentThread().getName();
			}
		}

		// if everything is ok, add notification
		if(notification != null && sessionKey != null) {

			NotificationService service = (NotificationService)getArgument("service");
			service.addNotification(sessionKey, notification);
		}

		return(null);
	}
}
