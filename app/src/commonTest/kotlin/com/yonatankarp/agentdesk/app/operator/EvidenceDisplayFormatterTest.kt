package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.testfixtures.commitEvidence
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class EvidenceDisplayFormatterTest :
    FunSpec({
        test("formats evidence kind label and target consistently") {
            EvidenceDisplayFormatter.format(
                EvidenceLine(
                    kind = "commit",
                    label = "Implementation commit",
                    target = "commit:80de32988617392e1f42e6c4c48c66a56aaae4c4",
                ),
            ) shouldBe "commit Implementation commit -> commit:80de32988617392e1f42e6c4c48c66a56aaae4c4"
        }

        test("formats domain evidence references with the same shape") {
            EvidenceDisplayFormatter.format(
                commitEvidence(
                    label = "Implementation commit",
                    target = "commit:80de32988617392e1f42e6c4c48c66a56aaae4c4",
                ),
            ) shouldBe "commit Implementation commit -> commit:80de32988617392e1f42e6c4c48c66a56aaae4c4"
        }
    })
