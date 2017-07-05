#!/bin/bash
declare -a arr
declare -i s=0
#declare -i c=10
declare -i c=$(mysql -uroot -p'root' portal -se "select count(*) from Association where Asso_ID > $s")
declare -i e=s+c
declare -i out_n
declare -i in_n
declare -i exists
declare -i inBytes
declare -i outBytes
while true
do
    arr=()
    while read -a line
    do
        arr+=($line)
    done < <(mysql -uroot -p'root' portal -se "select distinct User_ID from Registered_MAC where User_ID != \"\"")

    if [ $c -gt 0 ]; then
        for i in ${arr[@]}
        do
            mkdir -p /tmp/result/$i/out -m 777
            out_n=$(ls /tmp/result/$i/out | grep "^test" | wc -l)+1

            exists=$(mysql -uroot -p'root' portal -se "select if (exists(select * from Association where Asso_ID > $s \
            and Asso_ID <= $e and Src_User_ID = $i), 1, 0)")

            if [ $exists -eq 1 ]; then
                mysql -uroot -p'root' portal -se "select Date, Time, (select Location from Switch where \
                Switch_ID = Src_access_sw), Bytes from Association where Asso_ID > $s \
                and Asso_ID <= $e and Switch_ID = Src_access_sw and Switch_port = Src_access_port and Src_User_ID = $i \
                and Bytes != 0 into outfile '/tmp/result/$i/out/test$out_n.csv' fields terminated by ',' \
                enclosed by '\"' lines terminated by '\\n'"
            fi


            mkdir -p /tmp/result/$i/in -m 777
            in_n=$(ls /tmp/result/$i/in | grep "^test" | wc -l)+1

            exists=$(mysql -uroot -p'root' portal -se "select if (exists(select * from Association where Asso_ID > $s \
            and Asso_ID <= $e and Dst_User_ID = $i), 1, 0)")

            if [ $exists -eq 1 ]; then
                mysql -uroot -p'root' portal -se "select Date, Time, (select Location from Switch where \
                Switch_ID = Dst_access_sw), Bytes from Association where Asso_ID > $s and Bytes != 0 \
                and Asso_ID <= $e and Switch_ID = Dst_access_sw and Dst_User_ID = $i \
                into outfile '/tmp/result/$i/in/test$in_n.csv' fields terminated by ',' \
                enclosed by '\"' lines terminated by '\\n'"
            fi

        done

        #pig -x local -p a=$n -f loadIDs.pig
        s=$e
    fi
    sleep 3
    c=$(mysql -uroot -p'root' portal -se "select count(*) from Association where Asso_ID > $s")
    e=s+c
done
