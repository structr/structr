




    "SMTP" has topic "Sending emails", "Receiving emails", "SMTP Settings"


















## Configuring Sender Addresses for Structr Email Templates

When sending emails through Structr using a common SMTP server from your mail provider, be aware that most providers bind your sender address to your authenticated user address.

For example, if your SMTP account is `user.example@example.com`, the sender address in the email header must also be `user.example@example.com`.

By default, Structr sends password reset and registration confirmation emails from `<structr-mail-daemon@localhost>`. This typically results in an error from your provider, such as:
```
550 5.7.1 [M12] User [user.example@example.com] not authorized to send on behalf of <structr-mail-daemon@localhost>
```

### Solution

Configure the mail templates in Structr and set the `*_SENDER_ADDRESS` fields to your correct sender address:

1. Navigate to **Structr-UI → Mail Templates**
2. Use the wizard (wand icon) to create default templates
3. Modify `CONFIRM_REGISTRATION_SENDER_ADDRESS` and `RESET_PASSWORD_SENDER_ADDRESS` to match your authenticated SMTP address