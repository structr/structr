/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.node;

import java.util.Iterator;
import org.structr.core.Adapter;

/**
 *
 * @author Christian Morgner
 */
public class IterableAdapter<S, T> implements Iterable<T>
{
	Iterator<S> sourceIterator = null;
	Adapter<S, T> adapter = null;

	public IterableAdapter(Iterable<S> source, Adapter<S, T> adapter)
	{
		this.sourceIterator = source.iterator();
		this.adapter = adapter;
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
				return(adapter.adapt(sourceIterator.next()));
			}

			@Override
			public void remove()
			{
				sourceIterator.remove();
			}

		});
	}
}
