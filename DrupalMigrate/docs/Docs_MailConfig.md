# Mail configuration

Drupal needs to send emails for actions such as password resets. Because the site runs inside a container, there are a few extra moving parts in the delivery path. Before describing the workflow, here are the key terms a system administrator must understand:

1. **MTA** – A full mail transfer agent responsible for routing, queuing, and delivering email to remote servers.
2. **MSA** – A mail submission agent that accepts mail from applications and forwards it to an MTA.
3. **Postfix** – A full-featured MTA used for routing outgoing mail, performing DNS lookups, queuing, and final delivery.
4. **sSMTP** – A lightweight MSA that forwards outgoing mail to another SMTP server without any queuing or delivery logic.
5. **Mailutils** – A package that provides mail-related command-line tools used by applications inside the container.

---

# Workflow of email delivery

Drupal generates outbound messages using its internal createMail() mechanism. This invokes the system’s mail program inside the container. The container uses sSMTP as its MSA, with configuration injected through the following commands:

```
podman exec arduino_container apt update
podman exec arduino_container apt install mailutils ssmtp -y

podman exec arduino_container sh -c 'echo "mailhub=10.0.2.2:25" > /etc/ssmtp/ssmtp.conf'
podman exec arduino_container sh -c 'echo "root:root@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases'
podman exec arduino_container sh -c 'echo "'${ENV_USR}':'${ENV_USR}'@fossee.org.in:10.0.2.2:25" >> /etc/ssmtp/revaliases'
```

This setup instructs sSMTP to forward all outgoing mail to the host at 10.0.2.2 port 25, defines the default sender for the root user, and maps application users (through ENV_USR) to consistent sender identities. sSMTP does no delivery by itself; it simply hands the mail off to the host.

The host at 10.0.2.2 runs Postfix as the actual MTA. Postfix performs DNS and MX lookups, applies its policies, and handles the final delivery to external mail servers such as Gmail.

The container’s message flow is therefore: Drupal → sSMTP (container) → Postfix (host) → external SMTP servers.

---

# Important notes

Drupal sends mail using a “From” address such as [webmaster@domain.com](mailto:webmaster@domain.com), which can be configured per site. For the Arduino site this is under:

[https://arduino.fossee.org.in/admin/config/system/site-information](https://arduino.fossee.org.in/admin/config/system/site-information)

Changing the From address may cause delivery failures depending on your Postfix configuration and relay rules.

A general guide to Postfix configuration is available here:
[https://www.howtoforge.com/tutorial/configure-postfix-to-use-gmail-as-a-mail-relay/](https://www.howtoforge.com/tutorial/configure-postfix-to-use-gmail-as-a-mail-relay/)
