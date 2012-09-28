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
 *
 * @author Axel Morgner
 */
public class ReadLogCommand extends LogServiceCommand {

	private static final Logger logger = Logger.getLogger(ReadLogCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		TxPageFile logDb           = (TxPageFile) arguments.get("logDb");
		Map<String, Object> result = new HashMap<String, Object>();

		if (logDb != null) {

			Transaction tx                            = logDb.tx();
			MultiIndexFactory multiIndexFactory       = new MultiIndexFactory(tx);
			IndexFactory<String, Object> indexFactory = new BTreeIndexFactory<String, Object>();

			if (parameters.length == 1) {

				String key = (String) parameters[0];

				try {

					SortedIndex<String, Object> index  = (SortedIndex<String, Object>) multiIndexFactory.openOrCreate(key, indexFactory);
					Iterator<Entry<String, Object>> it = index.iterator();

					while (it.hasNext()) {

						Entry<String, Object> entry = it.next();

						result.put(entry.getKey(), entry.getValue());

					}

				} catch (org.fusesource.hawtdb.api.IndexException e) {

					logger.log(Level.WARNING, "Could not read log db page for key {0}", key);

				}

			}

		}

		return result;

	}

}
