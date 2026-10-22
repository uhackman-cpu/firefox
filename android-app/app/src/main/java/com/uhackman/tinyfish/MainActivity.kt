package com.uhackman.tinyfish

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.net.URLEncoder

class MainActivity : Activity() {
 private val tabs = mutableListOf<WebView>()
 private var active = 0
 private lateinit var tabStrip: LinearLayout
 private lateinit var address: EditText
 private lateinit var browserHost: LinearLayout
 private lateinit var status: TextView
 private val dark = Color.rgb(14,17,23)
 private val blue = Color.rgb(25,103,210)
 private val teal = Color.rgb(0,137,123)

 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.statusBarColor = dark; window.navigationBarColor = dark
  buildUi(); addTab("https://www.google.com")
 }
 private fun buildUi() {
  val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(dark); setPadding(8,8,8,8) }
  root.addView(TextView(this).apply { text="TINYFISH • BROWSER"; textSize=18f; setTextColor(Color.WHITE); gravity=Gravity.CENTER_VERTICAL; setPadding(8,8,8,12) })
  tabStrip=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
  root.addView(tabStrip,LinearLayout.LayoutParams(-1,dp(44)))
  val nav=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
  nav.addView(button("‹"){ current()?.let{if(it.canGoBack())it.goBack()} },lp(40))
  nav.addView(button("›"){ current()?.let{if(it.canGoForward())it.goForward()} },lp(40))
  nav.addView(button("↻"){current()?.reload()},lp(40))
  address=EditText(this).apply {
   singleLine=true; textSize=14f; setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY); hint="URL or search"
   setBackgroundColor(Color.rgb(35,40,48)); imeOptions=EditorInfo.IME_ACTION_GO; setPadding(dp(8),0,dp(8),0)
   setOnEditorActionListener{_,a,_->if(a==EditorInfo.IME_ACTION_GO||a==EditorInfo.IME_ACTION_DONE){navigate(text.toString());true}else false}
  }
  nav.addView(address,LinearLayout.LayoutParams(0,-1,1f)); root.addView(nav,LinearLayout.LayoutParams(-1,dp(48)))
  val actions=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  actions.addView(button("+ TAB"){addTab("https://www.google.com")},LinearLayout.LayoutParams(0,dp(42),1f))
  actions.addView(button("GO"){navigate(address.text.toString())},LinearLayout.LayoutParams(0,dp(42),1f))
  actions.addView(button("PC"){Toast.makeText(this@MainActivity,"Desktop mode is not implemented yet",Toast.LENGTH_SHORT).show()},LinearLayout.LayoutParams(0,dp(42),1f))
  root.addView(actions)
  status=TextView(this).apply{text="Ready";textSize=11f;setTextColor(Color.LTGRAY);setPadding(6,4,6,4)}
  root.addView(status)
  browserHost=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.WHITE)}
  root.addView(browserHost,LinearLayout.LayoutParams(-1,0,1f)); setContentView(root)
 }
 private fun addTab(url:String) {
  val web=WebView(this)
  web.settings.javaScriptEnabled=true; web.settings.domStorageEnabled=true
  web.settings.javaScriptCanOpenWindowsAutomatically=false; web.settings.setSupportMultipleWindows(false)
  web.webViewClient=object:WebViewClient(){
   override fun onPageStarted(v:WebView,url:String?,icon:android.graphics.Bitmap?){address.setText(url.orEmpty());status.text="Loading…";refreshTabs()}
   override fun onPageFinished(v:WebView,url:String?){address.setText(url.orEmpty());status.text=url.orEmpty();refreshTabs()}
  }
  web.webChromeClient=object:WebChromeClient(){
   override fun onProgressChanged(v:WebView,p:Int){status.text="Loading $p%"}
  }
  tabs.add(web);active=tabs.lastIndex;browserHost.removeAllViews();browserHost.addView(web,ViewGroup.LayoutParams(-1,-1));refreshTabs();web.loadUrl(url)
 }
 private fun refreshTabs(){
  if(!::tabStrip.isInitialized)return
  tabStrip.removeAllViews()
  tabs.forEachIndexed{index,web->
   val title=web.title?.take(10)?.ifBlank{"Tab ${index+1}"} ?: "Tab ${index+1}"
   tabStrip.addView(button(if(index==active)"● $title" else title){switchTab(index)},LinearLayout.LayoutParams(0,-1,1f))
  }
 }
 private fun switchTab(index:Int){if(index !in tabs.indices)return;active=index;browserHost.removeAllViews();browserHost.addView(tabs[index],ViewGroup.LayoutParams(-1,-1));address.setText(tabs[index].url.orEmpty());refreshTabs()}
 private fun navigate(raw:String){
  val input=raw.trim();if(input.isEmpty())return
  val url=when{input.startsWith("https://")||input.startsWith("http://")->input;input.contains(".")&&!input.contains(" ")->"https://$input";else->"https://www.google.com/search?q="+URLEncoder.encode(input,"UTF-8")}
  current()?.loadUrl(url)
 }
 private fun current():WebView?=tabs.getOrNull(active)
 private fun button(label:String,click:()->Unit)=Button(this).apply{text=label;textSize=11f;isAllCaps=false;setTextColor(Color.WHITE);setBackgroundColor(if(label.contains("TAB"))teal else blue);setPadding(2,0,2,0);setOnClickListener{click()}}
 private fun lp(width:Int)=LinearLayout.LayoutParams(dp(width),-1)
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 @Deprecated("Deprecated in Android")
 override fun onBackPressed(){val w=current();if(w?.canGoBack()==true)w.goBack()else super.onBackPressed()}
 override fun onDestroy(){tabs.forEach{it.stopLoading();it.destroy()};tabs.clear();super.onDestroy()}
}
