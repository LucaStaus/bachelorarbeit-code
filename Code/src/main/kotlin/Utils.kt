import org.jgrapht.alg.connectivity.ConnectivityInspector
import org.jgrapht.graph.SimpleGraph
import kotlin.math.min

fun prettyTime(millis: Long) : String {
    var h = 0L
    var m = 0L
    var s = 0L
    var ms = millis
    if (ms >= 3600000L) {
        h = ms / 3600000L
        ms %= 3600000L
    }
    if (ms >= 60000L) {
        m = ms / 60000L
        ms %= 60000L
    }
    if (ms >= 1000L) {
        s = ms / 1000L
        ms %= 1000L
    }
    return "%d:%02d:%02d.%03d".format(h, m, s, ms)
}

fun <V> checkIfSubset(sub: Set<V>, ignore: V, top: Set<V>) : Boolean {
    if (sub.size - 1 > top.size)
        return false
    for (e in sub) {
        if (e !in top && e != ignore) return false
    }
    return true
}

fun <V> checkIfSubset(sub: Set<V>, subIgnore: Set<V>, top: Set<V>) : Boolean {
    if (sub.size - subIgnore.size > top.size)
        return false
    for (e in sub) {
        if (e !in top && e !in subIgnore) return false
    }
    return true
}

fun <V,E> reduceToBiggestConnectedSubGraph(graph: SimpleGraph<V,E>) {
    val conInspector = ConnectivityInspector(graph)
    val biggest = conInspector.connectedSets().maxByOrNull { it.size }!!
    val remove = graph.vertexSet().filter { it !in biggest }
    graph.removeAllVertices(remove)
}

fun combineDistances(from1: IntArray, from2: IntArray, to: IntArray) : Int {
    var sum = 0
    for (i in to.indices) {
        to[i] = min(from1[i], from2[i])
        sum += to[i]
    }
    return sum
}

fun calcCombinedCentrality(from1: IntArray, from2: IntArray) : Int {
    var sum = 0
    for (i in from1.indices) {
        sum += min(from1[i], from2[i])
    }
    return sum
}