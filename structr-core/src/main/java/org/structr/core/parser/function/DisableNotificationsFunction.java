package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class DisableNotificationsFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_DISABLE_NOTIFICATIONS    = "Usage: ${disable_notifications()}";
	public static final String ERROR_MESSAGE_DISABLE_NOTIFICATIONS_JS = "Usage: ${Structr.disableNotifications()}";

	@Override
	public String getName() {
		return "disable_notifications()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		ctx.getSecurityContext().setDoTransactionNotifications(false);

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_DISABLE_NOTIFICATIONS_JS : ERROR_MESSAGE_DISABLE_NOTIFICATIONS);
	}

	@Override
	public String shortDescription() {
		return "Disables the Websocket notifications in the Structr Ui for the current transaction";
	}

}
