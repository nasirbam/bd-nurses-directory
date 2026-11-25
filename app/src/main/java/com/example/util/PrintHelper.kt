package com.example.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.DirectoryConstants
import com.example.data.NurseRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrintHelper {
  private var activeWebView: WebView? = null

  private fun escHtml(input: String): String =
    input
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")

  fun buildPrintHtml(
    records: List<NurseRecord>,
    selectedKeys: Set<String>,
    filterSummary: String,
  ): String {
    val cols = DirectoryConstants.PRINT_FIELDS.filter { it.first in selectedKeys }
    val today = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date())
    val summaryText = escHtml(filterSummary.ifBlank { "All records" })

    val headerCells = cols.joinToString("") { "<th>${escHtml(it.second)}</th>" }
    val bodyRows =
      records.mapIndexed { index, nurse ->
        val cells =
          cols.joinToString("") { (key, _) ->
            "<td>${escHtml(DirectoryConstants.fieldValue(nurse, key))}</td>"
          }
        "<tr><td>${index + 1}</td>$cells</tr>"
      }.joinToString("")

    return """
      <!DOCTYPE html>
      <html lang="bn">
      <head>
        <meta charset="utf-8">
        <style>
          @page { size: A4 landscape; margin: 10mm; }
          body { font-family: sans-serif; color: #000; background: #fff; margin: 0; }
          h2 { margin: 0; font-size: 18px; }
          p { margin: 4px 0 10px; font-size: 12px; color: #333; }
          table { border-collapse: collapse; width: 100%; font-size: 11px; }
          th, td { border: 1px solid #444; padding: 4px 6px; text-align: left; }
          th { background: #e5e5e5; }
        </style>
      </head>
      <body>
        <h2>BD Nurses Directory</h2>
        <p>$summaryText — মোট: ${records.size} জন — $today</p>
        <table>
          <thead><tr><th>#</th>$headerCells</tr></thead>
          <tbody>$bodyRows</tbody>
        </table>
      </body>
      </html>
    """.trimIndent()
  }

  fun printDirectory(
    context: Context,
    records: List<NurseRecord>,
    selectedKeys: Set<String>,
    filterSummary: String,
  ) {
    val html = buildPrintHtml(records, selectedKeys, filterSummary)
    val webView = WebView(context)
    activeWebView = webView
    webView.webViewClient =
      object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
          false

        override fun onPageFinished(view: WebView?, url: String?) {
          val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
          if (printManager != null && view != null) {
            val jobName = "BD_Nurses_Directory"
            val adapter = view.createPrintDocumentAdapter(jobName)
            val attrs =
              PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
                .build()
            printManager.print(jobName, adapter, attrs)
          }
          activeWebView = null
        }
      }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
  }
}
