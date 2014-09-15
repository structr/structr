/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud;

/**
 *
 * @author Christian Morgner
 */
public class ExportContext {

	private CloudListener listener = null;
	private int currentProgress    = 0;
	private int totalSize          = 0;

	public ExportContext(final CloudListener listener, final int totalSize) {
		this.listener  = listener;
		this.totalSize = totalSize;
	}

	public CloudListener getListener() {
		return listener;
	}

	public int getCurrentProgress() {
		return currentProgress;
	}

	public int getTotalSize() {
		return totalSize;
	}

	public void progress() {
		currentProgress++;

		if (listener != null) {
			listener.transmissionProgress(currentProgress, totalSize);
		}
	}

	public void increaseTotal(final int addTotal) {
		totalSize += addTotal;
	}

	public void transmissionStarted() {

		if (listener != null) {
			listener.transmissionStarted();
		}
	}

	public void transmissionFinished() {

		if (listener != null) {
			listener.transmissionFinished();
		}
	}

	public void transmissionAborted() {

		if (listener != null) {
			listener.transmissionAborted();
		}
	}
}
