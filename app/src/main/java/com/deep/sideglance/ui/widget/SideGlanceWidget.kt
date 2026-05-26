package com.deep.sideglance.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.deep.sideglance.MainActivity
import com.deep.sideglance.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

class SideGlanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val name = prefs[stringPreferencesKey("partner_name")] ?: "Partner"
            val statusType = prefs[stringPreferencesKey("status_type")] ?: "Offline"
            val statusDetail = prefs[stringPreferencesKey("status_detail")] ?: ""
            val statusTime = prefs[longPreferencesKey("status_time")] ?: 0L
            val statusExpiry = prefs[longPreferencesKey("status_expiry")] ?: 0L
            val distance = prefs[stringPreferencesKey("partner_distance")] ?: "infinity"

            val isExpired = statusExpiry != 0L && System.currentTimeMillis() > statusExpiry
            
            GlanceTheme {
                WidgetContent(
                    context = context,
                    name = name,
                    statusType = if (isExpired) "Offline" else statusType,
                    statusDetail = if (isExpired) "" else statusDetail,
                    statusTime = statusTime,
                    statusExpiry = statusExpiry,
                    distance = distance
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        name: String,
        statusType: String,
        statusDetail: String,
        statusTime: Long,
        statusExpiry: Long,
        distance: String
    ) {
        val backgroundProvider = ColorProvider(CreamBg)
        val textProvider = ColorProvider(Charcoal)
        val cardProvider = ColorProvider(SoftSand)
        val accentProvider = ColorProvider(SageGreen)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(backgroundProvider)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Widget Title/Partner name
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👀 $name",
                    style = TextStyle(
                        color = accentProvider,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Partner status card
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .background(cardProvider)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = statusType,
                    style = TextStyle(
                        color = textProvider,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                
                if (statusDetail.isNotEmpty()) {
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = statusDetail,
                        style = TextStyle(
                            color = textProvider,
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Bottom metadata (Distance & last updated time)
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formattedDistance = if (distance == "infinity") "∞" else distance.replace(" away", "")
                
                val timeStr = if (statusTime > 0L) {
                    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                    sdf.format(Date(statusTime))
                } else {
                    "--"
                }

                val expiryStr = if (statusType == "Offline") {
                    ""
                } else if (statusExpiry == 0L) {
                    " • forever"
                } else {
                    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                    " • until ${sdf.format(Date(statusExpiry))}"
                }

                Text(
                    text = "📍 $formattedDistance • $timeStr$expiryStr",
                    style = TextStyle(
                        color = textProvider,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

object WidgetStateUpdateHelper {
    suspend fun updateWidgetState(
        context: Context,
        name: String? = null,
        statusType: String? = null,
        detail: String? = null,
        time: Long? = null,
        expiry: Long? = null,
        distance: String? = null
    ) {
        val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(SideGlanceWidget::class.java)
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    if (name != null) set(stringPreferencesKey("partner_name"), name)
                    if (statusType != null) set(stringPreferencesKey("status_type"), statusType)
                    if (detail != null) set(stringPreferencesKey("status_detail"), detail)
                    if (time != null) set(longPreferencesKey("status_time"), time)
                    if (expiry != null) set(longPreferencesKey("status_expiry"), expiry)
                    if (distance != null) set(stringPreferencesKey("partner_distance"), distance)
                }
            }
            SideGlanceWidget().update(context, glanceId)
        }
    }
}
