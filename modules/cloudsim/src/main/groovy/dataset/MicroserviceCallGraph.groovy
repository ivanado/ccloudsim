package dataset

class MicroserviceCallGraph {
    String traceId
    String upstreamMs
    String downstreamMs

    @Override
    boolean equals(Object obj) {
        MicroserviceCallGraph other = (MicroserviceCallGraph) obj
        return this.traceId == other.traceId && this.upstreamMs == other.upstreamMs && this.downstreamMs == other.downstreamMs
    }
}
