/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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



package org.structr.core.log;

import org.fusesource.hawtdb.api.BTreeIndexFactory;
import org.fusesource.hawtdb.api.Transaction;
import org.fusesource.hawtdb.api.TxPageFile;

import org.structr.common.error.FrameworkException;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.fusesource.hawtdb.api.SortedIndex;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class WriteLogCommand extends LogServiceCommand {

	private static final Logger logger = Logger.getLogger(WriteLogCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		TxPageFile logDb = (TxPageFile) arguments.get("logDb");

		if (logDb != null) {

			Transaction tx                                 = logDb.tx();
			BTreeIndexFactory<String, String> indexFactory = new BTreeIndexFactory<String, String>();
			SortedIndex<String, String> index              = indexFactory.openOrCreate(tx);

			if (parameters.length == 1) {

				String timestamp   = String.valueOf(System.nanoTime());
				String value = parameters[0].toString();

				index.put(timestamp, value);
				logger.log(Level.INFO, "Logged '{0}':'{1}' to log database", new Object[] { timestamp, value });

			} else {
				throw new IllegalArgumentException();
			}

			tx.commit();

		}

		return null;

	}

}
