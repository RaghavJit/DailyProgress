# Folder Permissions for Drupal Sites

The folder permissions stored in a Drupal repository are often not suitable for production. Developers may relax permissions during local testing, and directly applying those permissions in a production container can lead to security issues or break site functionality.

This document explains:

* How mounted folders behave in Podman
* How SELinux labels interact with container mounts
* How permission fixing scripts must be used
* What additional `chown`/`chmod` is required specifically for mounted folders
* How user-namespace mapping affects ownership seen from host vs container

---

## Folder Mounting

To ensure persistence across container restarts, certain directories are mounted into the container. For example, for the Arduino site:

1. `arduino_public:/var/www/html/sites/default/files/`
2. `arduino_uploads:/var/www/html/arduino_uploads`

Mounted as:

```
-v arduino_public:/var/www/html/sites/default/files:Z
-v arduino_uploads:/var/www/html/arduino_uploads:Z
```

The `:Z` flag applies SELinux labels suitable for container use. When the container starts, SELinux applies the correct labels to the mounted directories, allowing only the container that owns the label range to access them.

A full explanation of SELinux behavior is included below.

---

## Permissions

A script from the Drupal community can reset and secure the entire Drupal installation's permissions. It is executed like this:

```
podman exec <container-name> sh -c 'curl -s https://static.fossee.in/IT/drupal_fix_permissions.sh | bash -s -- -u="<db-user>"'
```

This script is also stored on internal servers. If the server is offline, it can be obtained from
[text](../files/drupal_fix_permissions.sh).

It is important that:

* The script is executed **in the root of the Drupal site**
* The directory structure is correct
* You understand that this script applies *strict* permissions

These strict permissions are correct for local testing or bare-metal hosting, but **not sufficient when Podman/Docker mounted folders are used**, because mounted folders need looser permissions to allow container write access.

---

## Permissions for Mounted Folders

Once the script has run, most Drupal root permissions are correct.
However, **mounted folders require different handling** because:

1. The folder on the host is mounted into a container path.
2. The container needs read/write access to these folders.
3. SELinux labels must be correct on the host before any permission changes will work.
4. After mounting, `chown` and `chmod` must be executed **inside the container** on the mount points.
5. If SELinux labels are incorrect, even root inside the container will receive “permission denied” for `chown`/`chmod`.

### Commands applied inside the container

These commands update permissions on the mounted paths (changes reflect on the host):

```
chown -R root:www-data /opt/drupal/sites/default/files
chown -R root:www-data /opt/drupal/arduino_uploads
chmod -R 770 /opt/drupal/sites/default/files
chmod -R 770 /opt/drupal/arduino_uploads
```

Explanation:

* `drwxrwx---` means owner and group have full access; all others have none.
* Owner on the host appears as `jenkins`, group appears as `100032`.
* UID/GID `100032` on the host corresponds to UID/GID `33` (`www-data`) inside the container due to Podman’s user-namespace mapping.
* `100032` is not a real user on the host; it is a remapped namespace ID.
* `www-data` **must** have write + execute permissions for Drupal uploads to function.

---

## SELinux Labels

### Setting SELinux labels on host folders

Before mounting, the folders on the host must be labeled correctly:

```
chcon -R system_u:object_r:container_file_t:s0 <folder-path>
```

You can verify labels with:

```
ls -Z <folder-path>
```

### Explanation of the label

`system_u:object_r:container_file_t:s0` means:

* `system_u` – SELinux user (used by system processes, including Jenkins in this context)
* `object_r` – Object type (files, directories)
* `container_file_t` – File type intended for container access
* `s0` – SELinux level
* Category values (`cXXX,cYYY`) are **automatically assigned by Podman** when mounting with `:Z`

Mounted folders for a container share the same category range, which isolates them from other containers.

---

## Example of Correct Permissions and Labels

Below is a real example of properly labeled and permissioned directories:

```
drwxr-xr-x. 24 jenkins jenkins unconfined_u:object_r:container_file_t:s0       4096 May 14 09:39 .
drwxr-xr-x. 21 jenkins jenkins unconfined_u:object_r:var_lib_t:s0              4096 May 18 10:25 ..
drwxrwx---. 10 jenkins  100032 system_u:object_r:container_file_t:s0:c523,c893 8192 Apr 23 10:08 arduino_public
drwxrwx---.  2 jenkins  100032 system_u:object_r:container_file_t:s0:c523,c893   10 Apr 23 10:05 arduino_uploads
drwxrwx---. 10 jenkins  100032 system_u:object_r:container_file_t:s0:c301,c966 8192 Apr 23 11:03 cfd_public
drwxrwx---.  9 jenkins  100032 system_u:object_r:container_file_t:s0:c301,c966 4096 Apr 23 11:28 cfd_uploads
drwxrwx---. 10 jenkins  100032 system_u:object_r:container_file_t:s0:c238,c481 4096 Apr 23 11:27 dwsim_public
drwxr-x---.  8 jenkins  100032 system_u:object_r:container_file_t:s0:c238,c481  189 Apr 23 11:30 dwsim_uploads
drwxrwx---. 13 jenkins  100032 system_u:object_r:container_file_t:s0:c512,c654 4096 May  8 11:13 esim_public
drwxrwx---.  9 jenkins  100032 system_u:object_r:container_file_t:s0:c512,c654 4096 Apr 23 11:30 esim_uploads
drwxrwx---. 13 jenkins  100032 system_u:object_r:container_file_t:s0:c505,c939 4096 May 18 10:10 fossee_public
drwxrwx---.  2 jenkins  100032 system_u:object_r:container_file_t:s0:c505,c939   10 May 14 09:39 fossee_uploads
drwxrwx---.  6 jenkins  100032 system_u:object_r:container_file_t:s0:c44,c604  4096 Apr 23 10:11 freecad_public
...
```