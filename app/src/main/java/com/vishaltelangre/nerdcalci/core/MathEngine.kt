package com.vishaltelangre.nerdcalci.core

import com.vishaltelangre.nerdcalci.data.local.entities.LineEntity
import net.objecthunter.exp4j.ExpressionBuilder

object MathEngine {
    // Strip comments (anything after #)
    private fun stripComments(expr: String): String {
        val hashIndex = expr.indexOf('#')
        return if (hashIndex >= 0) {
            expr.substring(0, hashIndex).trim()
        } else {
            expr
        }
    }

    // Normalize Unicode operators to standard ASCII
    private fun normalizeOperators(expr: String): String {
        return expr
            .replace("×", "*")  // Multiplication sign → asterisk
            .replace("÷", "/")  // Division sign → slash
    }

    // Preprocess percentage expressions
    private fun preprocessPercentages(expr: String): String {
        var result = expr
        // Order matters: more specific patterns first
        // Accept both numbers and variable names (e.g., "22% of 1000" or "22% of annualSalary")
        result = result.replace(Regex("""(\d+(?:\.\d+)?)\s*%\s+off\s+(\d+(?:\.\d+)?|\w+)"""), "($2 - $2 * $1 / 100)")
        result = result.replace(Regex("""(\d+(?:\.\d+)?)\s*%\s+of\s+(\d+(?:\.\d+)?|\w+)"""), "($2 * $1 / 100)")
        result = result.replace(Regex("""(\d+(?:\.\d+)?|\w+)\s*\+\s*(\d+(?:\.\d+)?)\s*%"""), "($1 * (1 + $2 / 100))")
        result = result.replace(Regex("""(\d+(?:\.\d+)?|\w+)\s*-\s*(\d+(?:\.\d+)?)\s*%"""), "($1 * (1 - $2 / 100))")
        return result
    }

    // Preprocess variable names (replace spaces with underscores for exp4j)
    private fun preprocessVariableNames(expr: String): String {
        return expr.replace(Regex("""([a-zA-Z][a-zA-Z0-9\s]+?)(\s*[=+\-*/^()])""")) { matchResult ->
            val varName = matchResult.groupValues[1].trim()
            val operator = matchResult.groupValues[2]
            varName.replace(" ", "_") + operator
        }
    }

    fun calculate(lines: List<LineEntity>): List<LineEntity> {
        val variables = mutableMapOf<String, Double>()

        return lines.map { line ->
            if (line.expression.isBlank()) return@map line.copy(result = "")

            try {
                // Strip comments first
                val exprWithoutComments = stripComments(line.expression)
                if (exprWithoutComments.isBlank()) return@map line.copy(result = "")

                var processed = normalizeOperators(exprWithoutComments)
                // Preprocess variable names (spaces -> underscores)
                processed = preprocessVariableNames(processed)
                // Then preprocess percentages
                processed = preprocessPercentages(processed)

                // Handle variable assignment (e.g., price = 100)
                val parts = processed.split("=")
                val (varName, exprToEval) = if (parts.size == 2) {
                    parts[0].trim() to parts[1].trim()
                } else {
                    null to processed.trim()
                }

                val builder = ExpressionBuilder(exprToEval)

                // Add all existing variables to the expression
                if (variables.isNotEmpty()) {
                    builder.variables(variables.keys.toSet())
                }

                val expression = builder.build()

                // Set variable values if any exist
                if (variables.isNotEmpty()) {
                    variables.forEach { (key, value) ->
                        expression.setVariable(key, value)
                    }
                }

                val evalResult = expression.evaluate()

                // Update variable map if assignment exists
                if (varName != null) variables[varName] = evalResult

                val displayResult = if (evalResult % 1.0 == 0.0) {
                    // Check if result fits in Int range to avoid overflow
                    if (evalResult >= Int.MIN_VALUE && evalResult <= Int.MAX_VALUE) {
                        evalResult.toInt().toString()
                    } else if (evalResult >= Long.MIN_VALUE && evalResult <= Long.MAX_VALUE) {
                        // Use Long for larger whole numbers
                        evalResult.toLong().toString()
                    } else {
                        // Use scientific notation for very large numbers
                        String.format("%.2e", evalResult)
                    }
                } else {
                    String.format("%.2f", evalResult)
                }

                line.copy(result = displayResult)
            } catch (e: Exception) {
                line.copy(result = "Err")
            }
        }
    }
}
