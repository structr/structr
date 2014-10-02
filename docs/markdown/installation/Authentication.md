Structr has user authentication built-in and even supports external authentication over OAuth. Structr supports the following OAuth services:

- Twitter (OAuth 1a)
- Facebook, Google, GitHub, LinkedIn (all OAuth 2.0)

To configure external authentication, you need to properly configure the services in structr.conf.

If using the built-in authentication, a user node will be created. Structr doesn't store passwords, only a salted SHA-512 hash is stored to identify a user at login.
