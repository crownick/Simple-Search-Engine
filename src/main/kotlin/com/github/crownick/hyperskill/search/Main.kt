package com.github.crownick.hyperskill.search

import com.github.crownick.hyperskill.search.SearchMode.Companion.findMode
import java.io.File

const val findPersonNum = "1"
const val printAllPeopleNum = "2"
const val exitNum = "0"

enum class SearchMode {
    ALL,
    ANY,
    NONE;

    companion object {
        fun findMode(value: String): SearchMode? {
            return values().find { it.name == value }
        }
    }
}

class Menu(private val options: Map<String, MenuOption>) {
    fun activate() {
        start@ while (true) {
            println()
            println("=== Menu ===")
            options.forEach { println(it.value.printText) }
            val answer = readln()
            when (val option = options[answer]) {
                null -> {
                    println()
                    println("Incorrect option! Try again.")
                    continue@start
                }

                else -> {
                    option.perform()
                    if (option.isTerminated()) {
                        break@start
                    }
                }
            }
        }
    }
}

abstract class MenuOption(open val printText: String) {
    abstract fun perform()
    open fun isTerminated(): Boolean {
        return false
    }
}

class FindPerson(
    private val peoples: List<String>,
    private val index: Map<String, Set<Int>>,
    private val searchService: SearchService,
) : MenuOption("$findPersonNum. Find a person") {
    override fun perform() {
        println()
        println("Select a matching strategy: ALL, ANY, NONE")
        val mode = findMode(readln())!! // TODO
        println("Enter a name or email to search all suitable people.")
        val searchWords = readln().split(" ").map { it.lowercase() }
        val lines = searchService.search(peoples, searchWords, index, mode)
        printSearchResult(lines)
    }

    private fun printSearchResult(lines: Set<Int>) {
        peoples.filterIndexed { index, _ -> lines.contains(index) }.forEach(::println)
    }
}

class SearchService {
    fun search(
        peoples: List<String>,
        searchWords: List<String>,
        index: Map<String, Set<Int>>,
        mode: SearchMode
    ): Set<Int> {
        return when (mode) {
            SearchMode.ANY -> {
                searchWords.mapNotNull { index[it] }.flatten().toSet()
            }

            SearchMode.NONE -> {
                val toExclude = searchWords.asSequence().mapNotNull { index[it] }.flatten().toSet()
                List(peoples.size) { it }.filter { !toExclude.contains(it) }.toSet()
            }

            SearchMode.ALL -> {
                val wordEntries = searchWords.mapNotNull { index[it] }
                if (wordEntries.isEmpty()) {
                    emptySet()
                } else {
                    wordEntries.map { it.toMutableSet() }.reduce { acc, entries ->
                        acc.retainAll(entries)
                        acc
                    }
                }
            }
        }
    }
}

class PrintAllPeople(private val peoples: List<String>) : MenuOption("$printAllPeopleNum. Print all people") {
    override fun perform() {
        println()
        println("=== List of people ===")
        peoples.forEach(::println)
    }
}

class Exit : MenuOption("$exitNum. Exit") {
    override fun perform() {
        println()
        println("Bye!")
    }

    override fun isTerminated(): Boolean {
        return true
    }
}

fun main(args: Array<String>) {
    validateArgs(args)
    val file = openFile(args[1])
    val peoples = preparePeopleList(file)
    val menu = Menu(
        mapOf(
            Pair(findPersonNum, FindPerson(peoples, prepareIndex(peoples), SearchService())),
            Pair(printAllPeopleNum, PrintAllPeople(peoples)),
            Pair(exitNum, Exit()),
        )
    )
    menu.activate()
}

fun validateArgs(args: Array<String>) {
    if (args.size != 2) {
        throw IllegalArgumentException("Only two arguments are required!")
    }
    if (args[0] != "--data") {
        throw IllegalArgumentException("Argument \"--data\" is not passed")
    }
}

fun openFile(path: String): File {
    val file = File(path)
    if (!file.exists()) {
        throw IllegalArgumentException("File doesn't exist")
    }
    return file
}

fun preparePeopleList(file: File): List<String> {
    return file.readLines()
}

fun prepareIndex(peoples: List<String>): Map<String, Set<Int>> {
    val result = mutableMapOf<String, MutableSet<Int>>()
    peoples.onEachIndexed { index, s ->
        val lineWords = s.split(" ").map { it.lowercase() }
        lineWords.forEach {
            val inResult = result[it]
            if (inResult == null) {
                result[it] = mutableSetOf(index)
            } else {
                inResult += index
            }
        }
    }
    return result
}