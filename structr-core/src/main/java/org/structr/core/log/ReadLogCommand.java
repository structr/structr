/**
 * Copyright (C) 2010-2014 Structr, c/o Morgner UG (haftungsbeschränkt) <structr@structr.org>
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
package org.structr.core.log;

import java.util.HashMap;
import org.fusesource.hawtdb.api.BTreeIndexFactory;
import org.fusesource.hawtdb.api.IndexFactory;
import org.fusesource.hawtdb.api.MultiIndexFactory;
import org.fusesource.hawtdb.api.SortedIndex;
import org.fusesource.hawtdb.api.Transaction;
import org.fusesource.hawtdb.api.TxPageFile;

import org.structr.common.error.FrameworkException;

//~--- JDK imports ------------------------------------------------------------

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Returns the values previously stored under the given key with the
 * {@link WriteLogCommand}.
 *
 * @author Axel Morgner
 */
public class ReadLogCommand extends LogServiceCommand {

	private static final Logger logger = Logger.getLogger(ReadLogCommand.class.getName());

	//~--- methods --------------------------------------------------------

	public Map<String, Object> execute(String key) throws FrameworkException {

		TxPageFile logDb           = (TxPageFile) arguments.get("logDb");
		Map<String, Object> result = new HashMap<>();

		if (logDb != null) {

			Transaction tx                            = logDb.tx();
			MultiIndexFactory multiIndexFactory       = new MultiIndexFactory(tx);
			IndexFactory<String, Object> indexFactory = new BTreeIndexFactory<>();

			try {

				SortedIndex<String, Object> index  = (SortedIndex<String, Object>) multiIndexFactory.openOrCreate(key, indexFactory);
				Iterator<Entry<String, Object>> it = index.iterator();

				while (it.hasNext()) {

					Entry<String, Object> entry = it.next();

					result.put(entry.getKey(), entry.getValue());

				}

			} catch (Throwable t) {

				t.printStackTrace();
				
				logger.log(Level.WARNING, "Could not read log db page for key {0}", key);

			}
		}

		return result;
	}

}
