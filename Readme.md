# Q1


It fetches the data of Boston (US) of past years from : https://open-meteo.com/en/docs#hourly=&daily=temperature_2m_max,temperature_2m_min&time_mode=time_interval

user will eneter the date (there is a restriction that user can fetch data of past 10 years only i.e from 2014 till current year)

the maximum and minimum temperature gets displayed 
(if no internet connection then error message gets displayed)

# Q2

Implementation is same as above , but here SQLite is used to to store data in database.

A table / schema is created , and using query : "select maxtemp , min temp from <table name>" is used

When dte is in future it takes avaergae of past 10 years of amx and mijn temperature