package com.aftercup.expertrow

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.children
import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.function.Function
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private val historyList = mutableListOf<String>()
    private lateinit var inputArea: EditText
    private lateinit var resultText: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var keypadContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("ExpertRowData", Context.MODE_PRIVATE)
        loadHistory()

        inputArea = findViewById(R.id.inputArea)
        resultText = findViewById(R.id.resultText)
        keypadContainer = findViewById(R.id.keypadContainer)

        val btnHistory = findViewById<Button>(R.id.btnHistory)
        val btnSettings = findViewById<Button>(R.id.btnSettings)
        val btnToggleKeypad = findViewById<Button>(R.id.btnToggleKeypad)
        val keypadGridBasic = findViewById<GridLayout>(R.id.keypadGridBasic)
        val keypadGridAdvanced = findViewById<GridLayout>(R.id.keypadGridAdvanced)

        inputArea.requestFocus()
        inputArea.showSoftInputOnFocus = true

        applySettings() // Checks if keypad should be entirely hidden

        inputArea.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                actionId == EditorInfo.IME_ACTION_NEXT ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                performCalculation()
                return@setOnEditorActionListener true
            }
            false
        }

        btnHistory.setOnClickListener { showFullscreenHistory() }
        btnSettings.setOnClickListener { showFullscreenSettings() }

        // --- MORE/LESS SYMBOLS LOGIC ---
        btnToggleKeypad.setOnClickListener {
            if (keypadGridAdvanced.visibility == View.VISIBLE) {
                keypadGridAdvanced.visibility = View.GONE
                btnToggleKeypad.text = "MORE SYMBOLS"
            } else {
                keypadGridAdvanced.visibility = View.VISIBLE
                btnToggleKeypad.text = "LESS SYMBOLS"
            }
        }

        // Attach listeners to ALL buttons in both grids
        val allButtons = keypadGridBasic.children + keypadGridAdvanced.children
        for (child in allButtons) {
            if (child is Button) {
                child.setOnClickListener {
                    val tagToAppend = child.tag.toString()
                    appendToInput(tagToAppend)
                }
            }
        }
    }

    private fun applySettings() {
        // Default to true if not found
        val showKeypad = sharedPreferences.getBoolean("show_keypad_setting", true)
        keypadContainer.visibility = if (showKeypad) View.VISIBLE else View.GONE
    }

    private fun appendToInput(textToAppend: String) {
        val start = inputArea.selectionStart.coerceAtLeast(0)
        val end = inputArea.selectionEnd.coerceAtLeast(0)
        inputArea.text.replace(Math.min(start, end), Math.max(start, end), textToAppend, 0, textToAppend.length)
        inputArea.requestFocus()
    }

    private val log10Func = object : Function("log", 1) {
        override fun apply(args: DoubleArray): Double = Math.log10(args[0])
    }

    private val lnFunc = object : Function("ln", 1) {
        override fun apply(args: DoubleArray): Double = Math.log(args[0])
    }

    private val lognFunc = object : Function("logn", 2) {
        override fun apply(args: DoubleArray): Double = Math.log(args[0]) / Math.log(args[1])
    }

    private fun performCalculation() {
        val rawInput = inputArea.text.toString().replace("\n", "").replace(",", ".").trim()
        if (rawInput.isEmpty()) return

        var expressionStr = rawInput.replace(Regex("(?<=\\d)(?=[x(a-z])"), "*")
        expressionStr = expressionStr.replace(Regex("(?<=\\))(?=[\\dxa-z])"), "*")
        expressionStr = expressionStr.replace("÷", "/")
        expressionStr = expressionStr.replace("X", "x")

        try {
            if (expressionStr.contains("=") && expressionStr.contains("x")) {
                solveEquation(expressionStr)
            } else {
                val calcStr = expressionStr.replace("=", "")

                val expression = ExpressionBuilder(calcStr)
                    .function(log10Func)
                    .function(lnFunc)
                    .function(lognFunc)
                    .variables("e")
                    .build()
                    .setVariable("e", Math.E)

                val result = expression.evaluate()
                displayResult(rawInput, result, isEquation = false)
            }
        } catch (e: Exception) {
            resultText.text = "Error"
        }
    }

    private fun solveEquation(equationStr: String) {
        val parts = equationStr.split("=")
        if (parts.size != 2) {
            resultText.text = "Invalid Equation"
            return
        }

        val lhsStr = parts[0]
        val rhsStr = parts[1]
        val combinedFunction = "($lhsStr) - ($rhsStr)"

        try {
            val expression = ExpressionBuilder(combinedFunction)
                .variables("x", "e")
                .function(log10Func)
                .function(lnFunc)
                .function(lognFunc)
                .build()

            expression.setVariable("e", Math.E)

            var x = 1.0
            val maxIterations = 50
            val tolerance = 0.000001
            var found = false

            for (i in 0 until maxIterations) {
                expression.setVariable("x", x)

                val y = try { expression.evaluate() } catch (e: Exception) { Double.NaN }

                if (y.isNaN() || y.isInfinite()) {
                    x = if (x < 0) 1.0 else x + 0.5
                    continue
                }

                if (abs(y) < tolerance) {
                    found = true
                    break
                }

                val h = 0.00001
                expression.setVariable("x", x + h)
                val yPlusH = expression.evaluate()
                val derivative = (yPlusH - y) / h

                if (derivative == 0.0) break

                x = x - (y / derivative)
            }

            if (found) {
                displayResult(equationStr, x, isEquation = true)
            } else {
                resultText.text = "No Solution"
            }
        } catch (e: Exception) {
            resultText.text = "Solver Error"
        }
    }

    private fun displayResult(input: String, result: Double, isEquation: Boolean) {
        if (result.isNaN() || result.isInfinite()) {
            resultText.text = "Error"
            return
        }

        val finalResult = if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            String.format("%.8f", result).trimEnd('0').trimEnd('.')
        }

        if (isEquation) {
            resultText.text = "x = $finalResult"
            addToHistory("$input  =>  x=$finalResult")
        } else {
            resultText.text = "= $finalResult"
            addToHistory("$input = $finalResult")
        }
    }

    private fun addToHistory(entry: String) {
        historyList.add(0, entry)
        if (historyList.size > 20) historyList.removeAt(historyList.size - 1)
        saveHistory()
    }

    private fun saveHistory() {
        val editor = sharedPreferences.edit()
        val historyString = historyList.joinToString("###")
        editor.putString("history_key", historyString)
        editor.apply()
    }

    private fun loadHistory() {
        val historyString = sharedPreferences.getString("history_key", "")
        if (!historyString.isNullOrEmpty()) {
            historyList.clear()
            historyList.addAll(historyString.split("###"))
        }
    }

    private fun showFullscreenHistory() {
        val dialog = Dialog(this, R.style.Dialog_Fullscreen_NoAnim)
        val listView = ListView(this)
        listView.setBackgroundResource(android.R.color.transparent)

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, historyList)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val item = historyList[position]
            val parts = if (item.contains(" => ")) item.split(" => ") else item.split(" = ")

            if (parts.isNotEmpty()) {
                inputArea.setText(parts[0].trim())
                inputArea.setSelection(inputArea.text.length)
            }
            dialog.dismiss()
        }

        dialog.setContentView(listView)
        dialog.show()
    }

    private fun showFullscreenSettings() {
        val dialog = Dialog(this, R.style.Dialog_Fullscreen_NoAnim)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        dialog.setContentView(dialogView)

        val switchTheme = dialogView.findViewById<Switch>(R.id.switchTheme)
        val switchShowKeypad = dialogView.findViewById<Switch>(R.id.switchShowKeypad)
        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseSettings)

        // Theme Toggle logic
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        switchTheme.isChecked = currentNightMode == Configuration.UI_MODE_NIGHT_YES

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Keypad Master Toggle logic
        switchShowKeypad.isChecked = sharedPreferences.getBoolean("show_keypad_setting", true)

        switchShowKeypad.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("show_keypad_setting", isChecked).apply()
        }

        btnClose.setOnClickListener {
            applySettings() // Applies changes instantly to the main screen when closed
            dialog.dismiss()
        }

        dialog.show()
    }
}