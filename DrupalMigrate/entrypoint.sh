#!/bin/bash
set -e
MARKER="/opt/.initialized"
if [ ! -f "$MARKER" ]; then
    echo ">>> First container startup – running setup scripts..."
    unlink /var/www/html
    composer update --no-cache --no-dev --no-interaction --no-progress
    touch "$MARKER"
    ln -s /opt/drupal /var/www/html
    chown -R www-data:www-data /opt/drupal/sites/default/files
    echo ">>> Initialization complete."
else
    echo ">>> Container already initialized – skipping setup."
fi
exec "$@"

