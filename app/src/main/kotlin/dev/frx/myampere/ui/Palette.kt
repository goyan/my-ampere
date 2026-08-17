package dev.frx.myampere.ui

import androidx.compose.ui.graphics.Color

/** Couleurs Compose partagées par les écrans app (Live, LiveGraph, Historique).
 *  Mêmes valeurs que res/values/colors.xml, dupliquées ici car le widget (RemoteViews)
 *  ne peut pas consommer des Color Compose — les deux jeux doivent rester synchronisés. */
object Palette {
    val chargeGreen = Color(0xFF2E7D32)
    val dischargeRed = Color(0xFFC62828)
    val graphBlue = Color(0xFF1565C0)
}
