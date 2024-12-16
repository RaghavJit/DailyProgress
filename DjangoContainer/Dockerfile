FROM quay.io/roboxes/alma8

RUN dnf install -y python3-devel pkgconfig git kernel-headers mysql-devel && \
    useradd -m pyfossuser

USER pyfossuser
WORKDIR /home/pyfossuser

ARG ALLOWED_HOSTS="127.0.0.1"

RUN git clone https://github.com/Raghavjit/pyfoss && \
    cd pyfoss && \
    python3 -m venv venv && \
    source venv/bin/activate && \
    pip install --no-cache-dir -r requirements.txt && \
    sed -i "/ALLOWED_HOSTS = \[/ s/\]$/'${ALLOWED_HOSTS}']/g" pyfoss/settings.py

ARG CONFIG_FILE=./config.py
COPY ${CONFIG_FILE} /home/pyfossuser/pyfoss/pyfoss/config.py

WORKDIR /home/pyfossuser/pyfoss

CMD ["sh", "-c", "source venv/bin/activate && \
                  python3 manage.py makemigrations && \
                  python3 manage.py migrate && \
                  python3 manage.py runserver 0.0.0.0:8000"]
