#!/bin/bash
declare -a arr
declare -i s=0
#declare -i c=10
declare -i c=$(mysql -uroot -p'root' portal -se "select count(*) from Association where Asso_ID > $s")
declare -i e=s+c
declare -i n=0
declare -i exists
while true
do
    arr=()
    while read -a line
    do
        arr+=($line)
    done < <(mysql -uroot -p'root' portal -se "select User_ID from Registered_MAC where User_ID != \"\"")

    if [ $c -gt 0 ]; then
        for i in ${arr[@]}
        do
            mkdir -p /tmp/$i -m 777
            n=$(ls /tmp/$i | grep "^test" | wc -l)+1

            exists=$(mysql -uroot -p'root' portal -se "select if (exists(select * from Association where Asso_ID > $s \
            and Asso_ID <= $e and Src_User_ID = $i), 1, 0)")

            if [ $exists -eq 1 ]; then
                mysql -uroot -p'root' portal -se "select Src_User_ID, Date, Time from Association where Asso_ID > $s \
                and Asso_ID <= $e and Switch_ID = Access_sw and Switch_port = Access_port and Src_User_ID = $i \
                into outfile '/tmp/$i/test$n.csv' fields terminated by ',' enclosed by '\"' lines terminated by '\\n'"
            fi

            #n=$n+1
        done

        #pig -x local -p a=$n -f loadIDs.pig
        s=$e
        #n=$n+1
    fi 
    sleep 3
    c=$(mysql -uroot -p'root' portal -se "select count(*) from Association where Asso_ID > $s")
    e=s+c
done
