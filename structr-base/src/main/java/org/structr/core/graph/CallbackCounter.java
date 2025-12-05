/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.core.graph;

import org.slf4j.Logger;
import org.structr.api.config.Settings;

/**
 * A counter for detailed callback logging
 */
public class CallbackCounter {

	private static final int LOGGING_THRESHOLD = Settings.CallbackLoggingThreshold.getValue(50000);

	private Logger logger        = null;
	private String threadName    = null;
	private long transactionId   = 0L;
	private int onCreate         = 0;
	private int onSave           = 0;
	private int onDelete         = 0;
	private int afterCreate      = 0;
	private int afterSave        = 0;
	private int afterDelete      = 0;
	private int indexing         = 0;
	private int sum              = 0;

	public CallbackCounter(final Logger logger, final long transactionId, final String threadName) {

		this.logger        = logger;
		this.transactionId = transactionId;
		this.threadName    = threadName;
	}

	public void onCreate() {
		onCreate++;
		count();
	}

	public void onSave() {
		onSave++;
		count();
	}

	public void onDelete() {
		onDelete++;
		count();
	}

	public void afterCreate() {
		afterCreate++;
		count();
	}

	public void afterSave() {
		afterSave++;
		count();
	}

	public void afterDelete() {
		afterDelete++;
		count();
	}

	public void indexing() {
		indexing++;
		count();
	}

	// ------ private methods -----
	private void count() {

		if ((++sum % LOGGING_THRESHOLD) == 0) {

			logger.warn(
				  "Number of callbacks in transaction {} by thread {} has exceeded logging threshold: onCreate/onSave/onDelete: {}/{}/{}, "
				+ "afterCreate/afterSave/afterDelete: {}/{}/{}, indexing: {}. This is a warning that the transaction size might be too large, "
				+ "or that an unusual number of callbacks are executed in this transaction. To disable this warning message, increase the "
				+ "value of {} in structr.conf.",
				transactionId, threadName,
				onCreate, onSave, onDelete,
				afterCreate, afterSave, afterDelete,
				indexing,
				Settings.CallbackLoggingThreshold.getKey()
			);
		}
	}
}
