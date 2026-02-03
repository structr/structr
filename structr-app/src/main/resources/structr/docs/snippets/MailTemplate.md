# MailTemplate

Defines reusable email content for automated messages in your application. Mail templates store the structure and text of emails such as registration confirmations, password resets, or notification messages. Key properties include `name` for referencing the template in code, `locale` for language-specific variants, and content fields for subject, plain text body, and HTML body. Templates support template expressions using `${...}` syntax that are replaced with dynamic values when the email is sent, allowing personalization with user-specific information like `${link}` for confirmation URLs or `${me.name}` for the recipient's name.

## Built-in Workflows

Structr uses predefined template names for built-in workflows: `CONFIRM_REGISTRATION_*` templates control the user self-registration flow, and `RESET_PASSWORD_*` templates control the password reset flow. Both include templates for sender address, sender name, subject, text body, HTML body, base URL, and target pages. The Admin UI provides a Template Wizard that automatically generates standard templates, plus a visual editor with live preview. Each template can have multiple locale variants to support multi-language applications.
