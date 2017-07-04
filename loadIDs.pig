A = load '/tmp/$user/test$n.csv' using PigStorage(',') AS (Date:chararray, Time:chararray);
B = foreach A generate $0 as id;
store B into '/tmp/$user/id$n.out';
