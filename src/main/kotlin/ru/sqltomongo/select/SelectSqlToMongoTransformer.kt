package ru.sqltomongo.select

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import ru.sqltomongo.select.parser.GrammarBaseVisitor
import ru.sqltomongo.select.parser.GrammarLexer
import ru.sqltomongo.select.parser.GrammarParser

class SelectSqlToMongoTransformer {
    fun fromSqlToMongo(sql: String): String {
        val lexer = GrammarLexer(CharStreams.fromString(sql))
        val parser = GrammarParser(CommonTokenStream(lexer))
        parser.selectCommand().accept(SqlVisitor())
        val skip = if (offsetNumber == null) "" else ".skip($offsetNumber)"
        val limit = if (limitNumber == null) "" else ".limit($limitNumber)"
        val fields = if (fieldsList.isEmpty()) "" else
            fieldsList.joinToString(", ", prefix = " { ", postfix = " } ") { "$it: 1" }
        val where = when {
            fields.isEmpty() && whereString == null -> ""
            fields.isEmpty() && whereString != null -> whereString
            fields.isNotEmpty() && whereString == null -> "{},"
            else -> "$whereString,"
        }
        return "db.$tableName.find($where$fields)$skip$limit"
    }

    private lateinit var tableName: String
    private var limitNumber: Int? = null
    private var offsetNumber: Int? = null
    private var whereString: String? = null
    private val fieldsList = mutableListOf<String>()
    private val conditions = hashMapOf<String, MutableList<String>>()

    private inner class SqlVisitor : GrammarBaseVisitor<Int>() {
        override fun visitLimitNumber(ctx: GrammarParser.LimitNumberContext): Int {
            limitNumber = ctx.int()
            return 0
        }

        override fun visitOffsetNumber(ctx: GrammarParser.OffsetNumberContext): Int {
            offsetNumber = ctx.int()
            return 0
        }

        override fun visitTableName(ctx: GrammarParser.TableNameContext): Int {
            tableName = ctx.tableName()
            return 0
        }

        override fun visitSelectCommand(ctx: GrammarParser.SelectCommandContext): Int {
            ctx.tableName()?.accept(this) ?: throw SqlParseException("Table name is not specified.")
            ctx.fields()?.accept(this) ?: throw SqlParseException("Fields are missed")
            ctx.limitations()?.accept(this)
            ctx.conditions()?.accept(this)
            ctx.EOF() ?: throw SqlParseException("Unknown token")
            return 0
        }

        override fun visitLimitations(ctx: GrammarParser.LimitationsContext): Int {
            ctx.limit()?.limitNumber()?.accept(this)
            ctx.offset()?.offsetNumber()?.accept(this)
            return 0
        }

        override fun visitFields(ctx: GrammarParser.FieldsContext): Int {
            fieldsList.addAll(ctx.fields())
            return 0
        }

        override fun visitConditions(ctx: GrammarParser.ConditionsContext): Int {
            ctx.conditionsList.forEach { it.accept(this) }
            if (conditions.isEmpty()) return 0
            whereString = conditions.mapValues {
                val equalityConditions = it.value.filter { c -> c.startsWith("\$eq") }
                when {
                    equalityConditions.isEmpty() -> it.value.joinToString(", ", prefix = "{ ", postfix = " }")
                    equalityConditions.size == 1 -> equalityConditions[0].substring(5)
                    else -> throw SqlParseException("Too many equality limitations!")
                }
            }
                .map { "${it.key}: ${it.value}" }
                .joinToString(", ", prefix = " { ", postfix = " } ")
            return 0
        }

        override fun visitCondition(ctx: GrammarParser.ConditionContext): Int {
            val field = ctx.field()?.fieldName() ?: throw SqlParseException("Field name expected")
            val value = ctx.operand()?.value() ?: throw SqlParseException("Value expected")
            val operation = ctx.operation()
            if (!conditions.containsKey(field)) {
                conditions[field] = mutableListOf()
            }
            conditions[field]!!.add("$operation: $value")
            return 0
        }
    }
}

private fun GrammarParser.LimitNumberContext.int() =
    this.NUMBER().text.toIntOrNull() ?: throw SqlParseException("Int expected")

private fun GrammarParser.OffsetNumberContext.int() =
    this.NUMBER().text.toIntOrNull() ?: throw SqlParseException("Int expected")

private fun GrammarParser.TableNameContext.tableName() = this.STRING().text
private fun GrammarParser.FieldContext.fieldName() = this.STRING().text
private fun GrammarParser.FieldsContext.fields() = this.fieldsList.map { it.fieldName() }
private fun GrammarParser.OperandContext.value() = when {
    this.quotes() != null -> "\"${this.quotes().STRING().text}\""
    else -> this.NUMBER()?.text
}

private fun GrammarParser.ConditionContext.operation() = when (OP().text) {
    "=" -> "\$eq"
    ">=" -> "\$gte"
    ">" -> "\$gt"
    "<=" -> "\$lte"
    "<" -> "\$lt"
    else -> throw SqlParseException("Unexpected operation: ${OP()}")
}
