package com.yonatankarp.agentdesk.app.fixtures

import com.yonatankarp.agentdesk.testfixtures.CanonicalWorkEventFixtures
import com.yonatankarp.agentdesk.testfixtures.WorkEventFixtures

internal object AppFixtures : CanonicalWorkEventFixtures by WorkEventFixtures
