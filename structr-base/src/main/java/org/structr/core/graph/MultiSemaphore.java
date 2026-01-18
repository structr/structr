/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 *
 */
public class MultiSemaphore {

	private static final Logger logger = LoggerFactory.getLogger(MultiSemaphore.class.getName());

	private final Map<String, Semaphore> semaphoreMap = new ConcurrentHashMap<>();

	public synchronized void acquire(final Set<String> types) throws InterruptedException {

		if (types != null && !types.isEmpty()) {

			for (Semaphore semaphore : getSemaphores(types)) {
				semaphore.acquire();
			}
		}
	}

	public void release(final Set<String> types) {

		if (types != null && !types.isEmpty()) {

			for (Semaphore semaphore : getSemaphores(types)) {
				semaphore.release();
			}
		}
	}

	private Set<Semaphore> getSemaphores(final Set<String> types) {

		Set<Semaphore> semaphores = new LinkedHashSet<>();

		if (types != null) {

			for (String type : types) {

				if (type != null) {

					Semaphore semaphore = semaphoreMap.get(type);
					if (semaphore == null) {

						semaphore = new Semaphore(1, true);
						semaphoreMap.put(type, semaphore);
					}

					semaphores.add(semaphore);
				}
			}
		}

		return semaphores;
	}
}
