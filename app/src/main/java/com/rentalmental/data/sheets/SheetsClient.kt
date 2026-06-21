package com.rentalmental.data.sheets

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SheetsClient(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getAccessToken(accountName: String): String {
        val account = Account(accountName, "com.google")
        return GoogleAuthUtil.getToken(context, account, "oauth2:https://www.googleapis.com/auth/spreadsheets")
    }

    fun createSpreadsheet(accountName: String, title: String): String {
        val token = getAccessToken(accountName)
        val body = JSONObject().apply {
            put("properties", JSONObject().put("title", title))
        }
        val request = Request.Builder()
            .url("https://sheets.googleapis.com/v4/spreadsheets")
            .addHeader("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response creating spreadsheet")
        if (!response.isSuccessful) throw Exception("Failed to create spreadsheet: $responseBody")

        val spreadsheetId = JSONObject(responseBody).getString("spreadsheetId")

        val headerBody = JSONObject().apply {
            put("values", JSONArray().put(JSONArray().apply {
                put("Date/Time")
                put("Hindi Text")
                put("English Translation")
            }))
        }
        val headerRequest = Request.Builder()
            .url("https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/Sheet1!A1:C1?valueInputOption=RAW")
            .addHeader("Authorization", "Bearer $token")
            .put(headerBody.toString().toRequestBody(jsonMediaType))
            .build()
        httpClient.newCall(headerRequest).execute().close()

        return spreadsheetId
    }

    fun addSheet(accountName: String, spreadsheetId: String, sheetName: String) {
        val token = getAccessToken(accountName)
        val body = JSONObject().apply {
            put("requests", JSONArray().put(JSONObject().apply {
                put("addSheet", JSONObject().apply {
                    put("properties", JSONObject().put("title", sheetName))
                })
            }))
        }
        val request = Request.Builder()
            .url("https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId:batchUpdate")
            .addHeader("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (!response.isSuccessful) throw Exception("Failed to add sheet: $responseBody")
        response.close()

        val headerBody = JSONObject().apply {
            put("values", JSONArray().put(JSONArray().apply {
                put("Date/Time")
                put("Hindi Text")
                put("English Translation")
            }))
        }
        val headerRequest = Request.Builder()
            .url("https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/'${sheetName}'!A1:C1?valueInputOption=RAW")
            .addHeader("Authorization", "Bearer $token")
            .put(headerBody.toString().toRequestBody(jsonMediaType))
            .build()
        httpClient.newCall(headerRequest).execute().close()
    }

    fun insertRow(
        accountName: String,
        spreadsheetId: String,
        sheetName: String,
        dateTime: String,
        hindiText: String,
        englishText: String
    ) {
        val token = getAccessToken(accountName)

        val sheetId = getSheetId(token, spreadsheetId, sheetName)

        val insertBody = JSONObject().apply {
            put("requests", JSONArray().put(JSONObject().apply {
                put("insertDimension", JSONObject().apply {
                    put("range", JSONObject().apply {
                        put("sheetId", sheetId)
                        put("dimension", "ROWS")
                        put("startIndex", 1)
                        put("endIndex", 2)
                    })
                    put("inheritFromBefore", false)
                })
            }))
        }
        val insertRequest = Request.Builder()
            .url("https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId:batchUpdate")
            .addHeader("Authorization", "Bearer $token")
            .post(insertBody.toString().toRequestBody(jsonMediaType))
            .build()
        val insertResponse = httpClient.newCall(insertRequest).execute()
        if (!insertResponse.isSuccessful) {
            val err = insertResponse.body?.string() ?: ""
            throw Exception("Failed to insert row: $err")
        }
        insertResponse.close()

        val valueBody = JSONObject().apply {
            put("values", JSONArray().put(JSONArray().apply {
                put(dateTime)
                put(hindiText)
                put(englishText)
            }))
        }
        val valueRequest = Request.Builder()
            .url("https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/'${sheetName}'!A2:C2?valueInputOption=RAW")
            .addHeader("Authorization", "Bearer $token")
            .put(valueBody.toString().toRequestBody(jsonMediaType))
            .build()
        val valueResponse = httpClient.newCall(valueRequest).execute()
        if (!valueResponse.isSuccessful) {
            val err = valueResponse.body?.string() ?: ""
            throw Exception("Failed to write row data: $err")
        }
        valueResponse.close()
    }

    fun renameSheet(accountName: String, spreadsheetId: String, oldName: String, newName: String) {
        val token = getAccessToken(accountName)
        val sheetId = getSheetId(token, spreadsheetId, oldName)
        val body = JSONObject().apply {
            put("requests", JSONArray().put(JSONObject().apply {
                put("updateSheetProperties", JSONObject().apply {
                    put("properties", JSONObject().apply {
                        put("sheetId", sheetId)
                        put("title", newName)
                    })
                    put("fields", "title")
                })
            }))
        }
        val request = Request.Builder()
            .url("https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId:batchUpdate")
            .addHeader("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val err = response.body?.string() ?: ""
            throw Exception("Failed to rename sheet: $err")
        }
        response.close()
    }

    private fun getSheetId(token: String, spreadsheetId: String, sheetName: String): Int {
        val request = Request.Builder()
            .url("https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId?fields=sheets.properties")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()
        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response getting sheet info")
        if (!response.isSuccessful) throw Exception("Failed to get sheet info: $body")

        val sheets = JSONObject(body).getJSONArray("sheets")
        for (i in 0 until sheets.length()) {
            val props = sheets.getJSONObject(i).getJSONObject("properties")
            if (props.getString("title") == sheetName) {
                return props.getInt("sheetId")
            }
        }
        throw Exception("Sheet '$sheetName' not found in spreadsheet")
    }
}
