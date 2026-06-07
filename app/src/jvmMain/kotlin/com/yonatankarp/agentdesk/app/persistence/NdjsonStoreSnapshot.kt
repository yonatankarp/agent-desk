package com.yonatankarp.agentdesk.app.persistence

/**
 * Store-neutral snapshot read by [AppendOnlyNdjsonStore]: the committed
 * records plus the torn-trailing signal as plain values, adapted by each
 * repository into its own public read-result type.
 */
internal data class NdjsonStoreSnapshot<R : Any, ID : Any>(
    val records: List<R>,
    val recordIds: Set<ID>,
    val tornTrailingLineNumber: Int? = null,
    val endsWithoutNewline: Boolean = false,
)
