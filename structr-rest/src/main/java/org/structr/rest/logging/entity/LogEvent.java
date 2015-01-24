package org.structr.rest.logging.entity;

import java.util.Date;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public class LogEvent extends AbstractNode {

	public static final Property<String> hashProperty      = new StringProperty("hash").indexed();

	public static final Property<String> path              = new StringProperty("path");
	public static final Property<String> messageProperty   = new StringProperty("message");

	public static final Property<String> actionProperty    = new StringProperty("action").indexed();
	public static final Property<Date>   timestampProperty = new ISO8601DateProperty("timestamp").indexed();
	public static final Property<String> subject           = new StringProperty("subject").indexed();
	public static final Property<String> object            = new StringProperty("object").indexed();

	public static final View defaultView = new View(LogEvent.class, PropertyView.Public,
		path, actionProperty, messageProperty, timestampProperty, subject, object
	);

	public long getTimestamp() {

		final Date date = getProperty(LogEvent.timestampProperty);
		if (date != null) {

			return date.getTime();
		}

		return 0L;
	}

	public String getAction() {
		return getProperty(LogEvent.actionProperty);
	}

	public String getMessage() {
		return getProperty(LogEvent.messageProperty);
	}

	public String getSubjectId() {
		return getProperty(LogEvent.subject);
	}

	public String getObjectId() {
		return getProperty(LogEvent.object);
	}

	public static String getContentHash(final String action, final String subjectId, final String objectId, final long timestamp) throws FrameworkException {

		String contentHash  = action;
		contentHash        += subjectId;
		contentHash        += objectId;
		contentHash        += Long.toString(timestamp);

		return contentHash;
	}
}
