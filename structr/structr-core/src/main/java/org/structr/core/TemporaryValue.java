/*
 *  Copyright (C) 2011 Axel Morgner
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

package org.structr.core;

/**
 * Holds an arbitrary value and a timestamp that indicates the expiry
 * date of the value.
 *
 *
 * @author Christian Morgner
 */
public class TemporaryValue<T> {

	private long timeToLive = 0L;
	private long timestamp = 0L;
	private T value = null;

	public TemporaryValue(T value, long timeToLive) {

		this.timestamp = System.currentTimeMillis() + timeToLive;
		this.timeToLive = timeToLive;
		this.value = value;
	}

	public boolean isExpired() {
		return(System.currentTimeMillis() > timestamp);
	}

	public void refreshStoredValue(T value) {

		this.value = value;
		this.timestamp = System.currentTimeMillis() + timeToLive;
	}

	public long getTimeToLive() {

		return(timeToLive);
	}

	public long getCreateTimestamp() {

		return(timestamp - timeToLive);
	}

	public T getStoredValue() {

		if(isExpired()) {
			value = null;
		}
		
		return(value);
	}
}
