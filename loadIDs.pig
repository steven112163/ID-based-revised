A = load '/tmp/test$a.csv' using PigStorage(',') AS (Asso_ID:chararray, Src_MAC:chararray, Dst_MAC:chararray, Src_IP:chararray, Dst_IP:chararray, Src_port:chararray, Dst_port:chararray, Protocol:chararray, Switch_ID:chararray, Switch_port:chararray, Src_User_ID:chararray, Dst_User_ID:chararray, Access_sw:chararray, Access_port:chararray, Time:chararray);
B = foreach A generate $0 as id;
store B into '/tmp/id$a.out';
