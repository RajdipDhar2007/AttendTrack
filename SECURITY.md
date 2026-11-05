# Security Policy

## Supported versions

Only the latest release receives security fixes.

| Version | Supported |
|---------|-----------|
| Latest release (v1.1+) | ✅ |
| Older releases | ❌ |

## Reporting a vulnerability

Please **do not** open a public issue for security problems.

Report privately using GitHub's
[private vulnerability reporting](https://github.com/RajdipDhar2007/AttendTrack/security/advisories/new),
or email **rajdipdharcode@gmail.com** with:

- a description of the issue and its impact
- steps to reproduce (app version, Android version, device)
- any proof-of-concept, if available

You can expect an acknowledgement within **7 days** and a status update within **30 days**.
Once a fix is released you will be credited, unless you prefer to stay anonymous.

## Scope and known notes

- All attendance data is stored **locally** on the device (Room database).
- The optional AI Assistant sends your question and an attendance/timetable summary to
  Google's Gemini API using **your own API key**. Keys are never bundled in the repository.
- Never commit `.env`, `local.properties`, keystores or API keys. If you leaked a key, revoke it immediately.
- The APK in this repository is signed with a **debug** key and is meant for personal use and testing.
  Do not treat it as a production-signed build.
