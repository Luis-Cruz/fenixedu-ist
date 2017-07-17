## DiaSemana to DayOfWeek

alter table SUPPORT_LESSON modify WEEK_DAY text default null;
update SUPPORT_LESSON set WEEK_DAY = 'SUNDAY' where WEEK_DAY = '1';
update SUPPORT_LESSON set WEEK_DAY = 'MONDAY' where WEEK_DAY = '2';
update SUPPORT_LESSON set WEEK_DAY = 'TUESDAY' where WEEK_DAY = '3';
update SUPPORT_LESSON set WEEK_DAY = 'WEDNESDAY' where WEEK_DAY = '4';
update SUPPORT_LESSON set WEEK_DAY = 'THURSDAY' where WEEK_DAY = '5';
update SUPPORT_LESSON set WEEK_DAY = 'FRIDAY' where WEEK_DAY = '6';
update SUPPORT_LESSON set WEEK_DAY = 'SATURDAY' where WEEK_DAY = '7';

