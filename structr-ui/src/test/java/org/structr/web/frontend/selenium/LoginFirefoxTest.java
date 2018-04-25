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

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test basic backend login as admin/admin with Firefox
 */
public class LoginFirefoxTest extends SeleniumTest {
	
	private static final Logger logger = LoggerFactory.getLogger(LoginFirefoxTest.class.getName());
	
	static {
		activeBrowser = SupportedBrowsers.FIREFOX;
	}

	@Test
	public void testUsernameAndPasswordFieldsExist() {
		
		driver.get("http://localhost:8875/structr/#");
		
		assertEquals("Username field not found", 1, driver.findElements(By.id("usernameField")).size());
		assertEquals("Password field not found", 1, driver.findElements(By.id("passwordField")).size());
	}
	
	@Test
	public void testSuccessfulLogin() {
		
		createAdminUser();
		
		driver.get("http://localhost:8875/structr/#dashboard");
		
		assertEquals("Username field not found", 1, driver.findElements(By.id("usernameField")).size());
		assertEquals("Password field not found", 1, driver.findElements(By.id("passwordField")).size());
		assertEquals("Login button not found",   1, driver.findElements(By.id("loginButton")).size());

		final WebDriverWait wait = new WebDriverWait(driver, 2);
		
		final WebElement usernameField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("usernameField")));
		usernameField.sendKeys("admin");

		final WebElement passwordField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("passwordField")));
		passwordField.sendKeys("admin");

		final WebElement loginButton = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loginButton")));
		loginButton.click();
		
		wait.until(ExpectedConditions.titleIs("Structr Dashboard"));
		assertEquals("Structr Dashboard", driver.getTitle());
	}

	@Test
	public void testFailedLogin() {
		
		createAdminUser();
		
		driver.get("http://localhost:8875/structr/#dashboard");
		
		assertEquals("Username field not found", 1, driver.findElements(By.id("usernameField")).size());
		assertEquals("Password field not found", 1, driver.findElements(By.id("passwordField")).size());
		assertEquals("Login button not found",   1, driver.findElements(By.id("loginButton")).size());

		final WebDriverWait wait = new WebDriverWait(driver, 2);
		
		final WebElement usernameField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("usernameField")));
		usernameField.sendKeys("admin");

		final WebElement passwordField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("passwordField")));
		passwordField.sendKeys("wrongpassword");

		final WebElement loginButton = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loginButton")));
		loginButton.click();
		
		wait.until(ExpectedConditions.textToBe(By.id("errorText"), "Wrong username or password!"));
	}
		
	@Test
	public void testLoginToPages() {
		loginAsAdmin("Pages");
	}

	@Test
	public void testLoginToFiles() {
		loginAsAdmin("Files");
	}

	@Test
	public void testLoginToSecurity() {
		loginAsAdmin("Security");
	}

	@Test
	public void testLoginToDashboard() {
		loginAsAdmin("Dashboard");
	}

}
