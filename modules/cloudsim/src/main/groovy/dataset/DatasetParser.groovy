package dataset

import org.cloudbus.cloudsim.container.app.model.Microservice

class DatasetParser {


    public static final String EMPTY = "(?)"

    static void main(String[] args) {

        Set<MicroserviceCallGraph> msCallGraphs = new HashSet<>()
        Map<String, Set<MicroserviceCallGraph>> traces = [:]
        String path = "/Users/ivana/cluster-data/clusterdata/cluster-trace-microservices-v2021/data/MSCallGraph/"
        File dirc = new File(path)
        List csvFiles = dirc.listFiles().findAll {
            it.name.endsWith(".csv")
        }
        for (File csv : csvFiles) {
            println("Parsing file ${csv.path}")
            String line;
//            csv.withReader { reader ->
//                reader.readLine() //skip header
//                while ((line = reader.readLine()) != null) {
//                    String[] props = line.split(",")
//
//                    def traceId = props[1]
//                    def uMs = props[4]
//                    def dMs = props[6]
//                    if ((uMs != EMPTY || dMs != EMPTY) && uMs != dMs) {
//                        def msCallGraph = new MicroserviceCallGraph(traceId: traceId, upstreamMs: props[4], downstreamMs: props[6])
//                        updateTraces(traces, msCallGraph)
//
//                    }
//
//
//                }
//            }

            File x = new File("modules/cloudsim/build/file")
            x.withReader { reader ->
                boolean hasNext = true
                while (hasNext) {
                    String um = reader.readLine().substring(4)
                    String dm = reader.readLine().substring(4)
                    updateTraces(traces, new MicroserviceCallGraph(traceId: "trace1", upstreamMs: um, downstreamMs: dm))
                    hasNext = reader.readLine() != null
                }
            }
            String traceId = traces.keySet().first()
            println("Trace ${traceId}")
            List<MicroserviceCallGraph> callGraphs = traces[traceId]
            MicroserviceCallGraph entry = callGraphs.find { it.upstreamMs == EMPTY }
            List<Microservice> microservices = []
            parseMicroservices(entry, callGraphs, null, microservices)

//            traces.eachWithIndex { Map.Entry<String, List<MicroserviceCallGraph>> entry, int i ->
            traceId = traces.keySet().first()
            println("Trace ${traceId}")
            List l = traces[traceId]
            l.forEach { MicroserviceCallGraph mscg ->
                {
                    println("UM: ${mscg.upstreamMs}")
                    println("DM: ${mscg.downstreamMs}\n")
                }
            }

        }

    }

    static void parseMicroservices(current, List<MicroserviceCallGraph> callGraphs, Microservice parent, List<Microservice> microservices) {

        Microservice ms = new Microservice(15)
        ms.setProvider(parent)
        microservices.add(ms)
        if (current.downstreamMs != EMPTY) {
            List downstreamMss=callGraphs.findAll { it.upstreamMs == current.downstreamMs }
            for(MicroserviceCallGraph d: downstreamMss){
                parseMicroservices(d, callGraphs, ms, microservices)
            }
        }


    }

    private static def updateTraces(Map<String, Set<MicroserviceCallGraph>> traces, MicroserviceCallGraph microserviceCallGraph) {
        traces[microserviceCallGraph.traceId] = traces[microserviceCallGraph.traceId] ?: []
        if (traces[microserviceCallGraph.traceId].find { it.upstreamMs == microserviceCallGraph.upstreamMs && it.downstreamMs == microserviceCallGraph.downstreamMs } == null) {
            traces[microserviceCallGraph.traceId].add(microserviceCallGraph)
        }

    }
}
