package com.herwin.netez.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import java.util.*

@Composable
fun LegalConsentDialog(
    isExternalTarget: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val systemLocale = Locale.getDefault().language
    var selectedLanguage by remember { mutableStateOf(if (systemLocale == "in" || systemLocale == "id") "ID" else "EN") }
    
    var checked1 by remember { mutableStateOf(false) }
    var checked2 by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E2235)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            painter = AppIcons.shield(),
                            contentDescription = null,
                            tint = Color(0xFFFFA726),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Peringatan Hukum & Etika / Legal & Ethics Notice",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 18.sp
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF2A2E45), RoundedCornerShape(8.dp))
                            .padding(2.dp)
                    ) {
                        LanguageChip("ID", selectedLanguage == "ID") { selectedLanguage = "ID" }
                        LanguageChip("EN", selectedLanguage == "EN") { selectedLanguage = "EN" }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (isExternalTarget) {
                    Surface(
                        color = Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFC107)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = if (selectedLanguage == "ID") 
                                "⚠️ Target eksternal terdeteksi. Gunakan beban rendah (maks 5 koneksi) dan pastikan ada izin untuk menghormati rate limit server."
                            else 
                                "⚠️ External target detected. Use low load (max 5 connections) and ensure permission to respect server rate limits.",
                            color = Color(0xFF795548),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedLanguage == "ID") {
                        IndonesianNotice()
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFF2A2E45), thickness = 1.dp)
                        Spacer(Modifier.height(16.dp))
                        EnglishNotice(small = true)
                    } else {
                        EnglishNotice()
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFF2A2E45), thickness = 1.dp)
                        Spacer(Modifier.height(16.dp))
                        IndonesianNotice(small = true)
                    }
                }

                Spacer(Modifier.height(16.dp))

                ConsentCheckbox(
                    checked = checked1,
                    onCheckedChange = { checked1 = it },
                    textId = "Saya mengerti dan menyetujui bahwa saya hanya akan menguji jaringan milik saya sendiri atau yang telah mendapat izin",
                    textEn = "I understand and agree that I will only test networks I own or have permission to test",
                    currentLang = selectedLanguage
                )
                
                ConsentCheckbox(
                    checked = checked2,
                    onCheckedChange = { checked2 = it },
                    textId = "Saya bertanggung jawab penuh atas penggunaan fitur ini dan risikonya",
                    textEn = "I take full responsibility for the use of this feature and its risks",
                    currentLang = selectedLanguage
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2E45))
                    ) {
                        Text(if (selectedLanguage == "ID") "BATAL" else "CANCEL", fontSize = 14.sp, color = Color.LightGray)
                    }
                    
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = checked1 && checked2,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6C8CFF),
                            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            if (selectedLanguage == "ID") "SETUJU & LANJUTKAN" else "AGREE & CONTINUE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (checked1 && checked2) Color.White else Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) Color(0xFF6C8CFF) else Color.Transparent,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.White else Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun IndonesianNotice(small: Boolean = false) {
    Column {
        Text(
            "🇮🇩 BAHASA INDONESIA",
            color = if (small) TextGray else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = if (small) 12.sp else 14.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Fitur ini hanya boleh digunakan untuk menguji jaringan atau server yang menjadi hak milik Anda sendiri, atau yang telah mendapat izin resmi dari pemiliknya.\n\n" +
            "DILARANG KERAS:\n" +
            "• Menguji jaringan atau server milik orang lain tanpa izin\n" +
            "• Menggunakan fitur ini untuk mengganggu, merusak, atau melumpuhkan layanan (DoS/DDoS)\n" +
            "• Menguji server publik dengan beban berlebihan yang melanggar Terms of Service\n\n" +
            "Dengan melanjutkan, Anda menyatakan bahwa:\n" +
            "✓ Target yang Anda uji adalah milik Anda sendiri, atau\n" +
            "✓ Anda telah memiliki izin tertulis dari pemilik jaringan/server\n" +
            "✓ Anda bertanggung jawab penuh atas penggunaan fitur ini\n\n" +
            "Pelanggaran dapat melanggar UU ITE No 11 Tahun 2008.",
            color = if (small) TextGray.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.9f),
            fontSize = if (small) 11.sp else 13.sp,
            lineHeight = if (small) 15.sp else 18.sp
        )
    }
}

@Composable
fun EnglishNotice(small: Boolean = false) {
    Column {
        Text(
            "🇺🇸 ENGLISH",
            color = if (small) TextGray else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = if (small) 12.sp else 14.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "This feature may ONLY be used to test networks or servers that you own, or for which you have obtained official authorization from the owner.\n\n" +
            "STRICTLY PROHIBITED:\n" +
            "• Testing networks or servers owned by others without permission\n" +
            "• Using this feature to disrupt, damage, or disable services (DoS/DDoS)\n" +
            "• Testing public servers with excessive load that violates their Terms of Service\n\n" +
            "By continuing, you declare that:\n" +
            "✓ The target you are testing is your own property, or\n" +
            "✓ You have written permission from the network/server owner\n" +
            "✓ You take full responsibility for the use of this feature.",
            color = if (small) TextGray.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.9f),
            fontSize = if (small) 11.sp else 13.sp,
            lineHeight = if (small) 15.sp else 18.sp
        )
    }
}

@Composable
fun ConsentCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textId: String,
    textEn: String,
    currentLang: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF6C8CFF),
                uncheckedColor = Color.Gray
            )
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = if (currentLang == "ID") textId else textEn,
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Text(
                text = if (currentLang == "ID") textEn else textId,
                color = TextGray,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}
