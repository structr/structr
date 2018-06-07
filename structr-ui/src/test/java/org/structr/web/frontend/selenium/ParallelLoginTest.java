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
import org.structr.web.basic.FrontendTest;
import org.structr.web.frontend.selenium.SeleniumTest.SupportedBrowsers;


public class ParallelLoginTest extends FrontendTest {

	private static final Logger logger = LoggerFactory.getLogger(ParallelLoginTest.class.getName());

	@Test
	public void testParallelLogin() {

		createAdminUser();

		final int numberOfRequests        = 10000;
		final int numberOfParallelThreads = 8;
		final int waitForSec              = 30;

		final ExecutorService service = Executors.newFixedThreadPool(numberOfParallelThreads);
		final List<Future<Exception>> results = new ArrayList<>();

		final String menuEntry = "Pages";

		System.setProperty("webdriver.chrome.driver", SeleniumTest.getBrowserDriverLocation(SupportedBrowsers.CHROME));

		final ChromeOptions chromeOptions = new ChromeOptions();
		chromeOptions.setHeadless(true);



		for (int i = 0; i< numberOfRequests; i++) {

			Future<Exception> result = service.submit(() -> {

				//System.out.println(SimpleDateFormat.getDateInstance().format(new Date()) + " Login attempt from thread " + Thread.currentThread().toString());
				logger.info("Login attempt from thread " + Thread.currentThread().toString());

				WebDriver localDriver = new ChromeDriver(chromeOptions);

				try {
					long t0 = System.currentTimeMillis();


					// Wait for successful login
					SeleniumTest.loginAsAdmin(menuEntry, localDriver, waitForSec);

					long t1 = System.currentTimeMillis();

					logger.info("Successful login after " + (t1-t0) + " ms  with thread " +  Thread.currentThread().toString());

				} catch (Exception ex) {
					logger.error("Error in nested test in thread " +  Thread.currentThread().toString(), ex);
					return ex;
				} finally {
					localDriver.quit();
				}

				localDriver = null;

				return null;
			});

			results.add(result);
		}

		int r = 0;
		long t0 = System.currentTimeMillis();

		for (final Future<Exception> result : results) {

			try {

				long t1 = System.currentTimeMillis();
				Exception res = result.get();
				long t2 = System.currentTimeMillis();
				r++;

				logger.info(r + ": Got result from future after " + (t2-t1) + " ms");

				assertNull(res);


			} catch (final InterruptedException | ExecutionException ex) {
				logger.error("Error while checking result of nested test", ex);
			}
		}

		long t3 = System.currentTimeMillis();

		logger.info("Got all results within " + (t3-t0)/1000 + " s");

		service.shutdown();

		logger.info("Waiting " + waitForSec + " s to allow login processes to finish before stopping the instance");

		try {
			Thread.sleep(waitForSec * 1000);
		} catch (InterruptedException ex) {}
	}

}
