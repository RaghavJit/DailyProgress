#!/bin/bash

firewall-cmd --permanent --new-zone=FOSSEE
firewall-cmd --permanent --zone=FOSSEE --add-source=88.99.101.85/32 #Neptune
firewall-cmd --permanent --zone=FOSSEE --add-source=144.76.40.203/32 #Primary
firewall-cmd --permanent --zone=FOSSEE --add-source=144.76.40.204/32 #Primary
firewall-cmd --permanent --zone=FOSSEE --add-source=135.181.136.155/32 #Jupiter
firewall-cmd --permanent --zone=FOSSEE --add-source=95.217.115.241/32 #Saturn
firewall-cmd --permanent --zone=FOSSEE --add-source=135.181.136.137/32 #Zeus
firewall-cmd --permanent --zone=FOSSEE --add-source=65.108.196.217/32 #Megara
firewall-cmd --permanent --zone=FOSSEE --add-source=2a01:4f9:1a:98e2::2/128 #Megara IPv6
firewall-cmd --permanent --zone=FOSSEE --add-source=2a01:4f9:3a:10a4::4/128 #Zeus IPv6
firewall-cmd --permanent --zone=FOSSEE --add-service=mysql
firewall-cmd --permanent --zone=FOSSEE --add-service=ssh
firewall-cmd --permanent --zone=FOSSEE --add-service=http
firewall-cmd --permanent --zone=FOSSEE --add-service=https
firewall-cmd --permanent --zone=FOSSEE --add-service=smtp

firewall-cmd --reload