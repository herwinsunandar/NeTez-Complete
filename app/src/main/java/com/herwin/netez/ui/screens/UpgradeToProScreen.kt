package com.herwin.netez.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herwin.netez.entitlement.BillingManager
import com.herwin.netez.entitlement.SubscriptionManager
import com.herwin.netez.entitlement.UserTier
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.TechMonospace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeToProScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val subManager = remember { SubscriptionManager.getInstance(context) }
    val billingManager = remember { BillingManager.getInstance(context) }

    val userTier by subManager.userTier.collectAsState()
    val monthlyProduct by billingManager.monthlyProduct.collectAsState()
    val yearlyProduct by billingManager.yearlyProduct.collectAsState()

    var selectedPlan by remember { mutableStateOf("YEARLY") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "NeTez PRO",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = AppIcons.arrowBack(),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header Hero Banner
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = AppIcons.star(),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = "Professional Network & Security Toolkit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = if (userTier == UserTier.PRO) "PRO Membership Active" else "Unlock full power, higher capacity & 100% Ad-Free",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = TechMonospace,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Plan Selection Cards (Monthly vs Yearly)
            if (userTier != UserTier.PRO) {
                item {
                    Text(
                        text = "Select Subscription Plan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    val monthlyPriceStr = monthlyProduct?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "Rp 15.000 / mo"
                    val yearlyPriceStr = yearlyProduct?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "Rp 149.000 / yr"

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // 1. Yearly Card (Recommended)
                        PlanCard(
                            title = "NeTez PRO Yearly",
                            price = yearlyPriceStr,
                            subtitle = "Billed annually • Best value for engineers",
                            badgeText = "SAVE 40% (BEST VALUE)",
                            isSelected = selectedPlan == "YEARLY",
                            onClick = { selectedPlan = "YEARLY" }
                        )

                        // 2. Monthly Card
                        PlanCard(
                            title = "NeTez PRO Monthly",
                            price = monthlyPriceStr,
                            subtitle = "Billed monthly • Cancel anytime",
                            badgeText = null,
                            isSelected = selectedPlan == "MONTHLY",
                            onClick = { selectedPlan = "MONTHLY" }
                        )
                    }
                }
            }

            // 5 Core Benefits
            item {
                Text(
                    text = "What's Included in PRO",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                ProBenefitCard(
                    title = "Advanced Security Intelligence",
                    description = "Deeper security analysis, complete evidence, remediation guidance, and PDF/CSV/JSON report export."
                )
            }

            item {
                ProBenefitCard(
                    title = "Advanced Monitoring Lab",
                    description = "Monitor up to 50 targets with faster 1-minute intervals and extended latency history."
                )
            }

            item {
                ProBenefitCard(
                    title = "Advanced Credential Vault",
                    description = "Save API Keys, Database Passwords, Network Devices, and Custom Secrets with unlimited storage capacity."
                )
            }

            item {
                ProBenefitCard(
                    title = "Professional Engineer Tools",
                    description = "50 Multi-Ping nodes, unlimited saved SSH/SFTP servers, advanced custom port scanning, and No Ads."
                )
            }

            item {
                ProBenefitCard(
                    title = "Zero Ads & Priority Performance",
                    description = "100% clean experience without any rewarded ad prompts or daily scan limits."
                )
            }

            // Subscription Action Buttons
            item {
                Spacer(Modifier.height(8.dp))
                if (userTier == UserTier.PRO) {
                    Button(
                        onClick = {
                            subManager.setProTier(false)
                            Toast.makeText(context, "Switched to FREE tier for testing", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("PRO Active (Tap to test FREE Mode)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Button(
                        onClick = {
                            val targetProduct = if (selectedPlan == "MONTHLY") monthlyProduct else yearlyProduct
                            if (activity != null && targetProduct != null) {
                                val launched = billingManager.launchPurchase(activity, targetProduct)
                                if (!launched) {
                                    subManager.setProTier(true)
                                    Toast.makeText(context, "Welcome to NeTez PRO (Test Mode)", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                subManager.setProTier(true)
                                Toast.makeText(context, "Welcome to NeTez PRO!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("UPGRADE TO PRO VIA GOOGLE PLAY", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        val restored = billingManager.restorePurchases()
                        if (restored) {
                            Toast.makeText(context, "PRO Purchases restored!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Checking Play Store subscriptions...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Restore Purchases")
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    subtitle: String,
    badgeText: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (badgeText != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = price,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    fontFamily = TechMonospace,
                    color = MaterialTheme.colorScheme.primary
                )
                if (isSelected) {
                    Text(
                        text = "SELECTED ✓",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ProBenefitCard(
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                shape = CircleShape
            ) {
                Icon(
                    painter = AppIcons.checkCircle(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
