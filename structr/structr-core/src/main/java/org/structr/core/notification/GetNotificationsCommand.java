package org.structr.core.notification;

import java.util.List;

/**
 *
 * @author Christian Morgner
 */
public class GetNotificationsCommand extends NotificationServiceCommand {

	@Override
	public Object execute(Object... parameters)
	{
		NotificationService service = (NotificationService)getArgument("service");
		List<Notification> ret = null;

		if(parameters.length == 1 && service != null && parameters[0] instanceof String) {

			String sessionId = (String)parameters[0];
			ret = service.getNotifications(sessionId);
		}

		return(ret);
	}
}
