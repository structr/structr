package org.structr.core.notification;

/**
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
