#!/bin/bash
mysql -uroot -p'root' portal<<EOFMYSQL
delimiter |
drop procedure if exists q;
create procedure q()
begin
set @s = 0;
set @c = (select count(*) from Association where Asso_ID > @s);
set @e = @s + @c;
set @n = 1;
while 1 do
if @c > 0 then
set @a = concat("select * from Association where Asso_ID > @s and Asso_ID <= @e into outfile ", "'/tmp/test",@n,".csv'", " fields terminated by ',' enclosed by '\"' lines terminated by '\\n'");
prepare ex1 from @a;
execute ex1;
deallocate prepare ex1;
set @s = @e;
set @n = @n + 1;
end if;
select sleep(3);
set @c = (select count(*) from Association where Asso_ID > @s);
set @e = @s + @c;
end while;
end;
|
call q;
EOFMYSQL
