# PyFOSS Dockerfile Documentation  

A Dockerfile for containerizing the PyFOSS application using Podman/Docker. The setup is optimized for minimizing image layers and size while allowing flexibility in configuring the application through `config.py` and `ALLOWED_HOSTS`.

---

## Requirements and Limitations  

1. The Dockerfile is written for compatibility with Podman and Docker.  
2. Efforts are made to minimize the image size and number of layers for efficiency.  
3. Users can specify custom `config.py` and `ALLOWED_HOSTS` values through build-time arguments (`--build-arg`).  
4. The container assumes the application dependencies specified in `requirements.txt` are correct and complete, some minimal changes were made to the code [[commit-log](https://github.com/FOSSEE/pyfoss/compare/master...RaghavJit:pyfoss:master)].  
5. Ensure Podman or Docker is properly configured before running the build.  

---

## Dockerfile Overview  

The Dockerfile follows these key steps:  

1. **Base Image Setup**  
   - Uses `quay.io/roboxes/alma8` as the base image for compatibility with PyFOSS.  

2. **Dependency Installation**  
   - Installs Python development tools, libraries, and necessary packages.  

3. **Application Setup**  
   - Downloads the PyFOSS application from the GitHub repository.  
   - Sets up a Python virtual environment (`venv`) and installs required dependencies.  

4. **Dynamic Configuration**  
   - Allows modification of the `ALLOWED_HOSTS` in the Django settings.  
   - Copies a user-provided `config.py` file into the container for custom configurations.  

5. **Application Launch**  
   - Starts the Django development server to serve the application.  

---

## Usage  

### Build the Image  

To build the container image, use:  

```bash
podman build --build-arg ALLOWED_HOSTS="192.168.1.100" --build-arg CONFIG_FILE=./my-config.py -t pyfoss-app .
```  

- **Arguments**:  
  - `ALLOWED_HOSTS`: A comma-separated list of allowed hosts for the Django application. Defaults to `127.0.0.1`.  
  - `CONFIG_FILE`: Path to the configuration file to be copied into the container. Defaults to `./config.py`.  

### Run the Container  

To run the container after building:  

```bash
podman run -d -p 8000:8000 --name pyfoss-app pyfoss-app
```  

This command maps the container's port `8000` to the host's port `8000`.  

---

## Process Details  

### 1. Base Image Setup  

- **Why**: `quay.io/roboxes/alma8` provides a lightweight and stable base for the PyFOSS application.  
- **Command Reference**:  
  ```Dockerfile
  FROM quay.io/roboxes/alma8
  ```

---

### 2. Dependency Installation  

- **Why**: Required for Python development, application dependencies, and database integration.  
- **Command Reference**:  
  ```Dockerfile
  RUN dnf install -y python3-devel pkgconfig git kernel-headers mysql-devel
  ```

---

### 3. Application Setup  

- **Why**: Sets up the PyFOSS application with its dependencies in a Python virtual environment.  
- **Command Reference**:  
  ```Dockerfile
  RUN git clone https://github.com/Raghavjit/pyfoss && \
      cd pyfoss && \
      python3 -m venv venv && \
      source venv/bin/activate && \
      pip install --no-cache-dir -r requirements.txt
  ```

---

### 4. Dynamic Configuration  

- **Why**: Allows customization of `ALLOWED_HOSTS` and `config.py` without modifying the Dockerfile.  
- **Command Reference**:  
  ```Dockerfile
  ARG ALLOWED_HOSTS="127.0.0.1"
  ARG CONFIG_FILE=./config.py
  COPY ${CONFIG_FILE} /home/pyfossuser/pyfoss/pyfoss/config.py
  RUN sed -i "/ALLOWED_HOSTS = \[/ s/\]$/'${ALLOWED_HOSTS}']/g" pyfoss/settings.py
  ```

---

### 5. Application Launch  

- **Why**: Starts the Django application on `0.0.0.0:8000` to make it accessible.  
- **Command Reference**:  
  ```Dockerfile
  CMD ["sh", "-c", "source venv/bin/activate && \
                    python3 manage.py makemigrations && \
                    python3 manage.py migrate && \
                    python3 manage.py runserver 0.0.0.0:8000"]
  ```

---

## Notes  

1. **Custom Configuration**:  
   - Use the `CONFIG_FILE` argument to provide a path to your `config.py`.  
   - Modify `ALLOWED_HOSTS` using the `--build-arg ALLOWED_HOSTS` flag during the build process.  

2. **Optimized Image**:  
   - The script minimizes the number of layers by combining commands into a single `RUN` statement where feasible.  
   - The use of `--no-cache-dir` with `pip install` reduces image size.  

3. **Debugging**:  
   - Use the `-it` flag with `podman run` to interactively troubleshoot the container if issues arise.  
   - Example:  
     ```bash
     podman run -it --rm pyfoss-app bash
     ```  

By following this documentation, users can easily build and run the PyFOSS application within a containerized environment.
