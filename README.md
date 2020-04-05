# SelectSqlToMongo
### Простой транслятор из SQL в Mongo

Использование:
* gradle build
* запустить функцию main в main.kt - на вход ожидается одна SQL строка

Ограничения:
* только SELECT
* поддериваются skip, limit, фильтрация и проекция
* в фильтрации разрешены только <, <=, =, >, >=
* в фильтрации разрешен только оператор AND
* если в фильтрации есть условие с =, все остальные проверки игнорируются(два сравнения на равенство запрещены)
