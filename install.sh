#!/bin/bash

usage() {
    echo "usage: $PROGRAM "
}

tip() {
    printf "%-60s" "$1" > /dev/tty
}

pass() {
    echo -e "\033[32;49;1m [OK] \033[39;49;0m" > /dev/tty
}

fail() {
    echo -e "\033[31;49;1m [FAIL] \033[39;49;0m" > /dev/tty
    exit 1
}

PROGRAM=$0
HOME=$(cd `dirname $`; cd ..; pwd)

while getopts "h" option
do
    case $option in
        h)
            usage
            exit 1
            ;;
        *)
            usage
            exit 1
            ;;
     esac
done

#stop weye service
service weye stop


#setup daemon script
tip "Install weye service ..."
cp ./bin/daemon.sh /etc/init.d/weye
[ $? -eq 0 ] || fail
pass

tip "Change model ..."
chmod 755 /etc/init.d/weye
[ $? -eq 0 ] || fail
pass

#setup weye jar file
tip "Copy weye.jar ..."
cp ./bin/weye.jar /usr/bin/weye.jar
[ $? -eq 0 ] || fail
pass

#setup configuration
tip "mkdir /etc/weye ..."
mkdir -p /etc/weye
[ $? -eq 0 ] || fail
pass

tip "Copy log4j.properties ..."
cp ./conf/log4j.properties /etc/weye/log4j.properties
[ $? -eq 0 ] || fail
pass

tip "Copy web.xml ..."
cp ./conf/web.xml /etc/weye/web.xml
[ $? -eq 0 ] || fail
pass

tip "Copy server.xml ..."
cp ./conf/server.xml /etc/weye/server.xml
[ $? -eq 0 ] || fail
pass

tip "Config weye server ..."
chkconfig weye on
[ $? -eq 0 ] || fail
pass

