# **Hosting Mapathon using Django-uwsgi-Nginx on Odin**

## **Task Overview**

The objective was to host the following 5 branches of Mapathon repository using uwsgi and nginx.
1. https://github.com/FOSSEE/Mapathon/tree/iitb-aai-mapathon-2025
1. https://github.com/FOSSEE/Mapathon/tree/iot-gis-hackathon-2025
1. https://github.com/FOSSEE/Mapathon/tree/agrogis-2025
1. https://github.com/FOSSEE/Mapathon/tree/maritimegis-2025
1. https://github.com/FOSSEE/Mapathon/tree/aigis-2025

### Justification for using uwsgi 
- **Production-Ready**: uWSGI is designed for deployment in production environments, unlike Django’s development server which is not optimized for performance or security.

- **Performance & Scalability**: Supports multiple worker processes and threads, enabling better request handling under high load.

- **Advanced Features**: Offers features like asynchronous processing, process management, and emperor mode for managing multiple applications.

- **Web Server Integration**: Easily integrates with web servers like Nginx, allowing efficient static file handling and reverse proxying for improved performance and flexibility.

---

## **Documentation refered:**

1. [Django - UWSGI - Nginx Guide](https://tonyteaches.tech/django-nginx-uwsgi-tutorial/)
1. [Official UWSGI Docs](https://uwsgi-docs.readthedocs.io/en/latest/Nginx.html)
1. [Django docs](https://docs.djangoproject.com/en/5.1/howto/deployment/wsgi/uwsgi/)
1. [Configuration Guide](https://www.digitalocean.com/community/tutorials/how-to-serve-django-applications-with-uwsgi-and-nginx-on-ubuntu-16-04)
1. [Permission issue solution](https://stackoverflow.com/questions/30199501/uwsgi-socket-file-not-created)

---

## **Process**

### Cloning repo and configuration for SQLite

To keep things simple and production friendly we decided to use SQLite as database for the website.

```
git clone https://github.com/FOSSEE/Mapathon -b <branch you want to clone>
```

Read the changes made by me in this [diff file](./diff). This diff file can be applied to settings.py using `patch` command for convenience.

### Django Migrations

Perform the following steps for the website to run:
```
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
pip install uwsgi

python manage.py makemigrations
python maange.py migrate
python manage.py runserver 127.0.0.1:8000
```

This should run the django site with SQLite database on port 8000 on localhost.

### Running with UWSGI
 
Testing if UWSGI is working properly
```
uwsgi --http :8000 --module mapathon.wsgi
```

Place this [uwsgi_params](./uwsgi_params) file in the root of the cloned repo. This file can be used if `include uwsgi_params` doesn't work in Nginx.

Before serving the website with Nginx-UWSGI, it is advisable to use `collectstatic` to create static files. This ensures that all static content will be served saperatly (by Nginx) as it handles static content better than UWSGI.

```
python manage.py collectstatic
```

Similarly it is also advisable to use a media folder to store all media file (.jpg, mp3, .png etc) to be served directly by Nginx.

### Socket file

The placement of socket file can affect Nginx's ability to read it. If the path to socket file is not readable for nginx, it will not be able to serve the file. But Nginx will still serve /static and /media if they are configured. This can be used as a test of Nginx - socket file permission issues.

Obtain the ini file [here](./mapathon_uwsgi.ini)

To serve using uwsgi use the following command:
```
uwsgi --ini mapathon_uwsgi.ini
```

This spawns all the workers and creates a socket file in the designated location.

### Listening to socket file using Nginx

Before proceeding to this step, ensure that nginx is installed.

Obtain the config file [here](./nginx_config)
### **Conclusion:**
