package com.vishaltelangre.nerdcalci.ui.help

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vishaltelangre.nerdcalci.ui.theme.FiraCodeFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Help", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            HelpSection(title = "Basic Calculations") {
                DescriptionText("Perform calculations line by line:")
                CodeBlock("2 + 3\n5 * 4   # × works too\n10 / 2  # ÷ works too")
            }

            HelpSection(title = "Variables") {
                DescriptionText("Assign values to variables and reuse them:")
                CodeBlock("a = 10\nb = 20\na + b")
            }

            HelpSection(title = "Percentages") {
                DescriptionText("Calculate percentages easily:")
                CodeBlock("20% of 500\n15% off 1000\n50000 + 10%\n50000 - 5%")
            }

            HelpSection(title = "Comments") {
                DescriptionText("Add comments using # symbol:")
                CodeBlock("# Price calculations:\nprice = 100  # base price\nprice * 1.18  # with 18% tax")
            }

            HelpSection(title = "Mathematical Functions") {
                Column {
                    DescriptionText("Use built-in math functions:")
                    CodeBlock("sqrt(16)   # Square root: 4\nabs(-42)   # Absolute: 42\nfloor(3.7) # Round down: 3\nceil(3.2)  # Round up: 4\npow(2, 8)  # Power: 256")

                    Spacer(modifier = Modifier.height(16.dp))

                    DescriptionText("Constants:")
                    CodeBlock("pi()  # 3.14\ne()   # 2.71")

                    Spacer(modifier = Modifier.height(16.dp))

                    DescriptionText("Trigonometry (angles in radians):")
                    CodeBlock("sin(pi()/2)  # 1\ncos(0)       # 1")

                    Spacer(modifier = Modifier.height(16.dp))

                    DescriptionText("Logarithms:")
                    CodeBlock("log10(1000)  # Base 10: 3\nlog2(8)      # Base 2: 3\nlog(e())     # Natural log: 1")

                    Spacer(modifier = Modifier.height(16.dp))

                    val linkText = buildAnnotatedString {
                        append("Find the full list of all available mathematical functions in the ")
                        val url = "https://redmine.riddler.com.ar/projects/exp4j/wiki/Built_in_Functions"
                        withLink(LinkAnnotation.Url(url)) {
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append("exp4j documentation")
                            }
                        }
                        append(".")
                    }
                    Text(
                        text = linkText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HelpSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DescriptionText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun CodeBlock(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
            .horizontalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FiraCodeFamily
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
