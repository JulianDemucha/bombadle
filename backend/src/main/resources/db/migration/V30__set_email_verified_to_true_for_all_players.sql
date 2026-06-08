UPDATE player
SET email_verified = true
WHERE email_verified = false OR email_verified IS NULL;