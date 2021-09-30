import org.jgrapht.GraphTests
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleGraph
import java.io.*
import java.util.*
import java.util.concurrent.*
import kotlin.concurrent.timerTask
import kotlin.math.max

const val printAdditionalInfo = true

const val debugMode = false
const val testMultipleGraphs = false

const val dataRepositoryPath = ""
const val outputFilePath = ""

fun main(args: Array<String>) {

    if (debugMode) {

        if (testMultipleGraphs) {
            autoTestGraphs(intArrayOf(9), dataRepositoryPath, 10, outputFilePath, 10, TimeUnit.MINUTES)
        } else {
            // TEST PARAMETERS
            val size = 20
            val useBasis = false
            val useCentImp = false
            val useUpperBound = false
            val useInitSort = false
            val useGCC = false
            val useILPnew = false
            val useILPold = false
            val graph = createGraph(dataRepositoryPath, 11, System.out)

            val algos = LinkedList<Algo<Int, DefaultEdge>>()
            if (useBasis) algos.addLast(Basis(graph))
            if (useCentImp) algos.addLast(CentImp(graph))
            if (useUpperBound) algos.addLast(UpperBound(graph))
            if (useInitSort) algos.addLast(InitSort(graph))
            if (useGCC) algos.addLast(GCC(graph))
            if (useILPnew) algos.addLast(ILPnew(graph))
            if (useILPold) algos.addLast(ILPold(graph))

            for (algo in algos) {
                algo.solve(size)
                printInfoPretty(System.out, algo)
                println()
            }
        }
    } else {
        // get Input Parameters
        val pathOutput = args[0]
        val pathGraph = args[1]
        val k = args[2].toInt()
        val algoType = args[3]
        val problemID = args[4]
        val timeoutMS = args[5].toLong()
        // create PrintStream for Output File
        val out = PrintStream(FileOutputStream(File(pathOutput), true))
        // create Graph from File
        val graph = importSimpleGraphFromFile(pathGraph)
        if (!GraphTests.isConnected(graph)) {
            reduceToBiggestConnectedSubGraph(graph)
        }
        // Create the correct Algorithm
        val algo = when (algoType) {
            "Basis" -> Basis(graph)
            "CentImp" -> CentImp(graph)
            "UpperBound" -> UpperBound(graph)
            "InitSort" -> InitSort(graph)
            "GCC" -> GCC(graph)
            "ILPnew" -> ILPnew(graph)
            "ILPold" -> ILPold(graph)
            else -> null
        }
        if (algo != null) {
            // run the algorithm for the given k with the given timeout
            println("Working on problem $problemID...")
            val compFuture = CompletableFuture.supplyAsync {
                algo.solve(k)
                true
            }
            val runtime = Runtime.getRuntime()
            var memory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            val timer = Timer()
            timer.scheduleAtFixedRate(timerTask {
                memory = max(memory, (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024))
            }, 0, 1000)
            compFuture.completeOnTimeout(false, timeoutMS, TimeUnit.MILLISECONDS)
            val completed = compFuture.get()
            timer.cancel()
            memory = max(memory, (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024))
            if (completed) {
                println("Problem $problemID Completed")
            } else {
                println("Problem $problemID Timeout")
            }
            printInfoCSV(problemID, out, algo, memory, !completed, algoType)
        } else {
            println("Fourth argument needs to be one of ${listOf("Basis", "CentImp", "GCC", "ILP")}")
        }
        out.close()
    }
}

val testGraphs = arrayOf(
    "\\konect\\undirected-simple-small\\contiguous-usa\\out.contiguous-usa",    // 3,3
    "\\network repository\\brain\\bn-cat-mixed-species_brain_1.edges",          // 1,1
    "\\konect\\undirected-simple-small\\arenas-jazz\\out.arenas-jazz",          // 1,1
    "\\network repository\\collaboration\\ca-netscience.mtx",                   // 3,2
    "\\network repository\\misc\\robot24c1_mat5.edges",                         // 1,1
    "\\network repository\\econ\\econ-beause.edges",                            // 1,1
    "\\network repository\\bio\\bio-diseasome.mtx",                             // 3,2
    "\\network repository\\social\\soc-wiki-Vote\\soc-wiki-Vote.mtx",           // 2,3
    "\\network repository\\collaboration\\ca-CSphd.mtx",                        // 10,9
    "\\konect\\undirected-simple-small\\arenas-email\\out.arenas-email",        // 2,2
    "\\network repository\\misc\\comsol.edges",                                 // 1,3
    "\\network repository\\brain\\bn-fly-drosophila_medulla_1.edges",           // 1,2
    "\\network repository\\misc\\heart2.edges",                                 // 1,1
    "\\network repository\\econ\\econ-orani678.edges",                          // 1,1
    "\\network repository\\infrastructure\\inf-openflights.edges",              // 2,2
    "\\network repository\\infrastructure\\inf-power.mtx",                      // 12,14
    "\\network repository\\social\\soc-advogato\\soc-advogato.txt",             // 1,1
    "\\network repository\\bio\\bio-dmela.mtx",                                 // 2,2
    "\\network repository\\collaboration\\ca-HepPh.mtx",                        // 1,1
    "\\network repository\\collaboration\\ca-AstroPh.mtx"                       // 1,1
)

fun getTestGraphPath(repositoryPath: String, index: Int) : String {
    return if (index >= 0 && index < testGraphs.size) {
        repositoryPath + testGraphs[index]
    } else {
        repositoryPath
    }
}

