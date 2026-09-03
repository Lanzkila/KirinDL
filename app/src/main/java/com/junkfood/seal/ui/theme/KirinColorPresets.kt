package com.junkfood.seal.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Optional KirinDL body/button overrides. Index 0 always means follow the active Material theme. */
data class KirinColorPreset(
    val title: String,
    val bodyLight: Color,
    val bodyDark: Color,
    val buttonLight: Color,
    val buttonDark: Color,
)

val KirinColorPresets =
    listOf(
        KirinColorPreset("Follow theme", Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified),
        KirinColorPreset("Kirin Violet", Color(0xFFF7F3FF), Color(0xFF15111F), Color(0xFF7457E8), Color(0xFF9C83FF)),
        KirinColorPreset("Ocean Blue", Color(0xFFF2F7FF), Color(0xFF0C1726), Color(0xFF2868D8), Color(0xFF6A9DFF)),
        KirinColorPreset("Cyan", Color(0xFFF0FBFC), Color(0xFF071B1E), Color(0xFF008C9E), Color(0xFF51D4E3)),
        KirinColorPreset("Emerald", Color(0xFFF1FAF5), Color(0xFF0A1B12), Color(0xFF16865B), Color(0xFF4FD39B)),
        KirinColorPreset("Forest", Color(0xFFF4F8F1), Color(0xFF101A0C), Color(0xFF4D7C32), Color(0xFF8FC96E)),
        KirinColorPreset("Amber", Color(0xFFFFF8E8), Color(0xFF211806), Color(0xFFB36B00), Color(0xFFFFB84D)),
        KirinColorPreset("Orange", Color(0xFFFFF5EF), Color(0xFF241108), Color(0xFFC55720), Color(0xFFFF8C58)),
        KirinColorPreset("Rose", Color(0xFFFFF2F6), Color(0xFF231018), Color(0xFFC43E6C), Color(0xFFFF79A2)),
        KirinColorPreset("Magenta", Color(0xFFFFF1FC), Color(0xFF241021), Color(0xFFB73EA6), Color(0xFFF078DB)),
        KirinColorPreset("Red", Color(0xFFFFF3F2), Color(0xFF24100F), Color(0xFFC73D37), Color(0xFFFF7770)),
        KirinColorPreset("Slate", Color(0xFFF5F7FA), Color(0xFF11151A), Color(0xFF526273), Color(0xFF9CAFC1)),
        KirinColorPreset("Monochrome", Color(0xFFF5F5F5), Color(0xFF111111), Color(0xFF3E3E3E), Color(0xFFD0D0D0)),
        KirinColorPreset("Sky", Color(0xFFF1F9FF), Color(0xFF081923), Color(0xFF1976B8), Color(0xFF67C5FF)),
        KirinColorPreset("Teal", Color(0xFFEFFAF8), Color(0xFF071B18), Color(0xFF087F73), Color(0xFF52D4C4)),
        KirinColorPreset("Mint", Color(0xFFF0FBF7), Color(0xFF081B14), Color(0xFF299B70), Color(0xFF6EE0B0)),
        KirinColorPreset("Lime", Color(0xFFF8FBEF), Color(0xFF171B08), Color(0xFF718D22), Color(0xFFB5D96B)),
        KirinColorPreset("Gold", Color(0xFFFFFAEB), Color(0xFF201A08), Color(0xFF9B7610), Color(0xFFE2BD55)),
        KirinColorPreset("Coral", Color(0xFFFFF3F0), Color(0xFF24110D), Color(0xFFC95645), Color(0xFFFF8B78)),
        KirinColorPreset("Pink", Color(0xFFFFF1F8), Color(0xFF241019), Color(0xFFC54382), Color(0xFFFF7DB4)),
        KirinColorPreset("Indigo", Color(0xFFF4F4FF), Color(0xFF111326), Color(0xFF4D58C7), Color(0xFF8993FF)),
        KirinColorPreset("Midnight", Color(0xFFF3F5FA), Color(0xFF080D18), Color(0xFF334D78), Color(0xFF7F9FCE)),
    )

fun kirinBodyColor(index: Int, darkTheme: Boolean): Color? =
    KirinColorPresets.getOrNull(index)?.let { preset ->
        val value = if (darkTheme) preset.bodyDark else preset.bodyLight
        value.takeUnless { it == Color.Unspecified }
    }

fun kirinButtonColor(index: Int, darkTheme: Boolean): Color? =
    KirinColorPresets.getOrNull(index)?.let { preset ->
        val value = if (darkTheme) preset.buttonDark else preset.buttonLight
        value.takeUnless { it == Color.Unspecified }
    }

fun readableOnColor(color: Color): Color =
    if (color.luminance() > 0.45f) Color(0xFF101114) else Color.White
