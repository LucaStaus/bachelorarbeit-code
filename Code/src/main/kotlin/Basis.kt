import org.jgrapht.graph.SimpleGraph

class Basis<V, E>(graph: SimpleGraph<V, E>) : Algo<V, E>(graph) {

    var checkedSubsets = 0
        private set

    override fun preparations() {
        createVertexLists()
        calcNeighborhoods()
        createDistanceMatrix()
    }

    override fun calcResult(): Triple<Set<V>, Int, Float> {
        // resetting information variables
        checkedSubsets = 0

        // variables for keeping track of the best result
        var bestCentrality = -1
        var bestIndices = IntArray(0)

        // variables for iterating over the subsets
        var nextAction = NextAction.STAY
        val setIndices = IntArray(k) { -1 }
        var nextI = 0
        var spacesLeft = k - nextI

        // variables for dynamically saving important values while iterating over the subsets
        var curVI: Int
        var curCentrality = 0
        val tempDistances = Array(k) { IntArray(vertCount) }

        while (nextI > 0 || nextAction != NextAction.UP) {
            when (nextAction) {
                NextAction.UP -> {
                    nextI--
                    spacesLeft = k - nextI
                    nextAction = NextAction.STAY
                }
                NextAction.STAY -> {
                    if (setIndices[nextI] < vertCount - spacesLeft) {
                        // modify indices
                        curVI = ++setIndices[nextI]
                        // update tempDistances
                        curCentrality = if (nextI == 0) {
                            tempDistances[nextI] = distanceMatrix[curVI]
                            tempDistances[nextI].sum()
                        } else {
                            combineDistances(distanceMatrix[curVI], tempDistances[nextI - 1], tempDistances[nextI])
                        }
                        if (nextI == k - 1) {
                            // check subset
                            if (bestCentrality == -1 || curCentrality < bestCentrality) {
                                bestCentrality = curCentrality
                                bestIndices = setIndices.clone()
                            }
                            checkedSubsets++
                        } else {
                            nextAction = NextAction.DOWN
                        }
                    } else {
                        nextAction = NextAction.UP
                    }
                }
                NextAction.DOWN -> {
                    nextI++
                    spacesLeft = k - nextI
                    setIndices[nextI] = setIndices[nextI - 1]
                    nextAction = NextAction.STAY
                }
            }
        }
        val normCentrality = (vertCount - k).toFloat() / bestCentrality.toFloat()
        val bestSet = HashSet<V>(k)
        bestIndices.forEach { bestSet.add(vertexList[it]) }
        return Triple(bestSet, bestCentrality, normCentrality)
    }
}