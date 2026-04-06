package com.skids.idamobile.strings

/**
 * Aggregated strings and cross-references report for one DEX payload.
 */
data class StringsReport(
    val dexEntryName: String,
    val totalStrings: Int,
    val records: List<StringRecord>,
    val wasTruncated: Boolean
)

/**
 * Full string node including all observed xrefs.
 */
data class StringRecord(
    val entry: StringEntry,
    val xrefs: List<StringXref>
)

