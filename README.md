# StockDailyStatusRep

## SMTP configuration

Email publishing requires these environment variables:

- `SMTP_USERNAME`: the mailbox used to authenticate and send messages
- `SMTP_PASSWORD`: the mailbox password or app password
- `SMTP_RECIPIENTS`: a comma-separated list of recipient email addresses

Store these values in your runtime or GitHub Actions secret manager. Do not commit
credentials or local environment files to the repository.
