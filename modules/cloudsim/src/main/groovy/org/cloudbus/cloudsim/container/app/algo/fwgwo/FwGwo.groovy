package org.cloudbus.cloudsim.container.app.algo.fwgwo


import org.cloudbus.cloudsim.container.app.algo.fwgwo.model.GreyWolf
import org.cloudbus.cloudsim.container.app.model.DatacenterMetrics
import org.cloudbus.cloudsim.container.app.model.Task
import org.cloudbus.cloudsim.container.core.ContainerHost

class FwGwo {
    static class Config {
        static final int PACK_SIZE = 4
        static final int MAX_SPARKS_PER_FW = PACK_SIZE + 2
        static final int NO_OF_PACKS = 1
        static final int MAX_ITERATIONS = 10
    }
    Fwa fwa
    Gwo gwo


    FwGwo(int numberOfPacks, List<ContainerHost> allHosts) {
        fwa = new Fwa(numberOfFireworks: numberOfPacks, allHosts: allHosts, maxSparkCountPerFw: Config.MAX_SPARKS_PER_FW)
        gwo = new Gwo(numberOfPacks: 5, wolvesPerPack: Config.PACK_SIZE)
    }


    ContainerHost selectHostForContainerAllocation(Task taskToSchedule) {
        fwa.initFireworks()
        fwa.calculateAmplitudes(taskToSchedule)
        fwa.generateSparks()
        fwa.findBestSparks(taskToSchedule, Config.PACK_SIZE)
        gwo.initPacks(fwa.fireworks)
        int iteration = 0
        int maxIterations = Config.MAX_ITERATIONS
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
//        File outputLog = new File("modules/cloudsim/build/FWGWO-iterations${taskToSchedule.toString()}.log")
//        outputLog.text = sb.toString()

        GreyWolf best = gwo.getBestWolfFromALlPacks()
        if (best != null) {
            DatacenterMetrics.get().addObjectiveFunctionValueForTask(best.objectives, taskToSchedule)
            return best.currentHostCandidate
        }
        fwa.getAllHosts()
//       int randomIdx= MathUtil.randomInt(fwa.getAllHosts().size()-1)
//
//        org.cloudbus.cloudsim.Log.printLine(getClass().getName(), ": Selecting best by gwo failed. assigning random")
        return null;
    }

}
