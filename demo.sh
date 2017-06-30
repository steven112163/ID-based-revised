#!/bin/bash
declare -i s=0
#declare -i c=10
declare -i c=$(mysql -uroot -p'root' portal -se "select count(*) from Association where Asso_ID > $s")
declare -i e=s+c
declare -i n=1
while true
do
    if [ $c -gt 0 ]; then
        mysql -uroot -p'root' portal -se "select * from Association where Asso_ID > $s and Asso_ID <= $e into outfile '/tmp/test$n.csv' fields terminated by ',' enclosed by '\"' lines terminated by '\\n'"
        pig -x local -p a=$n -f loadIDs.pig
        s=$e
        n=$n+1
    fi 
sleep 3
c=$(mysql -uroot -p'root' portal -se "select count(*) from Association where Asso_ID > $s")
e=s+c
done
