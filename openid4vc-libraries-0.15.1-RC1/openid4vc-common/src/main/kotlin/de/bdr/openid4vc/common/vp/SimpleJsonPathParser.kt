/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp

import de.bdr.openid4vc.common.vp.dcql.ArrayElementSelector
import de.bdr.openid4vc.common.vp.dcql.DistinctClaimsPathPointer
import de.bdr.openid4vc.common.vp.dcql.DistinctClaimsPathPointerSelector
import de.bdr.openid4vc.common.vp.dcql.ObjectElementSelector

/**
 * Implements a subset of JSON path that overlaps with [DistinctClaimsPathPointer] and is used by
 * HAIP and ISO 18013-7
 *
 * The following parts are implemented:
 * - `$`: Root element reference
 * - `.<name>`: Dot notation to reference child
 * - `['<name>']`: Bracket notation to reference child
 * - `[<number>]`: Bracket notation to reference array element
 */
object SimpleJsonPathParser {

    private val ARRAY_ELEMENT_REF = Regex("""\[([0-9]+)]""")

    private val OBJECT_ELEMENT_REF_SINGLE_QUOTE = Regex("""\['(\\\\|\\'|[^'])']""")

    private val OBJECT_ELEMENT_REF_DOUBLE_QUOTE = Regex("""\["(\\\\|\\"|[^"])"]""")

    private val DOT_NOTATION = Regex("""\.([^.\[]+)""")

    fun parseSimpleJsonPathToClaimsPathPointer(
        path: String,
        allowDotNotation: Boolean = true,
    ): DistinctClaimsPathPointer {
        val selectors = mutableListOf<DistinctClaimsPathPointerSelector>()

        require(path.startsWith("$")) { "Path does not start with $" }

        var match: MatchResult?
        var offset = 1

        while (offset < path.length) {
            match = ARRAY_ELEMENT_REF.matchAt(path, offset)
            if (match != null) {
                selectors.add(ArrayElementSelector(match.groupValues[1].toInt()))
                offset = match.range.last + 1
                continue
            }

            match = OBJECT_ELEMENT_REF_SINGLE_QUOTE.matchAt(path, offset)
            if (match != null) {
                val escapedName = match.groupValues[1]
                val name = unescape(escapedName, '\'')
                selectors.add(ObjectElementSelector(name))
                offset = match.range.last + 1
                continue
            }

            match = OBJECT_ELEMENT_REF_DOUBLE_QUOTE.matchAt(path, offset)
            if (match != null) {
                val escapedName = match.groupValues[1]
                val name = unescape(escapedName, '"')
                selectors.add(ObjectElementSelector(name))
                offset = match.range.last + 1
                continue
            }

            if (allowDotNotation) {
                match = DOT_NOTATION.matchAt(path, offset)
                if (match != null) {
                    selectors.add(ObjectElementSelector(match.groupValues[1]))
                    offset = match.range.last + 1
                    continue
                }
            }

            throw IllegalArgumentException("Invalid character in JSON path $path at index $offset")
        }

        return DistinctClaimsPathPointer(selectors)
    }

    private fun unescape(escaped: String, quote: Char) = escaped // TODO implement unescaping
}
