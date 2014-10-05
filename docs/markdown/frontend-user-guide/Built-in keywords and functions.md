Structr provides a long list of functions which you can use to transform database values, for example if you want to capitalize the name of a user, or if you want to display the MD5 hash of an email address instead of the address itself (for use with avatar services etc.):

    ${capitalize(image.owner.name)}
    ${md5(image.owner.emailAddress)}

See <a href="/frontend-user-guide/#Appendix B: Keywords and Functions">Appendix B</a> for a complete list of built-in keywords and functions.

