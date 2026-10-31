package io.labkeeper.web.frontend.internal.blog

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BlogExampleYamlTest {
    @Test
    fun mapExampleKeepsOneSegmentPerType() {
        val segments = exampleSegments(
            mapOf("markdown" to "# Notes", "latex" to "E = mc^2"),
            "article",
        )

        segments.map { it.type } shouldContainExactly listOf("latex", "md")
        segments.all { it.parameters.visible } shouldBe true
        segments.all { !it.parameters.hideValue } shouldBe true
    }

    @Test
    fun listExampleKeepsOrderRepeatedTypesAndParameters() {
        val segments = exampleSegments(
            listOf(
                mapOf(
                    "type" to "compute",
                    "text" to "a = 1",
                    "parameters" to mapOf("hideValue" to true, "visible" to true),
                ),
                mapOf("type" to "latex", "text" to "first"),
                mapOf(
                    "type" to "latex",
                    "text" to "second",
                    "parameters" to mapOf("visible" to false, "hideAssignment" to true),
                ),
                mapOf("type" to "md", "text" to "third"),
                mapOf("type" to "asciimath", "text" to "a = 1"),
            ),
            "article",
        )

        segments.map { it.type } shouldContainExactly listOf(
            "computational",
            "latex",
            "latex",
            "md",
            "asciimath",
        )
        segments.map { it.text } shouldContainExactly listOf("a = 1", "first", "second", "third", "a = 1")
        segments[0].parameters.hideValue shouldBe true
        segments[1].parameters.visible shouldBe true
        segments[1].parameters.hideAssignment shouldBe false
        segments[2].parameters.visible shouldBe false
        segments[2].parameters.hideAssignment shouldBe true
    }

    @Test
    fun unknownSegmentParameterFails() {
        shouldThrow<IllegalStateException> {
            exampleSegments(
                listOf(mapOf("type" to "latex", "text" to "x", "parameters" to mapOf("bold" to true))),
                "article",
            )
        }
    }
}
