#!/bin/bash
firewall-cmd --permanent --new-zone=SysAd

firewall-cmd --permanent --zone=SysAd --add-service=http
firewall-cmd --permanent --zone=SysAd --add-service=https
firewall-cmd --permanent --zone=SysAd --add-service=mysql
firewall-cmd --permanent --zone=SysAd --add-service=smtp
firewall-cmd --permanent --zone=SysAd --add-service=ssh

firewall-cmd --permanent --zone=SysAd --add-source=103.233.64.0/22
firewall-cmd --permanent --zone=SysAd --add-source=111.92.0.0/17
firewall-cmd --permanent --zone=SysAd --add-source=116.68.64.0/18

firewall-cmd --permanent --zone=SysAd --add-source=59.92.0.0/14
firewall-cmd --permanent --zone=SysAd --add-source=117.192.0.0/10
firewall-cmd --permanent --zone=SysAd --add-source=59.160.0.0/13
firewall-cmd --permanent --zone=SysAd --add-source=120.56.0.0/13

firewall-cmd --permanent --zone=SysAd --add-source=137.97.0.0/16
firewall-cmd --permanent --zone=SysAd --add-source=157.32.0.0/12
firewall-cmd --permanent --zone=SysAd --add-source=202.164.128.0/19
firewall-cmd --permanent --zone=SysAd --add-source=202.83.32.0/19
firewall-cmd --permanent --zone=SysAd --add-source=202.88.224.0/19
firewall-cmd --permanent --zone=SysAd --add-source=43.252.92.0/22

#IPv6
firewall-cmd --permanent --zone=SysAd --add-source=2401:4900::/32 #IPv6 Airtel
firewall-cmd --permanent --zone=SysAd --add-source=2409:4000::/22 #IPv6 Jio
firewall-cmd --permanent --zone=SysAd --add-source=2001:4490::/30 #IPv6 BSNL
firewall-cmd --permanent --zone=SysAd --add-source=2406:8800::/32 #IPv6 Asianet

firewall-cmd --reload
