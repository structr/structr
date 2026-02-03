
The Configuration Interface provides access to all runtime settings that control Structr's behavior. You can open it by clicking the wrench icon in the header bar. The interface opens in a new browser tab and requires a separate login using the superuser password defined in `structr.conf`.

![Configuration Interface](configuration-interface_login.png)

This separation is intentional. The Configuration Interface provides access to sensitive operations that go beyond normal application administration: you can configure database connections, restart services, and define cron expressions for scheduled functions. These capabilities would otherwise require direct access to maintenance commands or configuration files. By requiring a separate authentication with the superuser password, Structr adds an additional layer of security that protects these critical settings even if an attacker gains access to a regular admin account.

## Interface Layout

![Configuration Interface](configuration-interface.png)

The Configuration Interface uses a different layout than other areas of the Admin UI. The header bar is present at the top, but it contains no main navigation menu. In the top right corner, you find a logout link to end your session in the Configuration Interface.

Instead of a menu, the secondary area below the header provides a search field that filters configuration options by name or description.

The main area is divided into two sections. The left side displays a list of categories. Depending on your screen resolution, this list may appear at the top instead of on the left. Click a category to display its settings on the right side. Each setting shows its current value, default value, and a description of its purpose.

At the bottom of the screen, you find buttons to create new configuration entries, reload the configuration file, and save your changes. When you modify a setting, click **Save to structr.conf** in the bottom right corner to persist your changes.

Some settings display a small red button next to them. Clicking this button resets the setting to its default value and saves the change automatically. You do not need to click the save button separately for these reset operations.

## What You Can Configure
Settings are organized into categories such as application settings, database configuration, HTTP server options, security settings, and more. Most changes take effect immediately, though some require a server restart.

Beyond simple configuration values, the Configuration Interface is currently the only place where you can define cron expressions for user-defined functions. This allows you to schedule functions to run at specific intervals without writing additional code.

For a complete reference of all available settings, see the Settings chapter in the References section.
