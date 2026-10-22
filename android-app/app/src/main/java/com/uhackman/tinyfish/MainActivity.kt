package com.uhackman.tinyfish

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import java.net.URLEncoder

/** TinyFish browser UI with Mozilla GeckoView, the engine used by Firefox. */
class MainActivity : Activity() {
    private data class BrowserTab(val session: GeckoSession, val view: GeckoView, val number: Int)

    private val tabs = mutableListOf<BrowserTab>()
    private var active = -1
    private lateinit var tabStrip: LinearLayout
    private lateinit var address: EditText
    private lateinit var browserHost: FrameLayout
    private lateinit var status: TextView

    private val chrome = Color.rgb(24, 26, 32)
    private val panel = Color.rgb(37, 40, 48)
    private val accent = Color.rgb(255, 119, 0)
    private val muted = Color.rgb(183, 188, 198)

    companion object {
        @Volatile private var sharedRuntime: GeckoRuntime? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = chrome
        window.navigationBarColor = chrome
        buildUi()
        addTab("https://www.google.com")
    }

    private fun runtime(): GeckoRuntime {
        return sharedRuntime ?: synchronized(MainActivity::class.java) {
            sharedRuntime ?: GeckoRuntime.create(this).also { sharedRuntime = it }
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(chrome)
            setPadding(dp(6), dp(5), dp(6), dp(3))
        }

        root.addView(TextView(this).apply {
            text = "TINYFISH  •  GECKO"
            textSize = 17f
            letterSpacing = 0.06f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(7), dp(5), dp(7), dp(8))
        }, LinearLayout.LayoutParams(-1, dp(38)))

        tabStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        root.addView(tabStrip, LinearLayout.LayoutParams(-1, dp(42)))

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        nav.addView(button("‹") { current()?.goBack() }, width(38))
        nav.addView(button("›") { current()?.goForward() }, width(38))
        nav.addView(button("↻") { current()?.reload() }, width(38))

        address = EditText(this).apply {
            singleLine = true
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            hint = "Search or enter address"
            setBackgroundColor(panel)
            imeOptions = EditorInfo.IME_ACTION_GO
            setPadding(dp(10), 0, dp(8), 0)
            setSelectAllOnFocus(true)
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) {
                    navigate(text.toString())
                    true
                } else false
            }
        }
        nav.addView(address, LinearLayout.LayoutParams(0, dp(42), 1f))
        root.addView(nav, LinearLayout.LayoutParams(-1, dp(46)))

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        actions.addView(button("+ New tab") { addTab("https://www.google.com") },
            LinearLayout.LayoutParams(0, dp(40), 1f))
        actions.addView(button("Open") { navigate(address.text.toString()) },
            LinearLayout.LayoutParams(0, dp(40), 1f))
        actions.addView(button("Close tab") { closeCurrentTab() },
            LinearLayout.LayoutParams(0, dp(40), 1f))
        root.addView(actions, LinearLayout.LayoutParams(-1, dp(42)))

        status = TextView(this).apply {
            text = "Firefox engine • GeckoView"
            textSize = 11f
            setTextColor(muted)
            setPadding(dp(7), dp(3), dp(7), dp(3))
        }
        root.addView(status, LinearLayout.LayoutParams(-1, dp(24)))

        browserHost = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        root.addView(browserHost, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun addTab(url: String) {
        val session = GeckoSession()
        session.setContentDelegate(object : GeckoSession.ContentDelegate {})
        val view = GeckoView(this)

        session.setProgressDelegate(object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                runOnUiThread {
                    if (activeSession() === session) {
                        address.setText(url)
                        status.text = "Loading…"
                    }
                }
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                runOnUiThread {
                    if (activeSession() === session) {
                        address.setText(session.uri.orEmpty())
                        status.text = if (success) "Page loaded" else "Page load failed"
                        refreshTabs()
                    }
                }
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                runOnUiThread {
                    if (activeSession() === session) status.text = "Loading $progress%"
                }
            }
        })

        session.open(runtime())
        view.setSession(session)
        val tab = BrowserTab(session, view, tabs.size + 1)
        tabs.add(tab)
        browserHost.addView(view, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        switchTab(tabs.lastIndex)
        session.loadUri(url)
    }

    private fun switchTab(index: Int) {
        if (index !in tabs.indices) return
        active = index
        tabs.forEachIndexed { i, tab ->
            tab.view.visibility = if (i == active) android.view.View.VISIBLE else android.view.View.GONE
        }
        address.setText(tabs[index].session.uri.orEmpty())
        status.text = "Tab ${tabs[index].number} • GeckoView"
        refreshTabs()
    }

    private fun refreshTabs() {
        if (!::tabStrip.isInitialized) return
        tabStrip.removeAllViews()
        tabs.forEachIndexed { index, tab ->
            val label = if (index == active) "● ${tab.number}" else "${tab.number}"
            tabStrip.addView(button(label) { switchTab(index) },
                LinearLayout.LayoutParams(0, -1, 1f))
        }
        if (tabs.isEmpty()) status.text = "No open tabs"
    }

    private fun navigate(raw: String) {
        val input = raw.trim()
        if (input.isEmpty()) return
        val url = when {
            input.startsWith("https://", true) || input.startsWith("http://", true) -> input
            input.contains(".") && !input.contains(" ") -> "https://$input"
            else -> "https://www.google.com/search?q=" + URLEncoder.encode(input, "UTF-8")
        }
        current()?.loadUri(url)
    }

    private fun closeCurrentTab() {
        if (active !in tabs.indices) return
        if (tabs.size == 1) {
            tabs[0].session.loadUri("about:home")
            address.setText("")
            status.text = "Home"
            return
        }
        val removed = tabs.removeAt(active)
        browserHost.removeView(removed.view)
        removed.session.close()
        tabs.indices.forEach { index ->
            val old = tabs[index]
            tabs[index] = old.copy(number = index + 1)
        }
        active = (active - 1).coerceAtLeast(0)
        switchTab(active)
    }

    private fun current(): GeckoSession? = tabs.getOrNull(active)?.session
    private fun activeSession(): GeckoSession? = current()

    private fun button(label: String, click: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 11f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(if (label.startsWith("+")) accent else panel)
            setPadding(dp(2), 0, dp(2), 0)
            minWidth = 0
            minimumWidth = 0
            setOnClickListener { click() }
        }
    }

    private fun width(value: Int) = LinearLayout.LayoutParams(dp(value), dp(42))
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        tabs.forEach { tab ->
            tab.view.releaseSession()
            tab.session.close()
        }
        tabs.clear()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        val session = current()
        if (session != null) session.goBack() else super.onBackPressed()
    }
}
