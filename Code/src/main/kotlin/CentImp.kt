import org.jgrapht.graph.SimpleGraph
import java.util.*
import kotlin.collections.HashSet

class CentImp<V, E>(graph: SimpleGraph<V, E>) : Algo<V, E>(graph) {

    var checkedSubsets = 0
        private set

    override fun preparations() {
        createVertexLists()
        calcNeighborhoods()
        createDistanceMatrix()
    }

    override fun calcResult() : Triple<Set<V>, Int, Float> {
        // resetting information variables
        checkedSubsets = 0

        // variables for keeping track of the best result
        var bestCentrality = -1
        var bestIndices = IntArray(k)

        // variables for iterating over the subsets
        var nextAction = NextAction.STAY
        val setIndices = IntArray(k) { -1 }
        var nextI = 0
        var spacesLeft = k - nextI
        val centImprovements = Array(k) { IntArray(vertCount) }
        val vertOrder = Array(k) { i -> TreeSet<Int> { vi1, vi2 ->
            (centImprovements[i][vi2] - centImprovements[i][vi1]).let { dif -> if (dif == 0) 1 else dif }
        } }

        // variables for dynamically saving important values while iterating over the subsets
        var curVI: Int
        var curCentrality = 0
        val centralities = IntArray(k)
        val tempDistances = Array(k) { IntArray(vertCount) }
        val centImprovementSums = IntArray(k)
        val topImprovementVerts : Array<Deque<Int>> = Array(k) { LinkedList() }

        // initialization
        for (vi in vertexList.indices) {
            centImprovements[nextI][vi] = vertCount - vi
            vertOrder[nextI].add(vi)
        }
        for (i in 0 until spacesLeft) {
            topImprovementVerts[nextI].addLast(vertOrder[nextI].pollFirst()!!)
        }

        if (k == 1) {
            // handle special case of size == 1
            while (vertOrder[0].isNotEmpty()) {
                curVI = vertOrder[0].pollFirst()!!
                curCentrality = distanceMatrix[curVI].sum()
                checkedSubsets++
                if (bestCentrality == -1 || curCentrality < bestCentrality) {
                    bestCentrality = curCentrality
                    bestIndices = intArrayOf(curVI)
                }
            }
        } else {
            while (nextI > 0 || nextAction != NextAction.UP) {
                when (nextAction) {
                    NextAction.UP -> {
                        // clear variables
                        vertOrder[nextI].clear()
                        centImprovementSums[nextI] = 0
                        topImprovementVerts[nextI].clear()
                        // go back to previous Index
                        nextI--
                        spacesLeft = k - nextI
                        curCentrality = centralities[nextI]
                        // check if there are enough vertices left to keep going
                        if (topImprovementVerts[nextI].size == spacesLeft) {
                            nextAction = NextAction.STAY
                        }
                        continue
                    }
                    NextAction.STAY -> {
                        if (nextI > 0) {
                            // CENTRALITY LOWER BOUND
                            // check if the sum is too low
                            if (bestCentrality != -1 && centralities[nextI - 1] - centImprovementSums[nextI] >= bestCentrality) {
                                nextAction = NextAction.UP
                                continue
                            } else {
                                // IMPROVEMENT LOWER BOUND
                                // remove vertices with an improvement that is too low
                                var lb = centImprovementSums[nextI] - centImprovements[nextI][topImprovementVerts[nextI].last()]
                                if (bestCentrality != -1 && curCentrality - lb > bestCentrality) {
                                    lb = curCentrality - lb - bestCentrality
                                    while (vertOrder[nextI].isNotEmpty() && centImprovements[nextI][vertOrder[nextI].last()] <= lb) {
                                        vertOrder[nextI].pollLast()
                                    }
                                    if (vertOrder[nextI].isEmpty() && centImprovements[nextI][topImprovementVerts[nextI].last()] <= lb) {
                                        nextAction = NextAction.UP
                                        continue
                                    }
                                }
                            }
                        }
                        // fill empty spot with next vertex
                        curVI = topImprovementVerts[nextI].pollFirst()!!
                        setIndices[nextI] = curVI
                        // calculate curCentrality and update tempDistances
                        curCentrality = if (nextI == 0) {
                            tempDistances[nextI] = distanceMatrix[curVI]
                            tempDistances[nextI].sum()
                        } else {
                            combineDistances(distanceMatrix[curVI], tempDistances[nextI - 1], tempDistances[nextI])
                        }
                        centralities[nextI] = curCentrality
                        // update topImprovements and centImprovementSums
                        centImprovementSums[nextI] -= centImprovements[nextI][curVI]
                        if (vertOrder[nextI].isNotEmpty()) {
                            val top = vertOrder[nextI].pollFirst()!!
                            topImprovementVerts[nextI].addLast(top)
                            centImprovementSums[nextI] += centImprovements[nextI][top]
                        }
                        nextAction = NextAction.DOWN
                        continue
                    }
                    NextAction.DOWN -> {
                        // go to next Index
                        nextI++
                        spacesLeft = k - nextI
                        if (spacesLeft > 1) {
                            // IMPROVEMENT LOWER BOUND 2
                            // only add vertices that had a good enough centrality in the previous iteration
                            val last = topImprovementVerts[nextI - 1].pollLast()!!
                            var lb = centImprovementSums[nextI - 1] - centImprovements[nextI - 1][last] - centImprovements[nextI - 1][topImprovementVerts[nextI - 1].last()]
                            topImprovementVerts[nextI - 1].addLast(last)
                            lb = if (nextI > 1 && curCentrality - lb > bestCentrality) {
                                curCentrality - lb - bestCentrality
                            } else 0
                            // calculate centImprovements and add to vertOrder
                            for (vi in topImprovementVerts[nextI - 1]) {
                                if (bestCentrality != -1 && centImprovements[nextI - 1][vi] <= lb) break
                                centImprovements[nextI][vi] = curCentrality - calcCombinedCentrality(tempDistances[nextI - 1], distanceMatrix[vi])
                                vertOrder[nextI].add(vi)
                            }
                            for (vi in vertOrder[nextI - 1]) {
                                if (bestCentrality != -1 && centImprovements[nextI - 1][vi] <= lb) break
                                centImprovements[nextI][vi] = curCentrality - calcCombinedCentrality(tempDistances[nextI - 1], distanceMatrix[vi])
                                vertOrder[nextI].add(vi)
                            }

                            if (vertOrder[nextI].size < spacesLeft) {
                                nextAction = NextAction.UP
                            } else {
                                // fill topImprovements
                                for (i in 0 until spacesLeft) {
                                    val top = vertOrder[nextI].pollFirst()!!
                                    centImprovementSums[nextI] += centImprovements[nextI][top]
                                    topImprovementVerts[nextI].addLast(top)
                                }
                                nextAction = NextAction.STAY
                            }
                        } else {
                            // handle special case with spacesLeft == 1
                            val checkVertex = { vi : Int ->
                                setIndices[nextI] = vi
                                // IMPROVEMENT LOWER BOUND 2
                                // check if improvement from previous step is big enough
                                if (k <= 2 || bestCentrality == -1 || curCentrality - centImprovements[nextI - 1][vi] < bestCentrality) {
                                    // calc and check centrality
                                    val cent = calcCombinedCentrality(distanceMatrix[vi], tempDistances[nextI - 1])
                                    if (bestCentrality == -1 || cent < bestCentrality) {
                                        // update best centrality
                                        bestCentrality = cent
                                        bestIndices = setIndices.clone()
                                    }
                                    checkedSubsets++
                                    true
                                } else {
                                    // stop checking here because the improvements after this will all be smaller
                                    false
                                }
                            }
                            for (vi in topImprovementVerts[nextI - 1]) {
                                if (!checkVertex(vi)) break
                            }
                            for (vi in vertOrder[nextI - 1]) {
                                if (!checkVertex(vi)) break
                            }
                            nextAction = NextAction.UP
                        }
                        continue
                    }
                }
            }
        }

        val normCentrality = (vertCount - k).toFloat() / bestCentrality.toFloat()
        val bestSet = HashSet<V>(bestIndices.size)
        bestIndices.forEach { bestSet.add(vertexList[it]) }
        return Triple(bestSet, bestCentrality, normCentrality)
    }
}