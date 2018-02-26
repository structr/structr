/**
 * Copyright (C) 2010-2018 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.frontend.selenium;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.web.basic.SeleniumTest;

/**
 * Parallel login login tests
 */
public class ParallelLoginTest extends SeleniumTest {

	private static final Logger logger = LoggerFactory.getLogger(ParallelLoginTest.class.getName());

	static {
		activeBrowser = SupportedBrowsers.NONE;
	}

	@Test
	public void testParallelLogin() {

		// Wait for the backend to finish initialization
		try {
			Thread.sleep(10000L);
		} catch (InterruptedException ex) {}

		createAdminUser();

		final int numberOfRequests        = 1000;
		final int numberOfParallelThreads = 8;
		final int waitForSec              = 60;

		final ExecutorService service = Executors.newFixedThreadPool(numberOfParallelThreads);
		final List<Future<Exception>> results = new ArrayList<>();

		for (int i = 0; i< numberOfRequests; i++) {

			Future<Exception> result = service.submit(() -> {

				//System.out.println(SimpleDateFormat.getDateInstance().format(new Date()) + " Login attempt from thread " + Thread.currentThread().toString());
				logger.info("Login attempt from thread " + Thread.currentThread().toString());

				final String menuEntry = "Pages";

				System.setProperty("webdriver.chrome.driver", getBrowserDriverLocation(SupportedBrowsers.CHROME));

				final ChromeOptions chromeOptions = new ChromeOptions();
				chromeOptions.setHeadless(true);
				WebDriver driver = new ChromeDriver(chromeOptions);

				try {
					long t0 = System.currentTimeMillis();
					
					// Wait for successful login
					loginAsAdmin(menuEntry, driver, waitForSec);
					
					long t1 = System.currentTimeMillis();
					
					logger.info("Successful login after " + (t1-t0) + " ms  with thread " +  Thread.currentThread().toString());
					
				} catch (Exception ex) {
					logger.error("Error in nested test in thread " +  Thread.currentThread().toString(), ex);
					return ex;
				} finally {
					driver.quit();
				}

				return null;
			});

			results.add(result);
		}

		int r = 0;
		
		for (final Future<Exception> result : results) {

			try {
				
				long t0 = System.currentTimeMillis();
				Exception res = result.get();
				long t1 = System.currentTimeMillis();
				r++;
				if (res != null) {
					
					logger.error(r + ": Got " + res + " from future after " + (t1-t0) + " ms");
					
					assertNull(result.get());
					
					break;
				}
				
			} catch (final InterruptedException | ExecutionException ex) {
				logger.error("Error while checking result of nested test", ex);
			}
		}

		service.shutdown();

		try {
			// Typically, one login is done in under about 2 seconds, so we assume non-parallel execution and add the waiting time
			Thread.sleep(1000L);
			//Thread.sleep((numberOfRequests * 2000) + (waitForSec*1000));
		} catch (InterruptedException ex) {}

	}

}
