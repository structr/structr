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
package org.structr.web.basic;

import java.util.Date;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Base class for selenium tests
 */

public class SeleniumTest extends FrontendTest {

	protected WebDriver driver;
	protected enum SupportedBrowsers { FIREFOX, CHROME, NONE };

	protected static SupportedBrowsers activeBrowser = SupportedBrowsers.FIREFOX;

	@Before
	public void startDriver() {

		switch (activeBrowser) {

			case FIREFOX :

				System.setProperty("webdriver.gecko.driver", getBrowserDriverLocation(activeBrowser));

				final FirefoxOptions firefoxOptions = new FirefoxOptions();
				firefoxOptions.setHeadless(true);

				driver = new FirefoxDriver(firefoxOptions);

				break;

			case CHROME :

				System.setProperty("webdriver.chrome.driver", getBrowserDriverLocation(activeBrowser));
				System.setProperty("webdriver.chrome.logfile", "/tmp/chromedriver.log");
				System.setProperty("webdriver.chrome.verboseLogging", "true");

				final ChromeOptions chromeOptions = new ChromeOptions();
				chromeOptions.setHeadless(true);

				driver = new ChromeDriver(chromeOptions);

				break;

			case NONE :

				// Don't create a driver in main thread, useful for parallel testing
				break;
		}
	}

	@After
	public void stopDriver() {

		driver.quit();
	}

	protected void logBrowserLog() {
		final LogEntries logEntries = driver.manage().logs().get(LogType.DRIVER);
		for (final LogEntry logEntry : logEntries) {
		    System.out.println(new Date(logEntry.getTimestamp()) + " " + logEntry.getLevel() + " " + logEntry.getMessage());
		}
	}

	/**
	 * Login into the backend UI as admin/admin using the given web driver and switch to the given menu entry.
	 *
	 * @param menuEntry
	 * @param driver
	 * @param waitForSeconds
	 */
	protected void loginAsAdmin(final String menuEntry, final WebDriver driver, final int waitForSeconds) {

		createAdminUser();

		driver.get("http://localhost:8875/structr/#" + menuEntry.toLowerCase());

		final WebDriverWait wait = new WebDriverWait(driver, waitForSeconds);
		//wait.until((ExpectedCondition<Boolean>) (final WebDriver f) -> (Boolean) ((JavascriptExecutor) f).executeScript("LSWrapper.isLoaded()"));

		assertEquals("Username field not found", 1, driver.findElements(By.id("usernameField")).size());
		assertEquals("Password field not found", 1, driver.findElements(By.id("passwordField")).size());
		assertEquals("Login button not found",   1, driver.findElements(By.id("loginButton")).size());

		final WebElement usernameField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("usernameField")));
		final WebElement passwordField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("passwordField")));
		final WebElement loginButton   = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loginButton")));

		usernameField.sendKeys("admin");
		passwordField.sendKeys("admin");
		loginButton.click();

		wait.until(ExpectedConditions.titleIs("Structr " + menuEntry));
		assertEquals("Structr " + menuEntry, driver.getTitle());
	}

	/**
	 * Login into the backend UI as admin/admin and switch to the given menu entry.
	 *
	 * @param menuEntry
	 */
	protected void loginAsAdmin(final String menuEntry) {
		loginAsAdmin(menuEntry, driver, 5);
	}

	public  String getBrowserDriverLocation (SupportedBrowsers BROWSER) {

		final String osName = System.getProperty("os.name").toLowerCase();
		final String driverRootPath = "src/test/selenium" + ((osName.contains("mac")) ? "_mac/" : "/");

		switch (BROWSER) {

			case FIREFOX :
				return driverRootPath + "geckodriver";

			case CHROME :
				return driverRootPath + "chromedriver";
		}

		return null;
	}
}
