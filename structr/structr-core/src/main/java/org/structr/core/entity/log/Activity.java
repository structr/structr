/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.log;

import java.util.Date;
import org.structr.core.entity.StructrNode;

/**
 *
 * Activity is the base class for any logged activity. An activity has start
 * and end time, so we can log long-running activities as well.
 *
 * Subclass this class to log more specific activities.
 *
 * @author axel
 */
public class Activity extends StructrNode {

    private final static String ICON_SRC = "/images/application_link.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    public static final String TIMESTAMP_KEY = "timestamp";
    public static final String START_TIMESTAMP_KEY = "startTimestamp";
    public static final String END_TIMESTAMP_KEY = "endTimestamp";
    public static final String ACTIVITY_TEXT_KEY = "activityText";

    public Date getStartTimestamp() {
        return getDateProperty(START_TIMESTAMP_KEY);
    }

    public void setStartTimestamp(final Date timestamp) {
        setProperty(START_TIMESTAMP_KEY, timestamp);
    }

    public Date getEndTimestamp() {
        return getDateProperty(END_TIMESTAMP_KEY);
    }

    public void setEndTimestamp(final Date timestamp) {
        setProperty(END_TIMESTAMP_KEY, timestamp);
    }

    public String getActivityText() {
        return getStringProperty(ACTIVITY_TEXT_KEY);
    }

    public void setActivityText(final String text) {
        setProperty(ACTIVITY_TEXT_KEY, text);
    }
}
