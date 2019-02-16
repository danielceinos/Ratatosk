package com.danielceinos.ratatosk

import android.content.SharedPreferences
import java.util.UUID

class RatatoskStorage(private val sharedPreferences: SharedPreferences) {

  private val UUID_KEY = "UUID_KEY"

  private fun genUUID(): String {
    val uuid = UUID.randomUUID()
        .toString()
    sharedPreferences.edit()
        .putString(UUID_KEY, uuid)
        .apply()
    return uuid
  }

  fun getUUID(): String = sharedPreferences.getString(UUID_KEY, null) ?: genUUID()
}