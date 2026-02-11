# MailTemplate

Defines reusable email content for automated messages like registration confirmations or password resets. Key properties include `name` for referencing in code, `locale` for language variants, and content fields for subject, plain text, and HTML body. Use `${...}` expressions for dynamic values like `${link}` or `${me.name}`.

## Details

Structr uses predefined template names for built-in workflows: `CONFIRM_REGISTRATION_*` for self-registration and `RESET_PASSWORD_*` for password reset. The Admin UI provides a Template Wizard that generates these automatically, plus a visual editor with live preview. Each template can have multiple locale variants for multi-language support.
