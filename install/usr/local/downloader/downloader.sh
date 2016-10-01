#! /bin/sh
CONFIG=/usr/local/downloader/downloader.properties
DB=/usr/local/downloader/mysql.properties
PROCESS="/usr/java/latest/bin/java -jar /usr/local/downloader/downloader.jar"
$PROCESS $CONFIG $DB