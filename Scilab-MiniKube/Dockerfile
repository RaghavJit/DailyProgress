FROM python:3.9.21-slim

RUN apt-get update && apt-get install -y \
    git \
    gcc \
    python3-dev \
    libffi-dev \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY ./scilab-on-cloud /app

ARG CONFIG_FILE=./config.py
COPY ${CONFIG_FILE} /app/soc/

RUN python3 -m venv venv && \
    ./venv/bin/pip install --no-cache-dir --upgrade pip && \
    ./venv/bin/pip install --no-cache-dir -r requirements.txt

EXPOSE 8000

CMD ["sh", "-c", "./venv/bin/python manage.py makemigrations website && \
                 ./venv/bin/python manage.py migrate && \
                 ./venv/bin/python manage.py runserver 0.0.0.0:8000"]


