package com.herwin.netez.ui.fragments

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.herwin.netez.R
import com.herwin.netez.engine.DotStatus
import com.herwin.netez.engine.SecurityPostureEngine
import com.herwin.netez.engine.SecurityPostureResult
import com.herwin.netez.ui.components.CircularScoreView
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private lateinit var ssidText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var localIpText: TextView
    private lateinit var publicIpText: TextView
    private lateinit var gatewayText: TextView
    private lateinit var gatewayLabel: TextView
    private lateinit var ispText: TextView
    private lateinit var scoreCircle: CircularScoreView
    private lateinit var categoryChips: ChipGroup
    private lateinit var issuesText: TextView
    private lateinit var wifiIcon: ImageView

    var onHeroClick: (() -> Unit)? = null
    var onWifiIconClick: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dashboard_hero_merged, container, false)
        
        ssidText = view.findViewById(R.id.tv_ssid)
        subtitleText = view.findViewById(R.id.tv_subtitle)
        localIpText = view.findViewById(R.id.tv_local_ip_value)
        publicIpText = view.findViewById(R.id.tv_public_ip_value)
        gatewayText = view.findViewById(R.id.tv_gateway_value)
        gatewayLabel = view.findViewById(R.id.tv_gateway_label)
        ispText = view.findViewById(R.id.tv_isp_value)
        scoreCircle = view.findViewById(R.id.score_circle)
        categoryChips = view.findViewById(R.id.category_chips)
        issuesText = view.findViewById(R.id.tv_issues)
        wifiIcon = view.findViewById(R.id.iv_wifi)
        
        view.findViewById<View>(R.id.hero_card).setOnClickListener {
            onHeroClick?.invoke()
        }
        
        wifiIcon.setOnClickListener {
            onWifiIconClick?.invoke()
        }

        refreshData()
        
        return view
    }

    fun refreshData() {
        lifecycleScope.launch {
            val result = SecurityPostureEngine.calculate(requireContext())
            updateUI(result)
        }
    }

    private fun updateUI(result: SecurityPostureResult) {
        val net = result.netInfo
        ssidText.text = net.ssidOrOperator
        subtitleText.text = if (net.isWifi) "WiFi Network" else "Cellular Network • ${net.networkType}"
        localIpText.text = net.localIp
        publicIpText.text = net.publicIp
        
        if (net.isWifi) {
            gatewayLabel.text = "GATEWAY"
            gatewayText.text = net.gateway
        } else {
            gatewayLabel.text = "TYPE"
            gatewayText.text = net.networkType
        }
        
        ispText.text = net.isp
        wifiIcon.setImageResource(if (net.isWifi) R.drawable.ic_wifi else R.drawable.ic_signal_cellular)
        
        scoreCircle.setScore(result.overallScore, result.overallStatus)
        
        categoryChips.removeAllViews()
        result.categories.forEach { category ->
            val chip = Chip(requireContext())
            val dot = when (category.status) {
                DotStatus.SAFE -> "🟢"
                DotStatus.WARNING -> "🟡"
                DotStatus.CRITICAL -> "🔴"
            }
            chip.text = "$dot ${category.name.split(" ")[0]} ${category.score}"
            chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#26FFFFFF"))
            chip.setTextColor(Color.WHITE)
            chip.textSize = 11f
            chip.chipCornerRadius = 8f * resources.displayMetrics.density
            chip.setEnsureMinTouchTargetSize(false)
            categoryChips.addView(chip)
        }
        
        val issueSummary = result.issues.joinToString(" • ") { it.title }
        issuesText.text = getString(R.string.security_issues, result.issues.size, issueSummary)
    }
}
