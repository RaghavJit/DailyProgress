#!/bin/bash
firewall-cmd --permanent --new-zone=VM

firewall-cmd --permanent --zone=VM --add-service=http
firewall-cmd --permanent --zone=VM --add-service=https
firewall-cmd --permanent --zone=VM --add-service=smtp
firewall-cmd --permanent --zone=VM --add-service=ssh

firewall-cmd --permanent --zone=VM --add-source=10.0.0.0/8  #VB VM

firewall-cmd --reload