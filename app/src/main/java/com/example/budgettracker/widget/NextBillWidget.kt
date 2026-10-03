package com.example.budgettracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.example.budgettracker.BudgetTrackerApplication
import com.example.budgettracker.MainActivity
import com.example.budgettracker.R
import com.example.budgettracker.domain.BillDirection
import com.example.budgettracker.domain.localDateOf
import com.example.budgettracker.ui.home.outlookFromLedger
import com.example.budgettracker.ui.utils.CurrencyUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class NextBillWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = render(context)
                for (id in appWidgetIds) {
                    appWidgetManager.updateAppWidget(id, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, NextBillWidget::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            val intent = Intent(context, NextBillWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}

private suspend fun render(context: Context): RemoteViews {
    val app = context.applicationContext as BudgetTrackerApplication
    val repository = app.container.budgetRepository
    val accounts = repository.getAllAccounts().first()
    val categories = repository.getAllCategories().first()
    val rules = repository.getAllRecurringTransactions().first()
    val debts = repository.getAllDebts().first()
    val included = accounts.filter { it.includeInTotalBalance }.sumOf { it.balance }
    val outlook = outlookFromLedger(
        accounts = accounts,
        categories = categories,
        rules = rules,
        debts = debts,
        unknownDebtName = context.getString(R.string.debt_tracker),
        includedBalanceCentavos = included
    )
    val views = RemoteViews(context.packageName, R.layout.widget_next_bill)
    val openApp = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    views.setOnClickPendingIntent(R.id.widget_root, openApp)
    views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_next_bill))
    val next = outlook.upcoming.firstOrNull()
    if (next == null) {
        views.setViewVisibility(R.id.widget_bill, View.GONE)
        views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
        views.setTextViewText(R.id.widget_empty, context.getString(R.string.nothing_due_this_month))
    } else {
        views.setViewVisibility(R.id.widget_bill, View.VISIBLE)
        views.setViewVisibility(R.id.widget_empty, View.GONE)
        val day = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
            .format(localDateOf(next.whenMillis, ZoneId.systemDefault()))
        val arrives = next.direction == BillDirection.ARRIVES
        val direction = context.getString(if (arrives) R.string.bill_arrives else R.string.bill_leaves)
        val sign = if (arrives) "+" else "-"
        views.setTextViewText(R.id.widget_name, next.name)
        views.setTextViewText(R.id.widget_when, context.getString(R.string.bill_when, day, direction))
        views.setTextViewText(R.id.widget_amount, sign + CurrencyUtils.formatAmount(next.amountCentavos))
        val color = ContextCompat.getColor(context, if (arrives) R.color.widget_income else R.color.widget_expense)
        views.setTextColor(R.id.widget_amount, color)
    }
    return views
}
