import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleGraph
import java.io.File
import java.lang.NumberFormatException

fun importSimpleGraphFromFile(path: String) : SimpleGraph<Int, DefaultEdge> {
    val result = SimpleGraph<Int, DefaultEdge>(DefaultEdge::class.java)
    val regex = Regex("[0-9]+ [0-9]+")
    val inputFile = File(path)
    inputFile.forEachLine {
        val line = cleanLine(it)
        if (line.matches(regex)) {
            val nums = line.split(' ', '\t')
            if (nums.size == 2) {
                try {
                    val first = Integer.parseInt(nums[0])
                    val second = Integer.parseInt(nums[1])
                    result.addVertex(first)
                    result.addVertex(second)
                    if (first != second) result.addEdge(first, second)
                } catch (e: NumberFormatException) {
                    println("Could not parse Int")
                }
            }
        }
    }
    return result
}

fun cleanLine(curLine: String) : String {
    var result = curLine.replace('\t', ' ')
    while (result.contains("  ")) {
        result = result.replace("  ", " ")
    }
    while (result.startsWith(" ")) {
        result = result.removePrefix(" ")
    }
    while (result.endsWith(" ")) {
        result = result.removeSuffix(" ")
    }
    return result
}