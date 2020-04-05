package ru.sqltomongo.select

fun main() {
    val transformer = SelectSqlToMongoTransformer()
    val input = readLine()!!
    try {
        val output = transformer.fromSqlToMongo(input)
        println(output)
    } catch (e: SqlParseException) {
        println(e.message)
    }
}
