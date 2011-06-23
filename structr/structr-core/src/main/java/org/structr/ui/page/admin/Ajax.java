package org.structr.ui.page.admin;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.click.ActionResult;
import org.structr.core.Services;
import org.structr.core.notification.GetNotificationsCommand;
import org.structr.core.notification.Notification;
import org.structr.ui.page.StructrPage;

/**
 *
 * @author Christian Morgner
 */
public class Ajax extends StructrPage
{
	public ActionResult onUpdateConsoleContent()
	{
		StringBuilder ret = new StringBuilder(200);
		int zIndex = 999;

		Collection<Notification> notifications = (Collection<Notification>)Services.command(GetNotificationsCommand.class).execute(getContext().getSession().getId());

		ret.append("<div id=\"consoleContent\">");

		if(notifications != null) {

			for(Notification notification : notifications)
			{
				String notificationTitle = notification.getTitle();
				String notificationText = notification.getText();

				ret.append("<div class='notification' style='z-index:");
				ret.append(zIndex--);
				ret.append(";'>");

				if(notificationTitle != null && notificationTitle.length() > 0) {

					ret.append("<h3 class='notificationTitle'>");
					ret.append(notificationTitle);
					ret.append("</h3>");
				}

				if(notificationText != null && notificationText.length() > 0) {

					ret.append("<p class='notificationText'>");
					ret.append(notification.getText());
					ret.append("</p>");
				}

				ret.append("</div>");
			}

		}

		ret.append("</div>");

		// Return an action result containing the message
		return new ActionResult(ret.toString(), ActionResult.HTML);
	}
}
