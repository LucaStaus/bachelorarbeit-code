import org.jgrapht.Graphs
import org.jgrapht.graph.SimpleGraph
import kotlin.math.max

abstract class Algo<V,E>(protected val graph: SimpleGraph<V, E>) {

    val vertCount = graph.vertexSet().size
    val edgeCount = graph.edgeSet().size

    var k = 0
        protected set
    var lastResult = Triple<Set<V>, Int, Float>(HashSet(), 0, 0f)
        protected set
    var diameter: Int = 0
        protected set

    var prepTime = 0L
        private set
    var algoTime = 0L
        private set

    private var tempTime = 0L

    protected var vertexToOrder: MutableMap<V, Int> = HashMap(0)
    protected var vertexList: List<V> = ArrayList(0)
    protected var distanceMatrix: Array<IntArray> = Array(0) { IntArray(0) }
    protected var neighborhoods: Array<Set<Int>> = Array(0) { setOf() }

    enum class NextAction { UP, STAY, DOWN }

    fun solve(k: Int) : Triple<Set<V>, Int, Float> {
        if (k < 1 || k >= vertCount)
            return Triple(HashSet(), 0, 0f)

        this.k = k
        // preparations
        tempTime = System.currentTimeMillis()
        preparations()
        prepTime = System.currentTimeMillis() - tempTime
        // calculate result
        tempTime = System.currentTimeMillis()
        lastResult = calcResult()
        algoTime = System.currentTimeMillis() - tempTime

        return lastResult
    }

    protected fun createVertexLists() {
        vertexList = graph.vertexSet().toList()
        vertexToOrder = HashMap(k)
        for (i in vertexList.indices) {
            vertexToOrder[vertexList[i]] = i
        }
    }

    protected fun calcNeighborhoods() {
        neighborhoods = Array(vertCount) { vi ->
            val res = HashSet<Int>()
            Graphs.neighborSetOf(graph, vertexList[vi]).forEach { v ->
                res.add(vertexToOrder[v]!!)
            }
            res
        }
    }

    protected fun createDistanceMatrix() {
        distanceMatrix = Array(vertCount) { vi ->
            val res = IntArray(vertCount)
            var current = HashSet<Int>()
            current.add(vi)
            var next = HashSet<Int>()
            val visited = HashSet<Int>(vertCount)
            var i = 0
            while (current.isNotEmpty()) {
                for (cur in current) {
                    res[cur] = i
                    for (vj in neighborhoods[cur]) {
                        if (vj !in visited && vj !in current) {
                            next.add(vj)
                        }
                    }
                }
                i++
                visited.addAll(current)
                current = next
                next = HashSet()
            }
            diameter = max(diameter, i - 1)
            res
        }
    }

    protected fun calcUpperBoundGreedy() : Pair<Int, IntArray> {
        val indices = IntArray(k) { -1 }
        var i = 1
        indices[0] = vertexList.indices.minByOrNull { distanceMatrix[it].sum() }!!
        val distances = Array(k) { IntArray(vertCount) }
        distances[0] = distanceMatrix[indices[0]]
        var cent = distances[0].sum()
        while (i < k) {
            for (vi in vertexList.indices) {
                if (vi in indices) continue
                val temp = calcCombinedCentrality(distances[i - 1], distanceMatrix[vi])
                if (temp < cent) {
                    indices[i] = vi
                    cent = temp
                    combineDistances(distances[i - 1], distanceMatrix[vi], distances[i])
                }
            }
            i++
        }
        return Pair(cent, indices)
    }

    protected abstract fun preparations()

    protected abstract fun calcResult() : Triple<Set<V>, Int, Float>

}