# OSDAG Website 503 crash issue

## Hypothesis 1 (Yet to be tested)
Odin server was overloaded making the podman containers slower, connection to upstream to timeout.

### Findings:
in nginx configuration proxy buffering was turned off, and proxy timeout was the default value.

### Logs:
```
2025/11/03 12:45:48 [error] 503615#503615: *196 upstream prematurely closed connection while reading response header from upstream, client: 49.43.91.86, server: osdag.fossee.org.in, request: "GET / HTTP/2.0", upstream: "http://127.0.0.1:8081/", host: "osdag.fossee.org.in"
```

### Sources:
1. [stackoverflow discussion](https://stackoverflow.com/questions/36488688/nginx-upstream-prematurely-closed-connection-while-reading-response-header-from)
1. [nginx configuration](https://www.uptimia.com/questions/how-to-fix-nginx-upstream-prematurely-closed-connection-while-reading-response-header-from-upstream-error)

### Fixes:
Improved nginx configuration
```
location / {

        proxy_pass http://127.0.0.1:8081;

        #timeouts
        proxy_read_timeout 300; 
        proxy_connect_timeout 300; 
        proxy_send_timeout 300; 

        #buffers
        proxy_buffering on;
        proxy_buffers 8 16k; 
        proxy_buffer_size 32k;

        #fastcgi
        fastcgi_buffers 8 16k; 
        fastcgi_buffer_size 32k;

        #headers
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_cache off;
}
```
### Conclusion 
Issue still occurs occasionally.


## Hypothesis 2 (For OSDAG)
A misconfigured php module. (Twig and database abstraction layer suspected)

### Findings:
upstream sends out empty response nginx interprets it as 502. Error can be due to invalid call to function db_set_active() which is undefined as it lost support after Drupal 7.

### Logs:
```
Error: Call to undefined function db_set_active() in /var/www/html/modules/contrib/php/php.module(81) : eval()'d code on line 95 #0 /var/www/html/modules/contrib/php/php.module(81): eval()\n#1 /var/www/html/modules/contrib/php/src/Plugin/Filter/Php.php(26): php_eval('<style>\\n.forumb...')\n#2 /var/www/html/core/modules/filter/src/Element/ProcessedText.php(123): Drupal\\php\\Plugin\\Filter\\Php->process('<style>\\n.forumb...', 'und')\n#3 [internal function]: 
```
For complete logs kindly refer [this](./osdag_db_set_active.logs)

### Sources:
1. [database.inc not supported anymore](https://api.drupal.org/api/drupal/includes%21database%21database.inc/function/db_set_active/7.x)
2. [module and source of error](https://drupal.stackexchange.com/questions/30443/call-to-undefined-function-db-set-active)

### Fixes:
No fixes yet by SysAdmin

### Conclusion 
No fixes attempted by Sys-Adm

## Hypothesis 2 (For FreeCAD)
Treating Message object as an array in FormBuilder.php and some getToken() function related problem, likely a programming bug.

### Findings:
Somewhere in Drupal’s form processing pipeline (FormBuilder), the code is trying to treat a Messenger object like an array.
It is recommended to check Symphony module for errors.

### Logs:
```
Uncaught PHP Exception Error: "Cannot use object of type Drupal\\Core\\Messenger\\Messenger as array" at /var/www/html/core/lib/Drupal/Core/Form/FormBuilder.php line 548
RuntimeException: Failed to start the session because headers have already been sent by "/var/www/html/vendor/symfony/http-foundation/Response.php" at line 408. in /var/www/html/vendor/symfony/http-foundation/Session/Storage/NativeSessionStorage.php on line 132 #0 /var/www/html/core/lib/Drupal/Core/Session/SessionManager.php(162): 
```

### Sources:
1. [MessageInterface instead of Message](https://www.drupal.org/project/queue_ui/issues/3455980) 
1. [Resolved headers issue Github](https://www.drupal.org/project/3168437/issues/3195754)
1. [Issue discussion](https://github.com/symfony/symfony/discussions/48282)

### Fixes:
No fixes yet by SysAdmin

### Conclusion 
No fixes attempted


