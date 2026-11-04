package com.herwin.netez.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herwin.netez.R
import com.herwin.netez.entitlement.*
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.utils.RewardedAdManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextualPaywallDialog(
    featureId: FeatureId,
    limitText: String,
    onDismiss: () -> Unit,
    onUpgradeClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val accessManager = remember { FeatureAccessManager.getInstance(context) }
    val billingManager = remember { BillingManager.getInstance(context) }

    val monthlyProduct by billingManager.monthlyProduct.collectAsState()
    val yearlyProduct by billingManager.yearlyProduct.collectAsState()

    var selectedPlan by remember { mutableStateOf("YEARLY") }
    var rewardGranted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        billingManager.querySubscriptionProducts()
        RewardedAdManager.loadAd(context)
    }

    val showRewardedAdOption = remember(featureId) {
        featureId == FeatureId.GLOBAL_TOOL_USAGE ||
                featureId == FeatureId.SECURITY_INTELLIGENCE_DAILY ||
                featureId == FeatureId.STABILITY_TEST_DAILY ||
                featureId == FeatureId.LEAK_CHECK_DAILY ||
                featureId == FeatureId.PHISHING_CHECK_DAILY ||
                featureId == FeatureId.BLACKLIST_CHECK_DAILY
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Icon(
                    painter = AppIcons.star(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(32.dp)
                )
            }

            Text(
                text = "NeTez PRO",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = limitText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Plan Selector Cards (Yearly vs Monthly)
            val monthlyPriceStr = monthlyProduct?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "Rp 15.000 / mo"
            val yearlyPriceStr = yearlyProduct?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "Rp 149.000 / yr"

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 1. Yearly Option
                DialogPlanCard(
                    title = "NeTez PRO Yearly",
                    price = yearlyPriceStr,
                    subtitle = "Billed annually • Best value",
                    badgeText = "SAVE 40% (BEST VALUE)",
                    isSelected = selectedPlan == "YEARLY",
                    onClick = { selectedPlan = "YEARLY" }
                )

                // 2. Monthly Option
                DialogPlanCard(
                    title = "NeTez PRO Monthly",
                    price = monthlyPriceStr,
                    subtitle = "Billed monthly • Cancel anytime",
                    badgeText = null,
                    isSelected = selectedPlan == "MONTHLY",
                    onClick = { selectedPlan = "MONTHLY" }
                )
            }

            if (showRewardedAdOption && !rewardGranted) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Support Developer (Optional)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Watch a quick ad to get +1 extra scan today",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    RewardedAdManager.showAd(
                                        activity = activity,
                                        onRewardEarned = {
                                            val success = accessManager.grantRewardedBonus(featureId)
                                            if (success) {
                                                rewardGranted = true
                                                Toast.makeText(context, "+1 Bonus scan granted! You can now scan.", Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            } else {
                                                Toast.makeText(context, "Daily rewarded ad bonus limit reached", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onAdFailed = { err ->
                                            Toast.makeText(context, "Ad unavailable: $err", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    val success = accessManager.grantRewardedBonus(featureId)
                                    if (success) {
                                        rewardGranted = true
                                        Toast.makeText(context, "+1 Bonus scan granted! You can now scan.", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("WATCH AD", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Main Upgrade Button
            Button(
                onClick = {
                    val activity = context as? Activity
                    val targetProduct = if (selectedPlan == "MONTHLY") monthlyProduct else yearlyProduct

                    if (activity != null && targetProduct != null) {
                        onDismiss()
                        billingManager.launchPurchase(activity, targetProduct)
                    } else {
                        Toast.makeText(context, "Loading Play Store plan details... Please try again in a moment.", Toast.LENGTH_SHORT).show()
                        billingManager.startConnection()
                    }
                    onUpgradeClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(painter = AppIcons.star(), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "SUBSCRIBE NOW VIA GOOGLE PLAY",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }

            TextButton(onClick = onDismiss) {
                Text(
                    text = "Not Now",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun DialogPlanCard(
    title: String,
    price: String,
    subtitle: String,
    badgeText: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (badgeText != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = price,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    fontFamily = TechMonospace,
                    color = MaterialTheme.colorScheme.primary
                )
                if (isSelected) {
                    Text(
                        text = "SELECTED ✓",
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
