package com.herwin.netez.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.herwin.netez.R
import com.herwin.netez.engine.*
import com.herwin.netez.ui.components.CircularScoreView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SecurityPostureDetailFragment : Fragment() {

    private lateinit var scoreCircle: CircularScoreView
    private lateinit var statusText: TextView
    private lateinit var lastScanText: TextView
    private lateinit var categoryContainer: LinearLayout
    private lateinit var issuesContainer: LinearLayout

    var onNavigateToDeviceCheck: (() -> Unit)? = null
    var onNavigateToNetworkAudit: (() -> Unit)? = null
    var onNavigateToAppScanner: (() -> Unit)? = null
    var onNavigateToPrivacy: (() -> Unit)? = null
    var onNavigateToSslReminder: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_security_posture_detail, container, false)
        
        scoreCircle = view.findViewById(R.id.score_circle_large)
        statusText = view.findViewById(R.id.tv_overall_status)
        lastScanText = view.findViewById(R.id.tv_last_scan)
        categoryContainer = view.findViewById(R.id.category_container)
        issuesContainer = view.findViewById(R.id.issues_container)
        
        view.findViewById<View>(R.id.toolbar).setOnClickListener {
            activity?.onBackPressed()
        }

        view.findViewById<Button>(R.id.btn_recommendations).setOnClickListener {
            showRecommendations()
        }

        view.findViewById<Button>(R.id.btn_rescan).setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.security_rescanning), Toast.LENGTH_SHORT).show()
            loadData()
        }
        
        view.findViewById<Button>(R.id.btn_share).setOnClickListener {
            shareReport()
        }

        loadData()
        
        return view
    }

    private fun loadData() {
        if (!isAdded) return
        lifecycleScope.launch {
            try {
                val context = context ?: return@launch
                val result = SecurityPostureEngine.calculate(context)
                if (view != null) {
                    updateUI(result)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateUI(result: SecurityPostureResult) {
        scoreCircle.setScore(result.overallScore, result.overallStatus)
        statusText.text = result.overallStatus
        
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        lastScanText.text = "Last scan at ${sdf.format(Date(result.lastScan))}"
        
        categoryContainer.removeAllViews()
        result.categories.forEach { category ->
            addCategoryView(category)
        }
        
        issuesContainer.removeAllViews()
        result.issues.forEach { issue ->
            addIssueView(issue)
        }
    }

    private fun addCategoryView(category: CategoryScore) {
        val view = layoutInflater.inflate(R.layout.item_security_category, categoryContainer, false)
        val dot = view.findViewById<View>(R.id.status_dot)
        val name = view.findViewById<TextView>(R.id.tv_category_name)
        val summary = view.findViewById<TextView>(R.id.tv_category_summary)
        val score = view.findViewById<TextView>(R.id.tv_category_score)
        
        name.text = category.name
        summary.text = category.summary
        score.text = category.score.toString()
        
        val dotColor = when (category.status) {
            DotStatus.SAFE -> Color.parseColor("#22C55E")
            DotStatus.WARNING -> Color.parseColor("#EAB308")
            DotStatus.CRITICAL -> Color.parseColor("#EF4444")
        }
        
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(dotColor)
        }
        dot.background = drawable

        view.setOnClickListener {
            when (category.name) {
                "Device Security" -> onNavigateToDeviceCheck?.invoke()
                "Network Security" -> onNavigateToNetworkAudit?.invoke()
                "App Security" -> onNavigateToAppScanner?.invoke()
                "Privacy" -> onNavigateToPrivacy?.invoke()
            }
        }
        
        categoryContainer.addView(view)
    }

    private fun addIssueView(issue: Issue) {
        val view = layoutInflater.inflate(R.layout.item_security_issue, issuesContainer, false)
        val icon = view.findViewById<TextView>(R.id.tv_issue_icon)
        val title = view.findViewById<TextView>(R.id.tv_issue_title)
        val desc = view.findViewById<TextView>(R.id.tv_issue_desc)
        val fixBtn = view.findViewById<Button>(R.id.btn_fix)
        
        icon.text = if (issue.severity == Severity.CRITICAL) "🔴" else "⚠️"
        title.text = issue.title
        desc.text = issue.desc
        
        fixBtn.setOnClickListener {
            when (issue.action) {
                Action.OPEN_DEVICE_CHECK -> onNavigateToDeviceCheck?.invoke()
                Action.OPEN_WIFI_AUDIT -> onNavigateToNetworkAudit?.invoke()
                Action.OPEN_APP_SCANNER -> onNavigateToAppScanner?.invoke()
                Action.OPEN_SSL_REMINDER -> onNavigateToSslReminder?.invoke()
                Action.OPEN_PASSWORD_MANAGER -> onNavigateToPrivacy?.invoke()
                else -> {}
            }
        }
        
        issuesContainer.addView(view)
    }

    private fun shareReport() {
        if (!isAdded) return
        lifecycleScope.launch {
            try {
                val context = context ?: return@launch
                val result = SecurityPostureEngine.calculate(context)
                val text = "Security Posture ${result.overallScore}/100 ${result.overallStatus}\n" +
                        "Details: " + result.categories.joinToString(", ") { "${it.name.split(" ")[0]}: ${it.score}" } + "\n" +
                        "Issues: ${result.issues.size} found\n" +
                        "via NeTez"
                
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.security_share_subject))
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.wifi_audit_share)))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showRecommendations() {
        if (!isAdded) return
        lifecycleScope.launch {
            try {
                val context = context ?: return@launch
                val result = SecurityPostureEngine.calculate(context)
                val recommendations = result.issues.map { it.desc }
                if (recommendations.isEmpty()) {
                    Toast.makeText(context, getString(R.string.security_recommendations_empty), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val items = recommendations.toTypedArray()
                AlertDialog.Builder(context)
                    .setTitle(getString(R.string.security_recommendations_title))
                    .setItems(items) { _, _ -> }
                    .setPositiveButton(getString(R.string.btn_close), null)
                    .show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
