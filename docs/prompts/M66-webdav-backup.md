# M66 — WebDAV backup destination

> Branch `m66-webdav-backup`. Not Cloudflare/R2. HTTPS + Basic auth only.

## In scope

- Backup destination: SAF (default) | WebDAV
- Configure HTTPS URL, username, password (EncryptedSharedPreferences), remote path
- Test connection (PROPFIND Depth 0); PUT backup + keep-N prune; list + GET restore
- DE + EN; unit tests for URL + PROPFIND parser

## Out of scope

- R2/S3, sardine, Digest/NTLM, scheduled sync, ROOT `/data/data` (M67)
- Cleartext HTTP; logging credentials

## Done when

- [ ] WebDAV backup/restore works with existing encrypt + Shizuku data tiers
- [ ] SAF + CreateDocument still work when SAF is selected
- [ ] Password never in DataStore / Kiln-Config / logs
- [ ] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
