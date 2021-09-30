import ilog.concert.IloIntVar
import ilog.cplex.IloCplex
import org.jgrapht.graph.SimpleGraph
import java.io.PrintStream

class ILPold<V, E>(graph: SimpleGraph<V, E>) : Algo<V, E>(graph) {

    override fun preparations() {
        createVertexLists()
        calcNeighborhoods()
        createDistanceMatrix()
    }

    override fun calcResult() : Triple<Set<V>, Int, Float> {
        val cplex = IloCplex()
        cplex.setParam(IloCplex.Param.Threads, 1) // Solve the problem in a single thread
        cplex.setOut(PrintStream.nullOutputStream())

        // decision variables
        val x = Array(vertCount) { cplex.boolVarArray(vertCount).also { xi -> cplex.add(xi) } }
        val y = cplex.boolVarArray(vertCount)
        cplex.add(y)

        // constraints
        for (i in 0 until vertCount) {
            cplex.addEq(cplex.sum(x[i]), 1.0)
        }

        cplex.addEq(cplex.sum(y), k.toDouble())

        for (i in 0 until vertCount) {
            for (j in 0 until vertCount) {
                cplex.addLe(x[i][j], y[j])
            }
        }

        // objective function
        val intExpr = cplex.linearIntExpr()
        for (i in 0 until vertCount) {
            for (j in 0 until vertCount) {
                intExpr.addTerm(x[i][j], distanceMatrix[i][j])
            }
        }
        cplex.addMinimize(intExpr)

        // solve
        cplex.solve()

        // retrieve solution
        val bestSet = HashSet<V>(k)
        val bestSetIndices = HashSet<Int>()
        for (i in 0 until vertCount) {
            if (cplex.getValue(y[i]) >= 0.999) {
                bestSet.add(vertexList[i])
                bestSetIndices.add(i)
            }
        }
        var cent = 0
        for (i in vertexList.indices) {
            cent += bestSetIndices.minOf { distanceMatrix[i][it] }
        }
        return Triple(bestSet, cent, (vertCount - k).toFloat() / cent.toFloat())
    }

}