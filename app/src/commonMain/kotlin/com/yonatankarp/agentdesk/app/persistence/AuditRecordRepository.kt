package com.yonatankarp.agentdesk.app.persistence

import com.yonatankarp.agentdesk.app.operator.audit.AuditEntry

interface AuditRecordRepository {
    fun append(entry: AuditEntry)

    fun readAll(): AuditRecordReadResult
}
