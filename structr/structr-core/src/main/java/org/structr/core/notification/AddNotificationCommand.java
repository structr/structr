package org.structr.core.notification;

import javax.servlet.http.HttpSession;
import org.structr.common.CurrentSession;

/**
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

			HttpSession session = CurrentSession.getSession();
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
