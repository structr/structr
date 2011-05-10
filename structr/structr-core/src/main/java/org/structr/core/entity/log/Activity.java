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
package org.structr.core.entity.log;

import java.util.Date;
import java.util.Map;
import org.structr.core.entity.DefaultNode;
import org.structr.core.entity.User;

/**
 *
 * Activity is the base class for any logged activity. An activity has start
 * and end time, so we can log long-running activities as well.
 *
 * Subclass this class to log more specific activities.
 *
 * @author axel
 */
public class Activity extends DefaultNode {

    private final static String ICON_SRC = "/images/sport_soccer.png";

    public Activity() {
        super();
    }

    public Activity(final Map<String, Object> properties) {
        super(properties);
    }

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    public static final String SESSION_ID_KEY = "sessionId";
    public static final String START_TIMESTAMP_KEY = "startTimestamp";
    public static final String END_TIMESTAMP_KEY = "endTimestamp";
    public static final String ACTIVITY_TEXT_KEY = "activityText";

    private User user;

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

    public long getSessionId() {
        return getLongProperty(SESSION_ID_KEY);
    }

    public void setSessionId(final long id) {
        setProperty(SESSION_ID_KEY, id);
    }

    /**
     * User property for logging purposes only
     * 
     * @return
     */
    public User getUser() {
        return user;
    }

    /**
     * User property for logging purposes only
     */
    public void setUser(final User user) {
        this.user = user;
    }
}
