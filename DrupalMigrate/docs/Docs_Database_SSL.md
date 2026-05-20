## Database SSL Issue

Sometimes when testing a SQL connection using a standalone SQL client, the database may fail to connect and show one of the following errors:

```
ERROR 2026 (HY000): TLS/SSL error: self-signed certificate in certificate chain
# OR
ERROR 2026 (HY000): TLS/SSL error: SSL is required, but the server does not support it
```

### Understanding the Issue

Drush has its own SQL connection engine. You only see these SSL-related errors when you install an external SQL client such as `mariadb-client` or `mysql-client`. These errors are **not** caused by Drush in most cases.

Key points:

* This issue is usually triggered by **your SQL client choice and version**, not by Drush or Drupal.
* The client attempts to use TLS certificates and fails for some reason.
* The database server on Odin is **configured without SSL certificates**, so any certificate-related failure is nearly always **client-side**.
* MariaDB client, in particular, **forces SSL/TLS checks by default**. So during diagnostic testing with `mariadb-client`, this error may appear even if the actual Drupal/Drush connection is fine.
* Drush can optionally use system SQL clients *if installed*, which means Drush might indirectly inherit these SSL problems. (See drush config section below)
* In newer versions of Drush, SSL/TLS might be enabled by default but can be disabled via command options.

### Troubleshooting

1. **Check if the database is empty or if the SQL dump is corrupt.**
   Corrupted dumps often produce misleading connection errors.

2. **Check container logs using:**

   ```
   podman logs <container name>
   ```

   There have been past cases where missing columns/tables (e.g., corruption in the Routes table) triggered the same SSL-looking error message.

3. **Try using an older SQL dump** for the site repository.
   If an older dump works, the issue likely lies in the dump. If not, there may be a code issue in the site itself.

### How to Connect to SQL Without SSL (mysql/mariadb clients)

**mysql-client**

```
mysql -u <user> -p --ssl-mode=DISABLED
```

**mariadb-client**

```
mysql -u <user> -p --skip-ssl
```

### Drush config issue

If due to any reason drush starts demanding for certs in future version here is a simple drush config file that can disable that 
Refrences:
1. [StackOverflow](https://stackoverflow.com/questions/63956543/drush-demands-ssl-connection-to-database)
2. [Drupal Forums](https://drupal.stackexchange.com/questions/322230/tls-ssl-error-self-signed-certificate-in-certificate-chain)

Create drush.yml file in /opt/drush/drush/drush.yml with following content:
```
command:
  sql:
    cli:
      options:
        extra: '--disable-ssl'
    query:
      options:
        extra: '--disable-ssl'
    dump:
      options:
        extra: '--disable-ssl'
        extra-dump: '--disable-ssl --no-tablespaces'
    drop:
      options:
        extra: '--disable-ssl'
    create:
      options:
        extra: '--disable-ssl'
  site:
    install:
      options:
        extra: '--disable-ssl'
```

Note: If creating this file in running does not work even after doing ``drush cr``. Ask the developer to add this file in {Drupal root}/drush/dursh.yml and build again with updated repo. 

---
