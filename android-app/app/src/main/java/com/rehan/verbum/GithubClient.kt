package com.rehan.verbum

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request

data class Manifest(
    val date: String,
    val cards: List<CardEntry>,
)

data class CardEntry(
    val word: String,
    val file: String,
    val ipa: String,
    val pos: String,
    val definition: String,
)

class GithubClient(
    private val owner: String,
    private val repo: String,
    private val token: String,
) {
    private val http = OkHttpClient()
    private val gson = Gson()
    private val branch = "daily"
    private val base = "https://raw.githubusercontent.com/$owner/$repo/$branch/output"

    private fun get(url: String): ByteArray {
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "token $token")
            .header("User-Agent", "Verbum/1.0")
            .build()
        val resp = http.newCall(req).execute()
        check(resp.isSuccessful) { "HTTP ${resp.code} for $url" }
        return resp.body!!.bytes()
    }

    fun fetchManifest(): Manifest {
        val json = get("$base/manifest.json").toString(Charsets.UTF_8)
        return gson.fromJson(json, Manifest::class.java)
    }

    fun fetchImage(filename: String): ByteArray = get("$base/$filename")
}
