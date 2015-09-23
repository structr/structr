package org.structr.common;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.Iterables;

/**
 *
 * @author Christian Morgner
 */
public class IdSorter<T extends Relationship> implements Iterable<T>, Comparator<T> {

	private List<T> sortedList = null;

	public IdSorter(final Iterable<T> source) {
		this.sortedList = Iterables.toList(source);
		Collections.sort(sortedList, this);
	}

	@Override
	public Iterator<T> iterator() {
		return sortedList.iterator();
	}

	@Override
	public int compare(T o1, T o2) {

		final long id1 = o1.getId();
		final long id2 = o2.getId();

		return id1 < id2 ? -1 : id1 > id2 ? 1 : 0;
	}
}
