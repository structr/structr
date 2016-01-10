/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.api.search;

/**
 *
 */
public interface QueryPredicate {

	/**
	 * The desired query type for this predicate. Return null here if the
	 * predicate should be ignored by the indexing system.
	 *
	 * @return the query type or null
	 */
	Class getQueryType();

	String getName();
	Class getType();
	Object getValue();

	Occurrence getOccurrence();
	boolean isExactMatch();

	String getSortKey();
	SortType getSortType();
	boolean sortDescending();
}
