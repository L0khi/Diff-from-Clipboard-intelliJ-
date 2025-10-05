package com.yourorg

data class Hunk(val removals: List<String>, val additions: List<String>)

object DiffParser {
    fun parse(diff: String): List<Hunk> {
        val hunks = mutableListOf<Hunk>()
        var removals = mutableListOf<String>()
        var additions = mutableListOf<String>()

        diff.lines().forEach { line ->
            when {
                line.startsWith("-") && !line.startsWith("---") -> removals.add(line.removePrefix("-"))
                line.startsWith("+") && !line.startsWith("+++") -> additions.add(line.removePrefix("+"))
                line.startsWith("@@") -> {
                    if (removals.isNotEmpty() || additions.isNotEmpty()) {
                        hunks.add(Hunk(removals, additions))
                        removals = mutableListOf()
                        additions = mutableListOf()
                    }
                }
            }
        }
        if (removals.isNotEmpty() || additions.isNotEmpty()) {
            hunks.add(Hunk(removals, additions))
        }
        return hunks
    }
}
