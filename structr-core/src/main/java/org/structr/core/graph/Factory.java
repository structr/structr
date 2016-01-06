/**
 * Copyright (C) 2010-2015 Structr GmbH
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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.function.Function;
import org.neo4j.graphdb.Relationship;

import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.PagingIterator;
import org.structr.common.FactoryDefinition;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.IdNotFoundToken;
import org.structr.core.Adapter;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;
import org.structr.schema.SchemaHelper;

public abstract class Factory<S, T extends GraphObject> implements Adapter<S, T>, Function<S, T> {

	private static final Logger logger = Logger.getLogger(Factory.class.getName());
	public static final ExecutorService service = Executors.newCachedThreadPool();
	public static final int DEFAULT_PAGE_SIZE = Integer.MAX_VALUE;
	public static final int DEFAULT_PAGE      = 1;

	/**
	 * This limit is the number of objects up to which the overall count
	 * will be accurate.
	 */
	public static final int RESULT_COUNT_ACCURATE_LIMIT	= 5000;

	// encapsulates all criteria for node creation
	protected FactoryDefinition factoryDefinition = StructrApp.getConfiguration().getFactoryDefinition();
	protected FactoryProfile factoryProfile       = null;

	public Factory(final SecurityContext securityContext) {

		factoryProfile = new FactoryProfile(securityContext);
	}

	public Factory(final SecurityContext securityContext, final boolean includeDeletedAndHidden, final boolean publicOnly) {

		factoryProfile = new FactoryProfile(securityContext, includeDeletedAndHidden, publicOnly);
	}

	public Factory(final SecurityContext securityContext, final int pageSize, final int page, final String offsetId) {

		factoryProfile = new FactoryProfile(securityContext);

		factoryProfile.setPageSize(pageSize);
		factoryProfile.setPage(page);
		factoryProfile.setOffsetId(offsetId);
	}

	public Factory(final SecurityContext securityContext, final boolean includeDeletedAndHidden, final boolean publicOnly, final int pageSize, final int page, final String offsetId) {
		factoryProfile = new FactoryProfile(securityContext, includeDeletedAndHidden, publicOnly, pageSize, page, offsetId);
	}

	public abstract T instantiate(final S obj);
	public abstract T instantiate(final S obj, final Relationship pathSegment);
	public abstract T instantiateWithType(final S obj, final Class<T> type, final Relationship pathSegment, boolean isCreation) throws FrameworkException;
	public abstract T instantiate(final S obj, final boolean includeDeletedAndHidden, final boolean publicOnly) throws FrameworkException;
	public abstract T instantiateDummy(final S entity, final String entityType) throws FrameworkException;

	/**
	 * Create structr nodes from all given underlying database nodes
	 * No paging, but security check
	 *
	 * @param input
	 * @return result
	 * @throws org.structr.common.error.FrameworkException
	 */
	public Result instantiateAll(final Iterable<S> input) throws FrameworkException {

		List<T> objects = bulkInstantiate(input);

		return new Result(objects, objects.size(), true, false);
	}

	/**
	 * Create structr nodes from the underlying database nodes
	 *
	 * Include only nodes which are readable in the given security context.
	 * If includeDeletedAndHidden is true, include nodes with 'deleted' flag
	 * If publicOnly is true, filter by 'visibleToPublicUsers' flag
	 *
	 * @param input
	 * @return result
	 * @throws org.structr.common.error.FrameworkException
	 */
	public Result instantiate(final IndexHits<S> input) throws FrameworkException {


		if (input != null) {

			try (final IndexHits<S> closeable = input) {

				if (factoryProfile.getOffsetId() != null) {

					return resultWithOffsetId(closeable);

				} else {

					return resultWithoutOffsetId(closeable);
				}

			}
		}

		return Result.EMPTY_RESULT;

	}

	/**
	 * Create structr nodes from all given underlying database nodes
	 * No paging, but security check
	 *
	 * @param input
	 * @return nodes
	 * @throws org.structr.common.error.FrameworkException
	 */
	public List<T> bulkInstantiate(final Iterable<S> input) throws FrameworkException {

		List<T> nodes = new LinkedList<>();

		if ((input != null) && input.iterator().hasNext()) {

			for (S node : input) {

				T n = instantiate(node);
				if (n != null) {

					nodes.add(n);
				}
			}
		}

		return nodes;
	}

	@Override
	public T adapt(S s) {
		return instantiate(s);
	}

	@Override
	public T apply(final S from) {
		return adapt(from);
	}

	protected Class<T> getClassForName(final String rawType) {
		return SchemaHelper.getEntityClassForRawType(rawType);
	}

	// <editor-fold defaultstate="collapsed" desc="private methods">
	protected List<S> read(final Iterable<S> it) {

		List<S> nodes = new LinkedList();

		while (it.iterator().hasNext()) {

			nodes.add(it.iterator().next());
		}

		return nodes;

	}

	protected Result resultWithOffsetId(final IndexHits<S> input) throws FrameworkException {

		int size                 = input.size();
		final int pageSize       = Math.min(size, factoryProfile.getPageSize());
		final int page           = factoryProfile.getPage();
		final String offsetId    = factoryProfile.getOffsetId();
		List<T> elements         = new LinkedList<>();
		int position             = 0;
		int count                = 0;
		int offset               = 0;

		try (final IndexHits<S> closeable = input) {

			// We have an offsetId, so first we need to
			// find the node with this uuid to get the offset
			List<T> nodesUpToOffset = new LinkedList();
			int i                   = 0;
			boolean gotOffset        = false;

			for (S node : closeable) {

				T n = instantiate(node);

				if (n == null) {

					continue;

				}

				nodesUpToOffset.add(n);

				if (!gotOffset) {

					if (!offsetId.equals(n.getUuid())) {

						i++;

						continue;

					}

					gotOffset = true;
					offset    = page > 0
						    ? i
						    : i + (page * pageSize);

					break;

				}

			}

			if (!nodesUpToOffset.isEmpty() && !gotOffset) {

				throw new FrameworkException(404, new IdNotFoundToken("offsetId", offsetId));
			}

			if (offset < 0) {

				// Remove last item
				nodesUpToOffset.remove(nodesUpToOffset.size()-1);

				return new Result(nodesUpToOffset, size, true, false);
			}

			for (T node : nodesUpToOffset) {

				if (node != null) {

					if (++position > offset) {

						// stop if we got enough nodes
						if (++count > pageSize) {

							return new Result(elements, size, true, false);
						}

						elements.add(node);
					}

				}

			}

			// If we get here, the result was not complete, so we need to iterate
			// through the index result (input) to get more items.
			for (S node : closeable) {

				T n = instantiate(node);
				if (n != null) {

					if (++position > offset) {

						// stop if we got enough nodes
						if (++count > pageSize) {

							return new Result(elements, size, true, false);
						}

						elements.add(n);
					}

				}

			}
		}

		return new Result(elements, size, true, false);

	}

	protected Result resultWithoutOffsetId(final IndexHits<S> input) throws FrameworkException {

		final int pageSize = factoryProfile.getPageSize();
		final int page     = factoryProfile.getPage();
		int fromIndex;

		if (page < 0) {

			final List<S> rawNodes = read(input);
			final int size         = rawNodes.size();

			fromIndex = Math.max(0, size + (page * pageSize));

			final List<T> nodes = new LinkedList<>();
			int toIndex         = Math.min(size, fromIndex + pageSize);

			for (final S n : rawNodes.subList(fromIndex, toIndex)) {

				nodes.add(instantiate(n));
			}

			// We've run completely through the iterator,
			// so the overall count from here is accurate.
			return new Result(nodes, size, true, false);

		} else {

			// FIXME: IndexHits#size() may be inaccurate!
			final int size = input.size();

			final SecurityContext securityContext = factoryProfile.getSecurityContext();

			// In case of superuser or in public context, don't check the overall result count
			// (SearchCommand adds visibleToPublicUsers: true in case of an anonymous user)
			final boolean dontCheckCount  = securityContext.isSuperUser() || securityContext.getUser(false) == null;
			if (dontCheckCount){

				fromIndex = pageSize == Integer.MAX_VALUE ? 0 : (page - 1);//* pageSize;

			} else {

				fromIndex = pageSize == Integer.MAX_VALUE ? 0 : (page - 1) * pageSize;
			}

			// The overall count may be inaccurate
			return page(input, size, fromIndex, pageSize, dontCheckCount);
		}
	}

	protected Result page(final IndexHits<S> input, final int overallResultCount, final int offset, final int pageSize, boolean dontCheckCount) throws FrameworkException {

		final AtomicBoolean keepRunning    = new AtomicBoolean(true);
		final AtomicInteger overallCount   = new AtomicInteger();
		final AtomicInteger processedItems = new AtomicInteger();

		if (dontCheckCount){

			overallCount.set(input.size());

			final PagingIterator<S> neoResult = new PagingIterator<>(input.iterator(), pageSize) ;
			final List<T> nodes               = new LinkedList<>();

			try {
				neoResult.page(offset);

			} catch (NoSuchElementException nex) {

				// do not throw an exception when a page beyond the
				// number of elements is returned,
				// return empty result instead
				return new Result(Collections.emptyList(), overallCount.get(), true, false);
			}

			Iterator<S> resultPage = neoResult.nextPage();

			while (resultPage.hasNext()) {

				final S s = resultPage.next();
				final T t = (T) instantiate(s);

				if(t != null){

					nodes.add(t);
				}
			}

			// We've run completely through the iterator,
			// so the overall count from here is accurate.
			return new Result(nodes, overallCount.get(), true, false);


		} else {

			final List<Item<T>> nodes = new LinkedList<>();

			try (final IndexHits<S> closeable = input) {

				final SecurityContext securityContext      = factoryProfile.getSecurityContext();
				final boolean preventFullCount             = securityContext.hasParameter("ignoreResultCount");
				final ConcurrentLinkedQueue<Item<S>> queue = new ConcurrentLinkedQueue<>();
				final List<Future> futures                 = new LinkedList<>();
				int threadCount                            = 1;
				int rawCount                               = 0;

				// fill queue with data and count elements
				for (final S item : closeable) {
					queue.add(new Item<>(rawCount++, item));
				}

				if (rawCount < 100) {

					// do not use multithreading
					final InstantiationWorker worker = new InstantiationWorker(securityContext, queue, nodes, offset, pageSize, dontCheckCount || preventFullCount);
					worker.setProcessedItems(processedItems);
					worker.setOverallCount(overallCount);
					worker.setKeepRunning(keepRunning);

					worker.doRun();

				} else {


					final double t0 = System.nanoTime();
					threadCount     = 8;

					// submit workers, use multithreading
					for (int i=0; i<threadCount; i++) {

						final InstantiationWorker worker = new InstantiationWorker(securityContext, queue, nodes, offset, pageSize, dontCheckCount || preventFullCount);
						worker.setProcessedItems(processedItems);
						worker.setOverallCount(overallCount);
						worker.setKeepRunning(keepRunning);

						// first worker logs
						worker.pleaseLog(i == 0);

						futures.add(service.submit(worker));
					}

					// wait for result..
					for (final Future future : futures) {

						try {

							future.get();

						} catch (InterruptedException | ExecutionException iex) {
							iex.printStackTrace();
						}
					}

					final double t1 = System.nanoTime();
					if (t1-t0 > 1000000000) {
						logger.log(Level.INFO, "Instantiated {0} out of {1} elements in {2} s using {3} threads.", new Object[] { nodes.size(), rawCount, (t1-t0) / 1000000000.0, threadCount } );
					}
				}

			}

			// keep initial sort order
			Collections.sort(nodes);

			final int size = nodes.size();
			final int from = Math.min(offset, size);
			final int to   = Math.min(offset+pageSize, size);
			final List<T> output = new LinkedList<>();

			for (final Item<T> item : nodes.subList(from, to)) {
				output.add(item.item);
			}

			// The overall count may be inaccurate
			return new Result(output, overallCount.get(), true, false);
		}
	}

	//~--- inner classes --------------------------------------------------

	private class InstantiationWorker implements Runnable {

		private final SecurityContext securityContext;
		private final Queue<Item<S>> source;
		private final List<Item<T>> nodes;

		private AtomicInteger processedItems = null;
		private AtomicInteger overallCount   = null;
		private AtomicBoolean keepRunning    = null;
		private boolean dontCheckCount       = false;
		private boolean doLogOutput          = false;
		private int pageSize                 = 0;
		private int offset                   = 0;

		public InstantiationWorker(final SecurityContext securityContext, final Queue<Item<S>> source, final List<Item<T>> nodes, final int offset, final int pageSize, final boolean dontCheckCount) {

			this.securityContext = securityContext;
			this.offset          = offset;
			this.source          = source;
			this.dontCheckCount  = dontCheckCount;
			this.pageSize        = pageSize;
			this.nodes           = nodes;
		}

		@Override
		public void run() {

			try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

				// transaction is only needed if we are running multiple threads
				doRun();

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}
		}

		private void doRun() {

			final long t0 = System.currentTimeMillis();
			long t1       = t0;
			Item<S> item;

			do {

				item = source.poll();
				if (item != null) {

					processedItems.incrementAndGet();

					T n = instantiate(item.item);
					if (n != null) {

						overallCount.incrementAndGet();

						// synchronize access to the target list
						synchronized (nodes) {
							nodes.add(new Item<>(item.index, n));
						}

						// stop evaluation of new nodes if count is not required
						if (dontCheckCount && overallCount.get() > offset + pageSize) {
							keepRunning.set(false);
						}
					}
				}

				// log output if desired
				if (doLogOutput && System.currentTimeMillis() - t1 > 2000) {

					t1 = System.currentTimeMillis();
					logger.log(Level.INFO, "Parallel instantiation: checked {0} nodes so far", processedItems.get());
				}

			} while (item != null && keepRunning.get());
		}

		public void setKeepRunning(final AtomicBoolean keepRunning) {
			this.keepRunning = keepRunning;
		}

		public void setProcessedItems(AtomicInteger processedItems) {
			this.processedItems = processedItems;
		}

		public void setOverallCount(final AtomicInteger overallCount) {
			this.overallCount = overallCount;
		}

		public void pleaseLog(final boolean doLogOutput) {
			this.doLogOutput = doLogOutput;
		}
	}

	private class Item<X> implements Comparable<Item<X>> {

		public int index = 0;
		public X item    = null;

		public Item(final int index, final X item) {
			this.index = index;
			this.item  = item;
		}

		@Override
		public int compareTo(final Item<X> o) {
			return Integer.valueOf(index).compareTo(o.index);
		}
	}

	protected class FactoryProfile {

		private boolean includeDeletedAndHidden = true;
		private String offsetId                 = null;
		private boolean publicOnly              = false;
		private int pageSize                    = DEFAULT_PAGE_SIZE;
		private int page                        = DEFAULT_PAGE;
		private SecurityContext securityContext = null;

		//~--- constructors -------------------------------------------

		public FactoryProfile(final SecurityContext securityContext) {

			this.securityContext = securityContext;

		}

		public FactoryProfile(final SecurityContext securityContext, final boolean includeDeletedAndHidden, final boolean publicOnly) {

			this.securityContext         = securityContext;
			this.includeDeletedAndHidden = includeDeletedAndHidden;
			this.publicOnly              = publicOnly;

		}

		public FactoryProfile(final SecurityContext securityContext, final boolean includeDeletedAndHidden, final boolean publicOnly, final int pageSize, final int page,
				      final String offsetId) {

			this.securityContext         = securityContext;
			this.includeDeletedAndHidden = includeDeletedAndHidden;
			this.publicOnly              = publicOnly;
			this.pageSize                = pageSize;
			this.page                    = page;
			this.offsetId                = offsetId;

		}

		//~--- methods ------------------------------------------------

		/**
		 * @return the includeDeletedAndHidden
		 */
		public boolean includeDeletedAndHidden() {

			return includeDeletedAndHidden;

		}

		/**
		 * @return the publicOnly
		 */
		public boolean publicOnly() {

			return publicOnly;

		}

		//~--- get methods --------------------------------------------

		/**
		 * @return the offsetId
		 */
		public String getOffsetId() {

			return offsetId;

		}

		/**
		 * @return the pageSize
		 */
		public int getPageSize() {

			return pageSize;

		}

		/**
		 * @return the page
		 */
		public int getPage() {

			return page;

		}

		/**
		 * @return the securityContext
		 */
		public SecurityContext getSecurityContext() {

			return securityContext;

		}

		//~--- set methods --------------------------------------------

		/**
		 * @param includeDeletedAndHidden the includeDeletedAndHidden to set
		 */
		public void setIncludeDeletedAndHidden(boolean includeDeletedAndHidden) {

			this.includeDeletedAndHidden = includeDeletedAndHidden;

		}

		/**
		 * @param offsetId the offsetId to set
		 */
		public void setOffsetId(String offsetId) {

			this.offsetId = offsetId;

		}

		/**
		 * @param publicOnly the publicOnly to set
		 */
		public void setPublicOnly(boolean publicOnly) {

			this.publicOnly = publicOnly;

		}

		/**
		 * @param pageSize the pageSize to set
		 */
		public void setPageSize(int pageSize) {

			this.pageSize = pageSize;

		}

		/**
		 * @param page the page to set
		 */
		public void setPage(int page) {

			this.page = page;

		}

		/**
		 * @param securityContext the securityContext to set
		 */
		public void setSecurityContext(SecurityContext securityContext) {

			this.securityContext = securityContext;

		}

	}

	// </editor-fold>
}
