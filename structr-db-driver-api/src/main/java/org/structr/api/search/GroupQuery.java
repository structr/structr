/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.api.search;

import java.util.List;

/**
 *
 */
public interface GroupQuery extends QueryPredicate {

	// nesting
	List<QueryPredicate> getQueryPredicates();
}
