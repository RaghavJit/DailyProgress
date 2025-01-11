#!/bin/bash

firewall-cmd --permanent --new-zone=IITB
firewall-cmd --permanent --zone=IITB --add-source=14.139.97.204/32
firewall-cmd --permanent --zone=IITB --add-source=103.21.124.10/32
firewall-cmd --permanent --zone=IITB --add-source=103.21.126.10/32
firewall-cmd --permanent --zone=IITB --add-source=103.21.124.0/22
firewall-cmd --permanent --zone=IITB --add-service=mysql
firewall-cmd --permanent --zone=IITB --add-service=ssh
firewall-cmd --permanent --zone=IITB --add-service=http
firewall-cmd --permanent --zone=IITB --add-service=https
firewall-cmd --permanent --zone=IITB --add-service=smtp
firewall-cmd --reload
