package org.cis_india.wsreader.utils.extensions

import java.text.NumberFormat

fun Number.formatPercentage(maximumFractionDigits: Int = 0): String {
    val format = NumberFormat.getPercentInstance()
    format.maximumFractionDigits = maximumFractionDigits
    return format.format(this)
}
