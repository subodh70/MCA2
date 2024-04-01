package com.ayush.a2

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.*
class MainActivity : AppCompatActivity() {

    private lateinit var dt: EditText
    private lateinit var yr: EditText
    private lateinit var disView: TextView
    private lateinit var _Button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // setContentView(R.layout.activity_main)

        dt = findViewById(R.id.dateEditText)
        yr = findViewById(R.id.yearEditText)
        disView = findViewById(R.id.displayTextView)
        _Button = findViewById(R.id.fetchButton)

        _Button.setOnClickListener {
            val date = dt.text.toString()
            val year = yr.text.toString()

            fetchWeatherData(date, year)
        }
    }

    private fun fetchWeatherData(date: String, year: String) {
        val apiKey = "MY_KEY"
        val apiUrl = "https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&daily=temperature_2m_max,temperature_2m_min&date=$date-$year&${apiKey}"

        GlobalScope.launch(Dispatchers.IO) {
            val response = StringBuilder()

            try {
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                }

                val jsonResponse = JSONObject(response.toString())
                val dailyForecasts = jsonResponse.getJSONArray("daily")

                // Assuming the first element in the daily forecasts array corresponds to the requested date
                val dailyForecast = dailyForecasts.getJSONObject(0)
                val maxTemp = dailyForecast.getDouble("temperature_2m_max")
                val minTemp = dailyForecast.getDouble("temperature_2m_min")

                runOnUiThread {
                    disView.text = "Max Temp: $maxTemp, Min Temp: $minTemp"
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class WeatherDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private  val VERSION = 1
        private  val NAME = "weather.db"
        private  val TABLENAME = "Data"
        private  val DATE = "date"
        private const val MAXTEMP = "maxtemp"
        private const val MINTEMP = "mintemp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = "CREATE TABLE $TABLE_NAME (" +
                "$DATE TEXT PRIMARY KEY," +
                "$MAXTEMP REAL," +
                "$MINTEMP REAL)"

        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertWeatherData(date: String, maxTemp: Double, minTemp: Double) {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(DATE, date)
        contentValues.put(MAXTEMP, maxTemp)
        contentValues.put(MINTEMP, minTemp)
        db.insert(TABLENAME, null, contentValues)
        db.close()
    }

    fun getWeatherDataByDate(date: String): Pair<Double, Double>? {
        val db = this.readableDatabase
        val query = "SELECT $MAXTEMP, $MINTEMP FROM $TABLE_NAME WHERE $DATE = ?"
        val cursor = db.rawQuery(query, arrayOf(date))
        var maxTemp: Double? = null
        var minTemp: Double? = null

/

        cursor.close()
        db.close()

        return if (maxTemp != null && minTemp != null) Pair(maxTemp!!, minTemp!!) else null
    }

    fun getAverageTemperaturesLastTenYears(date: String): Pair<Double, Double>? {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val years = mutableListOf<Int>()
        for (i in currentYear - 10 until currentYear) {
            years.add(i)
        }

        val db = this.readableDatabase
        val query = "SELECT AVG($MAXTEMP), AVG($MINTEMP) FROM $TABLE_NAME WHERE $DATE LIKE ?"
        val datePattern = date.substring(0, 5) + "%" // Pattern to match month and day ignoring year
        var maxTempSum = 0.0
        var minTempSum = 0.0
        var count = 0

        for (year in years) {
            val cursor = db.rawQuery(query, arrayOf("$datePattern-$year"))
            cursor.use {
                if (cursor.moveToFirst()) {
                    maxTempSum += cursor.getDouble(0)
                    minTempSum += cursor.getDouble(1)
                    count++
                }
            }
        }

        db.close()

        return if (count > 0) Pair(maxTempSum / count, minTempSum / count) else null
    }
}
