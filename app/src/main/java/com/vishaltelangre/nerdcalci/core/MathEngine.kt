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

    private fun preprocessPercentages(expr: String): String {
        var result = expr
        // Order matters: more specific patterns first

        // "20% off 100" → "(100 - 100 * 20 / 100)" = 80
        // "15% off price" → "(price - price * 15 / 100)"
        result = result.replace(Regex("""(\d+(?:\.\d+)?)\s*%\s+off\s+(\d+(?:\.\d+)?|\w+)"""), "($2 - $2 * $1 / 100)")

        // "20% of 100" → "(100 * 20 / 100)" = 20
        // "10% of salary" → "(salary * 10 / 100)"
        result = result.replace(Regex("""(\d+(?:\.\d+)?)\s*%\s+of\s+(\d+(?:\.\d+)?|\w+)"""), "($2 * $1 / 100)")

        // "100 + 20%" → "(100 * (1 + 20 / 100))" = 120
        // "salary + 10%" → "(salary * (1 + 10 / 100))"
        result = result.replace(Regex("""(\d+(?:\.\d+)?|\w+)\s*\+\s*(\d+(?:\.\d+)?)\s*%"""), "($1 * (1 + $2 / 100))")

        // "100 - 15%" → "(100 * (1 - 15 / 100))" = 85
        // "budget - 25%" → "(budget * (1 - 25 / 100))"
        result = result.replace(Regex("""(\d+(?:\.\d+)?|\w+)\s*-\s*(\d+(?:\.\d+)?)\s*%"""), "($1 * (1 - $2 / 100))")

        return result
    }


    /**
     * Calculate results for all lines in a file, maintaining variable state across lines.
     *
     * Processing pipeline:
     * - Strip comments (anything after #)
     * - Normalize Unicode operators (× → *, ÷ → /)
     * - Preprocess percentage expressions (% of, % off, +%, -%)
     * - Parse variable assignment (if present)
     * - Evaluate expression using exp4j
     * - Format result based on type
     *
     * Variables persist across lines in order, so later lines can reference earlier variables:
     *   Line 1: "price = 100"        → result: "100", variables: {price: 100.0}
     *   Line 2: "tax = 10% of price" → result: "10", variables: {price: 100.0, tax: 10.0}
     *   Line 3: "price + tax"        → result: "110", uses both variables
     *
     * Result formatting:
     * - Whole numbers: "100" (not "100.00")
     * - Decimals: "3.33" (2 decimal places)
     * - Very large: "1.23e+15" (scientific notation)
     * - Errors: "Err" (for any invalid expression)
     *
     * @param lines List of line entities to calculate
     * @return List of line entities with populated results
     */
    fun calculate(lines: List<LineEntity>): List<LineEntity> {
        val variables = mutableMapOf<String, Double>()

        return lines.map { line ->
            if (line.expression.isBlank()) return@map line.copy(result = "")

            try {
                // Strip comments first
                val exprWithoutComments = stripComments(line.expression)
                if (exprWithoutComments.isBlank()) return@map line.copy(result = "")

                // Normalize and preprocess the expression
                var processed = normalizeOperators(exprWithoutComments)
                processed = preprocessPercentages(processed)

                // Parse variable assignment (e.g., price = 100)
                val parts = processed.split("=")
                val (varName, exprToEval) = if (parts.size == 2) {
                    parts[0].trim() to parts[1].trim()
                } else {
                    null to processed.trim()
                }

                // Validate variable name if this is an assignment
                if (varName != null && !varName.matches(Regex(Constants.VARIABLE_NAME_PATTERN))) {
                    return@map line.copy(result = "Err")
                }

                // exp4j built-in functions (exclude from undefined variable check)
                // https://redmine.riddler.com.ar/projects/exp4j/wiki/Built_in_Functions
                val builtInFunctions = setOf(
                    "sin", "cos", "tan", "asin", "acos", "atan",
                    "sinh", "cosh", "tanh",
                    "log", "log10", "log2", "log1p",
                    "sqrt", "cbrt", "abs", "floor", "ceil", "signum",
                    "exp", "expm1", "pow", "e", "pi"
                )

                // Validate that expression doesn't contain undefined variables
                // This prevents issues like "rate2" being tokenized as "rate" + "2" by exp4j
                val variablePattern = Regex("""[a-zA-Z_][a-zA-Z0-9_]*""")
                variablePattern.findAll(exprToEval).forEach { match ->
                    val varRef = match.value
                    // Check if this looks like a variable but isn't defined or a built-in function
                    if (!variables.containsKey(varRef) && !builtInFunctions.contains(varRef.lowercase())) {
                        // It's an undefined variable - return error
                        return@map line.copy(result = "Err")
                    }
                }

                // Build expression with exp4j
                val builder = ExpressionBuilder(exprToEval)
                    .implicitMultiplication(false)  // Disable implicit multiplication

                // Add all existing variables to the expression context
                if (variables.isNotEmpty()) {
                    builder.variables(variables.keys.toSet())
                }

                val expression = builder.build()

                // Set variable values from previous lines
                if (variables.isNotEmpty()) {
                    variables.forEach { (key, value) ->
                        expression.setVariable(key, value)
                    }
                }

                val evalResult = expression.evaluate()

                // Store variable if this was an assignment
                if (varName != null) variables[varName] = evalResult

                // Format result for display
                val displayResult = if (evalResult % 1.0 == 0.0) {
                    // Whole number - choose format based on magnitude
                    if (evalResult >= Int.MIN_VALUE && evalResult <= Int.MAX_VALUE) {
                        evalResult.toInt().toString()  // e.g., "100"
                    } else if (evalResult >= Long.MIN_VALUE && evalResult <= Long.MAX_VALUE) {
                        evalResult.toLong().toString()  // e.g., "2000000000"
                    } else {
                        String.format("%.2e", evalResult)  // e.g., "1.23e+15"
                    }
                } else {
                    String.format("%.2f", evalResult)  // e.g., "3.33"
                }

                line.copy(result = displayResult)
            } catch (e: Exception) {
                line.copy(result = "Err")
            }
        }
    }
}
