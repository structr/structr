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

import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.web.basic.FrontendTest;

/**
 * Base class for selenium tests
 */

public class SeleniumTest extends FrontendTest {

	protected enum SupportedBrowsers { FIREFOX, CHROME, NONE };

	protected static SupportedBrowsers activeBrowser = SupportedBrowsers.FIREFOX;

	protected WebDriver webDriver;
	protected WebDriverWait driver;
	protected Actions actions;

	@Before
	public void startDriver() {

		switch (activeBrowser) {

			case FIREFOX :

				System.setProperty("webdriver.gecko.driver", getBrowserDriverLocation(activeBrowser));

				final FirefoxOptions firefoxOptions = new FirefoxOptions();
				firefoxOptions.setHeadless(true);

				webDriver = new FirefoxDriver(firefoxOptions);

				break;

			case CHROME :

				System.setProperty("webdriver.chrome.driver", getBrowserDriverLocation(activeBrowser));
				System.setProperty("webdriver.chrome.logfile", "/tmp/chromedriver.log");
				System.setProperty("webdriver.chrome.verboseLogging", "true");

				final ChromeOptions chromeOptions = new ChromeOptions();
				chromeOptions.setHeadless(true);

				webDriver = new ChromeDriver(chromeOptions);

				break;

			case NONE :

				// Don't create a driver in main thread, useful for parallel testing
				break;
		}

		webDriver.manage().window().setSize(new Dimension(1280, 1024));
		webDriver.get("http://localhost:8875/structr/#");

		driver  = new WebDriverWait(webDriver, 2);
		actions = new Actions(webDriver);

		createAdminUser();
		delay(1000);
	}

	@After
	public void stopDriver() {

		if (webDriver != null) {
			webDriver.quit();
		}
	}

	protected void logBrowserLog() {
		final LogEntries logEntries = webDriver.manage().logs().get(LogType.DRIVER);
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
	protected static void loginAsAdmin(final String menuEntry, final WebDriver driver, final int waitForSeconds) {

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

		try {

			wait.until(ExpectedConditions.titleIs("Structr " + menuEntry));

		} catch (WebDriverException wex) {

			try {
				Runtime.getRuntime().exec("/usr/bin/jstack -l $(ps xa|grep structr|grep java|tail -n1|awk '{print $1}') > /tmp/jstack.out." + new Date().getTime());

			} catch (IOException ex1) {
				throw new RuntimeException(ex1);
			}

		} catch (RuntimeException ex) {

			throw ex;
		}

		assertEquals("Structr " + menuEntry, driver.getTitle());
	}

	/**
	 * Login into the backend UI as admin/admin and switch to the given menu entry.
	 *
	 * @param menuEntry
	 */
	protected void loginAsAdmin(final String menuEntry) {
		createAdminUser();
		loginAsAdmin(menuEntry, webDriver, 5);
	}

	public static String getBrowserDriverLocation (SupportedBrowsers BROWSER) {

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

	protected WebElement id(final String id) {
		return driver.until(ExpectedConditions.visibilityOfElementLocated(By.id(id)));
	}

	protected WebElement xpath(final String path) {
		return driver.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(path)));
	}

	protected WebElement tagName(final String name) {
		return driver.until(ExpectedConditions.visibilityOfElementLocated(By.tagName(name)));
	}

	protected WebElement className(final String name) {
		return driver.until(ExpectedConditions.visibilityOfElementLocated(By.className(name)));
	}

	protected WebElement selector(final String selector) {
		return driver.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(selector)));
	}

	protected void delay() {
		delay(100);
	}

	protected void delay(final long milliseconds) {
		try { Thread.sleep(milliseconds); } catch (InterruptedException iex) {}
	}

	protected void takeScreenshot(final String path) throws IOException {
		final File scrFile = ((TakesScreenshot)webDriver).getScreenshotAs(OutputType.FILE);
		FileUtils.copyFile(scrFile, new File(path));
	}

	// interaction
	protected void hover(final WebElement element) {
		actions.moveToElement(element, 0, 0).build().perform();
		delay();
	}

	protected void dragAndDrop(final WebElement source, final int dx, final int dy) {

		actions.moveToElement(source, 0, 0).build().perform();
		delay();

		actions.clickAndHold().build().perform();
		delay();

		actions.moveByOffset(dx, dy).build().perform();
		delay();

		actions.release().build().perform();
		delay();
	}

	protected void input(final WebElement element, final String text) {

		element.click();
		element.clear();;
		element.sendKeys(text);
	}

	// more complex composite actions
	protected void login(final String username, final String password) {

		// login
		input(id("usernameField"), username);
		input(id("passwordField"), password);
		id("loginButton").click();

		delay();
	}

	protected void logout() {

		// logout
		hover(className("submenu-trigger"));
		id("logout_").click();

		delay(1000);
	}

	protected void area(final String which) {

		id(which + "_").click();

		delay();
	}

	protected String getUuidOfFirstElement(final String typeName) {

		final Class type = StructrApp.getConfiguration().getNodeEntityClass(typeName);
		String uuid      = null;

		try (final Tx tx = app.tx()) {

			final GraphObject node = app.nodeQuery(type).getFirst();
			if (node != null) {

				uuid = node.getUuid();
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return uuid;
	}

	protected int getCountOfType(final String typeName) {

		final Class type = StructrApp.getConfiguration().getNodeEntityClass(typeName);
		int count        = 0;

		try (final Tx tx = app.tx()) {

			count = app.nodeQuery(type).getAsList().size();

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return count;
	}
}
