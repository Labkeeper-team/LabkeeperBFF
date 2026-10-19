package io.labkeeper.web.frontend.internal.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LandingPagesTest {
    @Test
    fun blankMetrikaKeyBecomesUndefinedLiteral() {
        assertEquals("undefined", LandingPages.metrikaLiteral(null))
        assertEquals("undefined", LandingPages.metrikaLiteral("  "))
        assertEquals("123456", LandingPages.metrikaLiteral("123456"))
    }
}
