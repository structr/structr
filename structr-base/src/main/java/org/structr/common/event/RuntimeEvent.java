/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common.event;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.StringProperty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 */
public class RuntimeEvent {

	private static final Logger logger = LoggerFactory.getLogger(RuntimeEvent.class);
	private static final AtomicLong ID_SOURCE = new AtomicLong();

	private static final LongProperty _id                = new LongProperty("id");
	private static final StringProperty _type            = new StringProperty("type");
	private static final GenericProperty _data           = new GenericProperty("data");
	private static final BooleanProperty _seen           = new BooleanProperty("seen");
	private static final StringProperty _description     = new StringProperty("description");
	private static final LongProperty _absoluteTimestamp = new LongProperty("absoluteTimestamp");
	private static final LongProperty _relativeTimestamp = new LongProperty("relativeTimestamp");

	private final long id                  = ID_SOURCE.getAndIncrement();
	private final long absoluteTimestamp   = System.currentTimeMillis();
	private final long relativeTimestamp   = System.nanoTime();
	private final Map<String, Object> data = new LinkedHashMap<>();
	private boolean seen                   = false;
	private String description             = null;
	private String type                    = null;

	public RuntimeEvent(final String type, final String description, final Map<String, Object> data) {

		this.type        = type;
		this.description = description;

		this.data.putAll(data);
	}

	public long getId() {
		return id;
	}

	public long absoluteTimestamp() {
		return absoluteTimestamp;
	}
	public long relativeTimestamp() {
		return relativeTimestamp;
	}

	public String getType() {
		return type;
	}

	public String getDescription() {
		return description;
	}

	public Map<String, Object> getData() {
		return data;
	}

	public boolean getSeen() {
		return seen;
	}


	public GraphObject toGraphObject() {

		final GraphObject result = new GraphObjectMap();

		try {

			result.setProperty(_id,                id);
			result.setProperty(_type,              type);
			result.setProperty(_absoluteTimestamp, absoluteTimestamp);
			result.setProperty(_relativeTimestamp, relativeTimestamp);
			result.setProperty(_description,       description);
			result.setProperty(_seen,              seen);
			result.setProperty(_data,              data);

		} catch (Throwable t) {
			logger.error(ExceptionUtils.getStackTrace(t));
		}

		return result;
	}

	public void acknowledge() {
		this.seen = true;
	}

}
