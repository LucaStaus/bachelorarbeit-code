import ilog.concert.IloIntVar
import ilog.cplex.IloCplex
import org.jgrapht.graph.SimpleGraph
import java.io.PrintStream

class ILPnew<V, E>(graph: SimpleGraph<V, E>) : Algo<V, E>(graph) {

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
        val x = Array(vertCount) { cplex.boolVarArray(diameter + 1).also { xi -> cplex.add(xi) } }

        // constraints
        cplex.addEq(cplex.sum(Array(vertCount) { x[it][0] }), k.toDouble())

        for (i in 0 until vertCount) {
            cplex.addEq(cplex.sum(x[i]), 1.0)
        }

        for (i in 0 until vertCount) {
            for (di in 1..diameter) {
                val list = mutableListOf<IloIntVar>()
                for (j in 0 until vertCount) {
                    if (distanceMatrix[i][j] == di) {
                        list.add(x[j][0])
                    }
                }
                cplex.addLe(x[i][di], cplex.sum(list.toTypedArray()))
            }
        }

        // objective function
        val intExpr = cplex.linearIntExpr()
        for (i in 0 until vertCount) {
            for (di in 0..diameter) {
                intExpr.addTerm(x[i][di], di)
            }
        }
        cplex.addMinimize(intExpr)

        // solve
        cplex.solve()

        // retrieve solution
        val bestSet = HashSet<V>(k)
        val bestSetIndices = HashSet<Int>()
        for (i in 0 until vertCount) {
            if (cplex.getValue(x[i][0]) >= 0.999) {
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