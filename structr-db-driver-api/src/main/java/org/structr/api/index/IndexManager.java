/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.api.index;

/**
 *
 */
public interface IndexManager<T> {

	Index<T> fulltext();
	Index<T> exact();
	Index<T> spatial();
}
