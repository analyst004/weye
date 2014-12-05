#!/bin/sh
#
# chkconfig: 2345 80 50
# description: weye daemon script
#
# weye
#
. /etc/rc.d/init.d/functions

NAME="weye"
PID_FILE="/var/run/weye.pid"

ret=0
case $1 in
    start)
        echo "start ${NAME} ..."
        if [ ! -f $PID_FILE ]; then
            nohup java -jar /usr/bin/weye.jar &
            echo $! > $PID_FILE
            echo "${NAME} started ..."
        else
            echo "${NAME} is already running ..."
        fi
    ;;
    stop)
        if [ -f $PID_FILE ]; then
            PID=$(cat $PID_FILE);
            echo "Stopping ${NAME} ..."
            kill $PID;
            echo "${NAME} stopping ..."
            rm $PID_FILE
        else
            echo "${NAME} is not running ..."
        fi
    ;;
    restart)
        if [ -f $PID_FILE ]; then
            PID=$(cat $PID_FILE);
            echo "Stopping ${NAME} ...";
            kill $PID;
            echo "${NAME} stopped ...";
            rm $PID_FILE

            echo "Starting ${NAME} ..."
            nohup java -jar /usr/bin/weye.jar &
            echo $! > $PID_FILE
            echo "${NAME} started ..."
        else
            echo "${NAME} is not running ..."
        fi
     ;;
esac