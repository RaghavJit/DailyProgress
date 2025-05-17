# **Hosting Mapathon using Django-uwsgi-Nginx on Odin**

## **Task Overview**

The objective was to host the following 5 branches of Mapathon repository using uwsgi and nginx.
1. https://github.com/FOSSEE/Mapathon/tree/iitb-aai-mapathon-2025 [[visit site](https://iitb-aai-mapathon-2025.fossee.in/)]
1. https://github.com/FOSSEE/Mapathon/tree/iot-gis-hackathon-2025 [[visit site](https://iot-gis-hackathon-2025.fossee.in/)]
1. https://github.com/FOSSEE/Mapathon/tree/agrogis-2025 [[visit site](https://agrogis.fossee.in/)]
1. https://github.com/FOSSEE/Mapathon/tree/maritimegis-2025 [[visit site](https://maritimegis.fossee.in/)]
1. https://github.com/FOSSEE/Mapathon/tree/aigis-2025 [[visit](https://aigis-hackathon.fossee.in/)]

### Justification for using uwsgi 
- **Production-Ready**: uWSGI is designed for deployment in production environments, unlike Django’s development server which is not optimized for performance or security.

- **Performance & Scalability**: Supports multiple worker processes and threads, enabling better request handling under high load.

- **Advanced Features**: Offers features like asynchronous processing, process management, and emperor mode for managing multiple applications.

- **Web Server Integration**: Easily integrates with web servers like Nginx, allowing efficient static file handling and reverse proxying for improved performance and flexibility.


## **Documentation referred:**

1. [Django - UWSGI - Nginx Guide](https://tonyteaches.tech/django-nginx-uwsgi-tutorial/)
1. [Official UWSGI Docs](https://uwsgi-docs.readthedocs.io/en/latest/Nginx.html)
1. [Django docs](https://docs.djangoproject.com/en/5.1/howto/deployment/wsgi/uwsgi/)
1. [Configuration Guide](https://www.digitalocean.com/community/tutorials/how-to-serve-django-applications-with-uwsgi-and-nginx-on-ubuntu-16-04)
1. [Permission issue solution](https://stackoverflow.com/questions/30199501/uwsgi-socket-file-not-created)
1. [Comments under this article](https://www.digitalocean.com/community/tutorials/how-to-set-up-uwsgi-and-nginx-to-serve-python-apps-on-centos-7)
---

## **Process**

### Cloning repo and file permissions

The repository is kept in the root directory at path: `/Assets/Django/Mapathon-<branchname>/Mapathon`

```
git clone https://github.com/FOSSEE/Mapathon -b <branch you want to clone>
```

The owner of the files were changed to `raghav`, this was done both out of convenience and to allow nginx to read the files. This step is not necessary.

```
chown -R raghav:raghav /Assets/Django/*
```

### SQLite configuration and other code changes

To keep things simple and production friendly we decided to use SQLite as database for the website.

Read the changes made by me in this [diff file](./diff). This diff file can be applied to settings.py using `patch`.

```
patch mapathon/settings.py < diff
```

We also need to add the site name to allowed hosts. To ensure making minimal changes to code I simply added the site name to allowed hosts variable.

```
ALLOWED_HOSTS = ALLOWED_HOSTS_VAL + ['<sitename>']
```

### Installing packages and running the site.

The website uses python version `Python 3.9.21` and pip version `pip 23.0.1`.

```
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

python manage.py makemigrations
python manage.py migrate
python manage.py createsuperuser
python manage.py runserver 127.0.0.1:8000
```

This should run the django site with SQLite database on port 8000 on localhost. Visit /admin/ endpoint to access admin panel (trailing / is important)/

### Running with UWSGI
 
Ensure that uwsgi package is installed, before proceeding. Testing if UWSGI is working properly
```
uwsgi --http :8000 --module mapathon.wsgi
```

If this works, it means that uwsgi is properly installed and configured. For production environment, it is recommended to use a uwsgi.ini file. The ini files for this project can be found at `/etc/uwsgi.d/`

Kindly refer [this](./mapathon_uwsgi.ini) file as a sample. To run the website with the given uwsgi ini file run the following command:
```
uwsgi --ini /etc/uwsgi.d/mapathon-<sitename>.ini
```

Kindly note that placement of socket file might affect nginx's ability to read them. It is recommended to keep sockets in `/run/uwsgi` directory.

### Nginx setup and other configurations

Ensure that nginx is installed and updated. I used the directory `/etc/nginx/conf.fossee/` to keep the site configs, I was already provided with certificate files for each website.

Place this [uwsgi_params](./uwsgi_params) file in the root of the cloned repo. This file can be used if `include uwsgi_params` doesn't work in Nginx.

Before serving the website with Nginx-UWSGI, it is advisable to use `collectstatic` to create static files. This ensures that all static content will be served separately (by Nginx) as it handles static content better than UWSGI.

```
python manage.py collectstatic
```

Similarly it is also advisable to use a media folder to store all media file (.jpg, mp3, .png etc) to be served directly by Nginx.

After creating the nginx config test it by visiting `/static/admin/css/login.css`. This ensures that nginx has the appropriate permissions and is able to serve files. 

To serve the website run the website with uwsgi (as guided above) and visit /admin/. 

If the page shows 503 error it might be due to SELinux, to fix the issue use this:

```
sudo cat /var/log/audit/audit.log | grep nginx | grep denied | audit2allow -M mynginx
sudo semodule -i mynginx.pp
```

Please note that this will only allow nginx to read socket files if uwsgi is run interactively. When creating systemd service to run uwsgi as emperor, SELinux will block nginx from reading sockets, as the context will be different. 

### Making SystemD service

To serve the sites even after reboots, a systemd service can be configured. 

When running more than one sites with uwsgi, uwsgi should be used in emperor mode. Emperor mode reloads workers if it detects any changes in the ini files.

Obtain the systemd service file [here](./uwsgi-emperor.service). The ExecStart should have the original location of uwsgi.

Enable the service and start it, this will probably result in 503 error at /.

Finally, perform the below to allow nginx to read uwsgi sockets, in systemd's context

```
sudo cat /var/log/audit/audit.log | grep nginx | grep denied | audit2allow -M mynginx
sudo semodule -i mynginx.pp
```

### Conclusion
The sites were hosted using UWSGI-Nginx and SystemD service was configured to run the sites.