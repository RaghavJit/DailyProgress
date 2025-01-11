#!/bin/bash
firewall-cmd --permanent --new-zone=Team

firewall-cmd --permanent --zone=Team --add-service=http
firewall-cmd --permanent --zone=Team --add-service=https
firewall-cmd --permanent --zone=Team --add-service=smtp
firewall-cmd --permanent --zone=Team --add-service=ssh

firewall-cmd --permanent --zone=Team --add-source=2405:200::/29  #IPv6 Jio Sashi
firewall-cmd --permanent --zone=Team --add-source=2405:201:5000::/40 # added by raghav
firewall-cmd --permanent --zone=Team --add-source=49.43.90.0/24 # added by raghav


firewall-cmd --reload
