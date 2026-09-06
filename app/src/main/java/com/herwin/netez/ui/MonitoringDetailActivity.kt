package com.herwin.netez.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.MonitoringLog
import com.herwin.netez.data.MonitoringTarget
import com.herwin.netez.data.MonitoringType
import com.herwin.netez.databinding.ActivityMonitoringDetailBinding
import com.herwin.netez.databinding.ItemHistoryLogBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import kotlinx.coroutines.flow.combine

import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class MonitoringDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMonitoringDetailBinding
    private var targetId: Long = -1
    private lateinit var historyAdapter: HistoryAdapter
    private var chartTimestamps = listOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        try {
            binding = ActivityMonitoringDetailBinding.inflate(layoutInflater)
            setContentView(binding.root)
            setupEdgeToEdge()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading UI: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        targetId = intent.getLongExtra("TARGET_ID", -1)
        if (targetId == -1L) {
            finish()
            return
        }

        setupToolbar()
        setupChart()
        setupRecyclerView()
        setupButtons()
        observeData()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                12 * (resources.displayMetrics.density).toInt() + systemBars.bottom
            )
            insets
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.parseColor("#8A8E9A")
                gridColor = Color.parseColor("#2A2E45")
                axisLineColor = Color.parseColor("#2A2E45")
                setDrawGridLines(false)
                granularity = 1f
                setAvoidFirstLastClipping(true)
                labelRotationAngle = -20f
                valueFormatter = object : ValueFormatter() {
                    private val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        if (index < 0 || index >= chartTimestamps.size) return ""
                        return sdf.format(Date(chartTimestamps[index]))
                    }
                }
            }
            
            axisLeft.apply {
                textColor = Color.parseColor("#8A8E9A")
                gridColor = Color.parseColor("#1E2235")
                setDrawGridLines(true)
                axisMinimum = 0f
                granularity = 20f
            }
            
            axisRight.isEnabled = false
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter()
        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MonitoringDetailActivity)
            adapter = historyAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupButtons() {
        val database = AppDatabase.getDatabase(this)
        val dao = database.monitoringDao()

        binding.btnPauseResume.setOnClickListener {
            lifecycleScope.launch {
                val target = dao.getTargetById(targetId)
                target?.let {
                    val updated = it.copy(isActive = !it.isActive)
                    dao.updateTarget(updated)
                    binding.btnPauseResume.text = if (updated.isActive) "Pause" else "Resume"
                }
            }
        }

        binding.btnDelete.setOnClickListener {
            lifecycleScope.launch {
                val target = dao.getTargetById(targetId)
                target?.let {
                    dao.deleteTarget(it)
                    dao.deleteLogsForTarget(targetId)
                    finish()
                }
            }
        }

        binding.btnEdit.setOnClickListener {
            Toast.makeText(this, "Edit coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeData() {
        val database = AppDatabase.getDatabase(this)
        val dao = database.monitoringDao()

        lifecycleScope.launch {
            val target = dao.getTargetById(targetId)
            target?.let {
                binding.toolbar.title = getTargetDisplayName(it)
                updateStatusCard(it)
                binding.btnPauseResume.text = if (it.isActive) "Pause" else "Resume"
            }
        }

        lifecycleScope.launch {
            dao.getLogsForTarget(targetId).collectLatest { logs ->
                val last24h = System.currentTimeMillis() - 86400000
                val filteredLogs = logs.filter { it.timestamp > last24h }.sortedBy { it.timestamp }
                
                if (filteredLogs.size < 2) {
                    binding.lineChart.visibility = View.GONE
                    binding.emptyChartText.visibility = View.VISIBLE
                    binding.emptyChartText.text = "Collecting data... chart will appear after 2 successful checks."
                } else {
                    binding.lineChart.visibility = View.VISIBLE
                    binding.emptyChartText.visibility = View.GONE
                    updateChart(filteredLogs)
                }
                historyAdapter.submitList(logs)
            }
        }
        
        lifecycleScope.launch {
            dao.getAverageLatency(targetId).collectLatest { avg ->
                binding.avgLatencyText.text = "${avg?.toInt() ?: 0}ms"
            }
        }
        
        lifecycleScope.launch {
            combine(
                dao.getUpLogsCount(targetId),
                dao.getTotalLogsCount(targetId)
            ) { up, total -> up to total }.collectLatest { (upCount, total) ->
                val uptime = if (total > 0) (upCount.toDouble() / total * 100) else 0.0
                binding.uptimeText.text = String.format("%.1f%%", uptime)
            }
        }
    }

    private fun getTargetDisplayName(target: MonitoringTarget): String {
        return when (target.type) {
            MonitoringType.PING -> target.host ?: ""
            MonitoringType.PORT -> "${target.host}:${target.port}"
            MonitoringType.HTTP -> target.url ?: ""
            MonitoringType.DNS -> target.host ?: ""
        }
    }

    private fun updateStatusCard(target: MonitoringTarget) {
        val color = if (target.lastStatus == "UP") 0xFF22C55E.toInt() else 0xFFEF4444.toInt()
        binding.statusDot.setBackgroundColor(color)
        
        binding.statusDot.clearAnimation()
        val anim = AlphaAnimation(1f, 0.4f)
        anim.duration = 800
        anim.repeatMode = Animation.REVERSE
        anim.repeatCount = Animation.INFINITE
        binding.statusDot.startAnimation(anim)

        binding.statusText.text = "${target.lastStatus} • ${target.lastLatency ?: 0}ms • just now"
        binding.downEventsText.text = target.failCount.toString()
    }

    private fun updateChart(logs: List<MonitoringLog>) {
        chartTimestamps = logs.map { it.timestamp }
        val entries = logs.mapIndexed { index, log ->
            Entry(index.toFloat(), log.latency.toFloat())
        }

        val dataSet = LineDataSet(entries, "Latency").apply {
            color = 0xFF22C55E.toInt()
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = 0xFF22C55E.toInt()
            fillAlpha = 51 
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        binding.lineChart.data = LineData(dataSet)
        binding.lineChart.xAxis.setLabelCount(6, true)
        binding.lineChart.notifyDataSetChanged()
        binding.lineChart.invalidate()
    }

    inner class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
        private var logsList = listOf<MonitoringLog>()

        fun submitList(newLogs: List<MonitoringLog>) {
            logsList = newLogs
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemHistoryLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val log = logsList[position]
            holder.binding.apply {
                val timeSdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                tvTime.text = timeSdf.format(Date(log.timestamp))
                tvStatus.text = log.status
                tvLatency.text = "${log.latency}ms"
                
                val color = if (log.status == "UP") 0xFF22C55E.toInt() else 0xFFEF4444.toInt()
                ivStatusIcon.setColorFilter(color)
            }
        }

        override fun getItemCount() = logsList.size

        inner class ViewHolder(val binding: ItemHistoryLogBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
