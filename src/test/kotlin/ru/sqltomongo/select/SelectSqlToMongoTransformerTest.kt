package ru.sqltomongo.select

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream

internal class SelectSqlToMongoTransformerTest {
    private val transformer = SelectSqlToMongoTransformer()

    @ParameterizedTest
    @MethodSource("provideInput")
    fun fromSqlToMongo(sql: String, mongo: String) {
        val result = transformer.fromSqlToMongo(sql)
        assertEquals(mongo, result)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "UPDATE",
            "SELECT FROM myDB",
            "SELECT *",
            "SELECT * FROM myDB WHERE",
            "SELECT * FROM m WHERE x",
            "SELECT * FROM m LIMIT",
            "SELECT * FROM m OFFSET ",
            "SELECT * FROM m WHEN x = 'name'",
            "SELECT * FROM m WHERE x = 'name' AND x = 'name2'",
            "SELECT * FROM m WHERE x = 3 OR name = 'Hello'",
            "SELECT name, FROM m"
        ]
    )
    fun illegalInputs(sql: String) {
        assertThrows<SqlParseException> { transformer.fromSqlToMongo(sql) }
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        private fun provideInput() = Stream.of(
            Arguments.of("SELECT * FROM persons", "db.persons.find()"),
            Arguments.of("SELECT  * FROM \n persons", "db.persons.find()"),
            Arguments.of("SELECT * FROM persons OFFSET 10", "db.persons.find().skip(10)"),
            Arguments.of("SELECT * FROM persons LIMIT 2", "db.persons.find().limit(2)"),
            Arguments.of("SELECT * FROM persons LIMIT 20 OFFSET 28", "db.persons.find().skip(28).limit(20)"),
            Arguments.of("SELECT * FROM persons OFFSET 28 LIMIT 20 ", "db.persons.find().skip(28).limit(20)"),
            Arguments.of("SELECT name, age FROM persons", "db.persons.find({}, { name: 1, age: 1 } )"),
            Arguments.of("SELECT name FROM persons", "db.persons.find({}, { name: 1 } )"),
            Arguments.of("SELECT * FROM persons WHERE name = 'Michael'", "db.persons.find( { name: \"Michael\" } )"),
            Arguments.of(
                "SELECT * FROM persons WHERE name = 'Michael' AND age < 40",
                "db.persons.find( { name: \"Michael\", age: { \$lt: 40 } } )"
            ),
            Arguments.of(
                "SELECT * FROM persons WHERE name = 'Michael' AND age = 40",
                "db.persons.find( { name: \"Michael\", age: 40 } )"
            ),
            Arguments.of(
                "SELECT * FROM persons WHERE name = 'Michael' AND age < 40 AND age >= 20",
                "db.persons.find( { name: \"Michael\", age: { \$lt: 40, \$gte: 20 } } )"
            ),
            Arguments.of(
                "SELECT name FROM persons WHERE name = 'Michael' AND age < 40 AND age >= 20",
                "db.persons.find( { name: \"Michael\", age: { \$lt: 40, \$gte: 20 } } , { name: 1 } )"
            )
        )
    }
}
