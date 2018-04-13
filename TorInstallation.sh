#!/bin/bash
# Tor installation script for running experiments

gpg --keyserver keys.gnupg.net --recv A3C4F0F979CAA22CDBA8F512EE8CBC9E886DDD89
gpg --export A3C4F0F979CAA22CDBA8F512EE8CBC9E886DDD89 | apt-key add -
echo "# 

# deb cdrom:[Debian GNU/Linux 8.10.0 _Jessie_ - Official amd64 CD Binary-1 20171209-21:52]/ jessie main

# deb cdrom:[Debian GNU/Linux 8.10.0 _Jessie_ - Official amd64 CD Binary-1 20171209-21:52]/ jessie main

deb http://ftp.br.debian.org/debian/ jessie main
deb-src http://ftp.br.debian.org/debian/ jessie main

deb http://security.debian.org/ jessie/updates main
deb-src http://security.debian.org/ jessie/updates main

# jessie-updates, previously known as 'volatile'
deb http://ftp.br.debian.org/debian/ jessie-updates main
deb-src http://ftp.br.debian.org/debian/ jessie-updates main

# jessie main tor repositories
deb http://deb.torproject.org/torproject.org jessie main
deb-src http://deb.torproject.org/torproject.org jessie main" > /etc/apt/sources.list

apt update -y
apt install tor deb.torproject.org-keyring -y
apt install tcpdump -y

echo "Installation complete."