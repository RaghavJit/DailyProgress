# IP white listing

**Firewalld rules applied:** FOSSEE uses [firewalld](https://firewalld.org/documentation/man-pages/firewall-cmd.html) as it's firewall, it's also the default firewall for RHEL based systems. The following rules were applied to the host machine

---

### Zone-wise rules:
1. [FOSSEE](./FirewallRules/firewall.cmd.FOSSEE.sh)
1. [IITB](./FirewallRules/firewall.cmd.IITB.sh)
1. [SysAd](./FirewallRules/firewall.cmd.SysAd.sh)
1. [Team](./FirewallRules/firewall.cmd.Team.sh) (Modified to allow access to me)
1. [VM](./FirewallRules/firewall.cmd.VM.sh)

---

### Problem faced
The VM was totally accessible and reachable from the VitualBox host. But case was not the same for global access.

**Pinging:**

Upon activating these rules, network packets were not longer able to reach the VM's IPV6 `2a01:4f9:3080:2d4d::5`. Upon pinging it following result was obtained

```
PING 2a01:4f9:3080:2d4d::5 (2a01:4f9:3080:2d4d::5) 56 data bytes
From 2a01:4f9:3080:2d4d::2 icmp_seq=1 Destination unreachable: Administratively prohibited
From 2a01:4f9:3080:2d4d::2 icmp_seq=2 Destination unreachable: Administratively prohibited
From 2a01:4f9:3080:2d4d::2 icmp_seq=3 Destination unreachable: Administratively prohibited
From 2a01:4f9:3080:2d4d::2 icmp_seq=4 Destination unreachable: Administratively prohibited
^C
--- 2a01:4f9:3080:2d4d::5 ping statistics ---
5 packets transmitted, 0 received, +4 errors, 100% packet loss, time 4006ms
```
This shows that IPV6 is still rout able but the packets are being drooped due to firewall. 

**SSH:**

SSH was also not working, either because port 22 was close for us or ssh protocol was blocked for my subnet.
```
ssh: connect to host 2a01:4f9:3080:2d4d::5 port 22: Permission denied
```

**Traceroute:**

Traceroute was performed to do further investigation and yielded these results

```
traceroute to 2a01:4f9:3080:2d4d::5 (2a01:4f9:3080:2d4d::5), 30 hops max, 80 byte packets
 1  2405:201:5015:d815:da78:c9ff:fe7e:5829 (2405:201:5015:d815:da78:c9ff:fe7e:5829)  175.041 ms  174.997 ms  174.971 ms
 2  * * *
 3  2405:200:801:1500::70 (2405:200:801:1500::70)  174.885 ms  174.866 ms  174.847 ms
 4  * * *
 5  2405:200:39b:3168:61::4 (2405:200:39b:3168:61::4)  174.772 ms  174.753 ms  175.056 ms
 6  * * *
 7  2405:200:801:1500::688 (2405:200:801:1500::688)  13.144 ms  13.434 ms 2405:200:801:1500::68c (2405:200:801:1500::68c)  20.575 ms
 8  * * *
 9  * * *
10  * * *
11  * * *
12  ae15-0.fra20.core-backbone.com (2a01:4a0:1338:166::1)  292.689 ms  292.667 ms *
13  ae14-2021.fra10.core-backbone.com (2a01:4a0:0:2021::3)  292.634 ms  204.477 ms  204.438 ms
14  ae1-2081.sth10.core-backbone.com (2a01:4a0:0:2081::34)  204.426 ms  204.639 ms  204.602 ms
15  ae1-2081.sth10.core-backbone.com (2a01:4a0:0:2081::34)  204.589 ms  204.742 ms 2a01:4a0:1338:40::2 (2a01:4a0:1338:40::2)  208.648 ms
16  core53.sto.hetzner.com (2a01:4f8:0:3::3f5)  204.597 ms 2a01:4a0:1338:40::2 (2a01:4a0:1338:40::2)  204.410 ms core52.sto.hetzner.com (2a01:4f8:0:3::3e5)  204.367 ms
17  core53.sto.hetzner.com (2a01:4f8:0:3::3f5)  200.497 ms core52.sto.hetzner.com (2a01:4f8:0:3::3e5)  201.765 ms core53.sto.hetzner.com (2a01:4f8:0:3::3f5)  201.211 ms
18  core32.hel1.hetzner.com (2a01:4f8:0:3::559)  201.694 ms  207.868 ms core31.hel1.hetzner.com (2a01:4f8:0:3::555)  207.610 ms
19  yamuna.fossee.in (2a01:4f9:3080:2d4d::2)  207.586 ms ex9k1.dc8.hel1.hetzner.com (2a01:4f8:0:3::b2)  201.368 ms ex9k1.dc8.hel1.hetzner.com (2a01:4f8:0:3::4e)  201.083 ms
20  yamuna.fossee.in (2a01:4f9:3080:2d4d::2)  201.266 ms !X  207.890 ms !X  207.812 ms !X
```

This show, the firewall configuration we did was blocking the incoming traffic otherwise to be routed to VM.

---

### Solutions tried
1. **Enabled IPV6 forwarding**
    ```
    sysctl -w net.ipv4.ip_forward=1
    ```
    It was already enabled however traffic was still being blocked

2. **Added rule to route traffic to VM**
    ```
    sudo firewall-cmd --zone=public --add-rich-rule='rule family="ipv6" source address=2a01:4f9:3080:2d4d::2 destination address=2a01:4f9:3080:2d4d::/64 accept'

    sudo firewall-cmd --reload
    ```

    This again made no difference the traffic was still being blocked.

3. **Stopping/Reset firewall entirely**
    ```
    sudo firewall-cmd --permanent --reset-to-defaults
    ```
    ```
    sudo systemctl stop firewalld
    ```

    Both have failed to give the required results, now the connection to VM was not only blocked but **enitery blocked.**

    To get the system in the older state I had to do reinstall everything.

4. **Chaning gateway**
    Idea was to add the gateway of host as the gateway of VM. Since the interface of VM is in bridged mode network packets will be routed via this common gateway.

    Unfortunately host gateway had no IPV6. **I will recommend contacting Hetzner and asking them to assign an IPV6 to the gateway.**
