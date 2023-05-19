package org.cloudbus.cloudsim.container.app.model.algo

import org.cloudbus.cloudsim.container.app.model.DatacenterMetrics
import org.cloudbus.cloudsim.container.app.model.Task
import org.cloudbus.cloudsim.container.core.ContainerHost

class FwGwo {

    def PACK_SIZE = 4
    def MAX_SPARKS_PER_FW = PACK_SIZE + 2
    Fwa fwa
    Gwo gwo


    FwGwo(int numberOfPacks, List<ContainerHost> allHosts) {
        fwa = new Fwa(numberOfFireworks: numberOfPacks, allHosts: allHosts, maxPerFwSparkCount: MAX_SPARKS_PER_FW)
        gwo = new Gwo(numberOfPacks: 1, wolvesPerPack: PACK_SIZE)
    }


    ContainerHost selectHostForContainerAllocation(Task taskToSchedule) {
        fwa.initFireworks()
        fwa.calculateAmplitudes(taskToSchedule)
        fwa.generateSparks()
        fwa.findBestSparks(taskToSchedule, PACK_SIZE)
        gwo.initPacks(fwa.fireworks)
        int iteration = 0
        int maxIterations = 10
        StringBuilder sb = new StringBuilder() //output objective function value per iteration
        sb.append("iteration,thresholdDistance,clusterBalance,systemFailureRate,totalNetworkDistance,objectiveFunction\n")
        while (iteration < maxIterations) {
            double a = 2 - (double) iteration / maxIterations
//            Log.printLine("Iteration #$iteration")
            gwo.updateWolvesPositions(a)
            gwo.calculateFitness(taskToSchedule)
            gwo.rankWolves()
            sb.append(gwo.getIterationLogMessage(iteration))
            iteration++
        }
        File outputLog = new File("modules/cloudsim/build/FWGWO-iterations.log")
        outputLog.text = sb.toString()

        GreyWolf best = gwo.getBestWolfFromALlPacks()
        DatacenterMetrics.get().setBestObjectiveFunctionValues(best.objectives)
        return best.currentHostCandidate
    }

}
