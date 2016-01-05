package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class EnableNotificationsFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_ENABLE_NOTIFICATIONS    = "Usage: ${enable_notifications()}";
	public static final String ERROR_MESSAGE_ENABLE_NOTIFICATIONS_JS = "Usage: ${Structr.enableNotifications()}";

	@Override
	public String getName() {
		return "enable_notifications()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		ctx.getSecurityContext().setDoTransactionNotifications(true);

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_ENABLE_NOTIFICATIONS_JS : ERROR_MESSAGE_ENABLE_NOTIFICATIONS);
	}

	@Override
	public String shortDescription() {
		return "Enables the Websocket notifications in the Structr Ui for the current transaction";
	}

}
