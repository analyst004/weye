#!/bin/sh
#
# chkconfig: 2345 80 50
# description: weye daemon script
#
# weye
#
. /etc/rc.d/init.d/functions

ret=0

start() {

    echo "start weye"
    daemon /usr/bin/weye.jar &
    ret=$?
}

stop() {
    echo "stop weye"
    kill -9 $(ps -ef | grep myrand | grep -v grep | awk '{print $2}')
    ret=$?
}

status() {
    local result
    echo "check status of weye ..."
    result=$( ps -ef | grep myrand | grep -v myrandservice | grep -v grep | wc -l )
    if [ $result -gt 0 ]; then
        echo "weye is up"
        ret=0
    else
        echo "weye is down"
        ret=1
    fi
    echo "check status of weye ... done."
}