fun createGraph(repositoryPath: String, index: Int, out: PrintStream) : SimpleGraph<Int, DefaultEdge> {
    val graphPath = getTestGraphPath(repositoryPath, index)
    val graph = importSimpleGraphFromFile(graphPath)

    out.println("Graph: $graphPath")
    out.println("Edges: " + graph.edgeSet().size)
    out.println("Vertices: " + graph.vertexSet().size)
    val connected = GraphTests.isConnected(graph)
    out.println("Connected: $connected\n")

    if (!connected) {
        out.println("Calculating biggest connected subgraph...")
        reduceToBiggestConnectedSubGraph(graph)
        out.println("Edges: ${graph.edgeSet().size}")
        out.println("Vertices: ${graph.vertexSet().size}\n")
    }
    return graph
}

fun autoTestGraphs(graphIndices: IntArray, repositoryPath: String, maxSize: Int, outPutFilePath: String, timeout: Long, timeUnit: TimeUnit) {
    val outputFile = File(outPutFilePath)
    if (outputFile.exists()) outputFile.delete()
    outputFile.createNewFile()
    val out = PrintStream(outputFile)
    out.println("Timeout: ${prettyTime(TimeUnit.MILLISECONDS.convert(timeout, timeUnit))}\n")

    for (i in graphIndices) {
        println("Using Graph ${testGraphs[i]}")
        val algo = GCC(createGraph(repositoryPath, i, out))
        var cancelled = false
        var size = 1
        while (!cancelled && (size <= maxSize || maxSize == -1)) {
            println("Calculating size $size...")
            val compFuture = CompletableFuture.supplyAsync {
                algo.solve(size)
                printInfoPretty(out, algo)
                out.println()
                true
            }
            compFuture.completeOnTimeout(false, timeout, timeUnit)
            val completed = compFuture.get()
            if (completed) {
                size++
            } else {
                println("Timeout")
                out.println("Execution has been cancelled because it took too long\n")
                cancelled = true
            }
        }
        out.println("--------------------------------------------------------------------------------\n")
    }
    out.close()
}

fun <V,E> printInfoPretty(out: PrintStream, algo: Algo<V,E>) {
    out.println("Results with Optimized Algorithm for size ${algo.k}:")
    out.println("Time for Prep: ${prettyTime(algo.prepTime)}")
    out.println("Time for Algo: ${prettyTime(algo.algoTime)}")
    out.println("Result: (${algo.lastResult.second}, ${algo.lastResult.third}) ${algo.lastResult.first}")
    if (printAdditionalInfo) {
        out.println("\nAdditional Info:")
        out.println("Graph Info: #Vertices = ${algo.vertCount} #Edges = ${algo.edgeCount}")
        out.println("Graph Diameter: ${algo.diameter}")
        if (algo is GCC) {
            out.println("Upper Bound: (${algo.lastUpperBound.second}, ${algo.lastUpperBound.third}) ${algo.lastUpperBound.first}")
            out.println("Sufficient Vertices: (${algo.sufficientVertices.size}) ${algo.sufficientVertices}")
            out.println("Checked Subsets: ${algo.checkedSubsets}")
        }
        if (algo is Basis) {
            out.println("Checked Subsets: ${algo.checkedSubsets}")
        }
    }
}

fun <V,E> printInfoCSV(problemID: String, out: PrintStream, algo: Algo<V,E>, memory: Long, timedOut: Boolean, algString: String) {
    // ProblemID; Vertices; Edges; Diameter; k; UsedMemoryMiB; PrepTimeMS; AlgoTimeMS; TotalTimeMS; SufficientVertCount; UpperBoundCent; ResultCent; ResultCentNorm; ResultVertices; Algo
    if (!timedOut) {
        when (algo) {
            is GCC -> {
                out.println("$problemID;${algo.vertCount};${algo.edgeCount};${algo.diameter};${algo.k};$memory;${algo.prepTime};${algo.algoTime};${algo.prepTime + algo.algoTime};${algo.sufficientVertices.size};${algo.lastUpperBound.second};${algo.lastResult.second};${algo.lastResult.third};${algo.lastResult.first};$algString")
            }
            is InitSort -> {
                out.println("$problemID;${algo.vertCount};${algo.edgeCount};${algo.diameter};${algo.k};$memory;${algo.prepTime};${algo.algoTime};${algo.prepTime + algo.algoTime};null;${algo.lastUpperBound.second};${algo.lastResult.second};${algo.lastResult.third};${algo.lastResult.first};$algString")
            }
            is UpperBound -> {
                out.println("$problemID;${algo.vertCount};${algo.edgeCount};${algo.diameter};${algo.k};$memory;${algo.prepTime};${algo.algoTime};${algo.prepTime + algo.algoTime};null;${algo.lastUpperBound.second};${algo.lastResult.second};${algo.lastResult.third};${algo.lastResult.first};$algString")
            }
            else -> {
                out.println("$problemID;${algo.vertCount};${algo.edgeCount};${algo.diameter};${algo.k};$memory;${algo.prepTime};${algo.algoTime};${algo.prepTime + algo.algoTime};null;null;${algo.lastResult.second};${algo.lastResult.third};${algo.lastResult.first};$algString")
            }
        }
    } else {
        out.println("$problemID;${algo.vertCount};${algo.edgeCount};null;${algo.k};null;null;null;null;null;null;null;null;null;$algString")
    }
}