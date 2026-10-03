package com.example.budgettracker.data.backup

import com.example.budgettracker.domain.Money
import com.example.budgettracker.domain.localDateOf
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object BackupCodec {
    const val CURRENT_VERSION = 2

    private val gson = Gson()

    fun encode(data: BackupData): String = gson.toJson(data.copy(version = CURRENT_VERSION))

    fun decode(json: String): DecodedBackup {
        val root = JsonParser.parseString(json).asJsonObject
        val legacy = !root.has("version") || root.get("version").asInt < CURRENT_VERSION
        if (legacy) convertLegacyPesos(root)
        ensureArray(root, "accounts")
        ensureArray(root, "categories")
        ensureArray(root, "transactions")
        ensureArray(root, "budgetLimits")
        ensureArray(root, "savingsGoals")
        ensureArray(root, "recurringTransactions")
        ensureArray(root, "debts")
        ensureArray(root, "templates")
        fillPreferenceDefaults(root)
        root.addProperty("version", CURRENT_VERSION)
        val data = gson.fromJson(root, BackupData::class.java)
        return DecodedBackup(data, legacy)
    }

    private fun convertLegacyPesos(root: JsonObject) {
        scale(root, "accounts", "balance")
        root.array("accounts")?.forEachObject { account ->
            if (!account.has("includeInTotalBalance")) account.addProperty("includeInTotalBalance", true)
            if (!account.has("openingBalance")) account.addProperty("openingBalance", 0)
        }
        root.array("categories")?.forEachObject { category ->
            if (!category.has("role") || category.get("role").isJsonNull) {
                val name = category.get("name")?.asString
                val type = category.get("type")?.asString
                val role = when {
                    name == "Withdraw / Transfer Out" && type == "EXPENSE" -> "TRANSFER_OUT"
                    name == "Deposit / Transfer In" && type == "INCOME" -> "TRANSFER_IN"
                    else -> "NORMAL"
                }
                category.addProperty("role", role)
            }
        }
        scale(root, "budgetLimits", "assignedAmount")
        scale(root, "transactions", "amount")
        scale(root, "savingsGoals", "targetAmount", "currentAmount", "contributionAmount")
        scale(root, "recurringTransactions", "amount")
        root.array("recurringTransactions")?.forEachObject { rule ->
            if (!rule.has("anchorDay") || rule.get("anchorDay").isJsonNull) {
                val start = rule.get("startDate")?.takeIf { !it.isJsonNull }?.asLong ?: 0L
                val day = if (start > 0L) localDateOf(start).dayOfMonth else 1
                rule.addProperty("anchorDay", day)
            }
            if (!rule.has("paused") || rule.get("paused").isJsonNull) {
                rule.addProperty("paused", false)
            }
        }
        scale(root, "debts", "amount")
        scale(root, "templates", "amount")
    }

    private fun scale(root: JsonObject, arrayName: String, vararg fields: String) {
        root.array(arrayName)?.forEachObject { obj ->
            for (field in fields) {
                if (!obj.has(field) || obj.get(field).isJsonNull) continue
                obj.addProperty(field, Money.fromDoublePesos(obj.get(field).asDouble))
            }
        }
    }

    private fun fillPreferenceDefaults(root: JsonObject) {
        if (!root.has("preferences") || root.get("preferences").isJsonNull || !root.get("preferences").isJsonObject) return
        val prefs = root.getAsJsonObject("preferences")
        if (!prefs.has("accent") || prefs.get("accent").isJsonNull) prefs.addProperty("accent", "EMERALD")
        if (!prefs.has("dynamicColor") || prefs.get("dynamicColor").isJsonNull) prefs.addProperty("dynamicColor", false)
        if (!prefs.has("reminderHour") || prefs.get("reminderHour").isJsonNull) prefs.addProperty("reminderHour", 20)
    }

    private fun ensureArray(root: JsonObject, name: String) {
        if (!root.has(name) || root.get(name).isJsonNull) {
            root.add(name, JsonArray())
        }
    }

    private fun JsonObject.array(name: String): JsonArray? {
        if (!has(name) || get(name).isJsonNull || !get(name).isJsonArray) return null
        return getAsJsonArray(name)
    }

    private fun JsonArray.forEachObject(block: (JsonObject) -> Unit) {
        for (element in this) {
            if (element.isJsonObject) block(element.asJsonObject)
        }
    }
}
