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

package org.structr.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;

/**
 * Converts an Iterable of source type S to an Iterable of target type T by
 * passing each element through and {@see org.structr.core.Adapter}. This class
 * implements lazy evaluation in the sense that a call to {@see #next} causes
 * the next element of the source Iterable to be converted and returned.
 *
 * @author Christian Morgner
 */
public class IterableAdapter<S, T> implements Iterable<T>
{
	private static final Logger logger = Logger.getLogger(IterableAdapter.class.getName());
	private Iterator<S> sourceIterator = null;
	private Adapter<S, T> adapter = null;
	private int size = -1;

	public IterableAdapter(Iterable<S> source, Adapter<S, T> adapter)
	{
		this.sourceIterator = source.iterator();
		this.adapter = adapter;
		
		// try to obtain size in advance
		if (source instanceof Collection) {
			size = ((Collection)source).size();
		}
	}

	@Override
	public Iterator<T> iterator()
	{
		return(new Iterator<T>()
		{
			@Override
			public boolean hasNext()
			{
				return(sourceIterator.hasNext());
			}

			@Override
			public T next()
			{
				try {
					return(adapter.adapt(sourceIterator.next()));
				} catch(FrameworkException fex) {
					logger.log(Level.WARNING, "Error in iterable adapter", fex);
				}
				return null;
			}

			@Override
			public void remove()
			{
				sourceIterator.remove();
			}

		});
	}
	
	public int size() {
		return size;
	}
}
