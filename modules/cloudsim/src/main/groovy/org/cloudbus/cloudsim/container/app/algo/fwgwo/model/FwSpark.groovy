package org.cloudbus.cloudsim.container.app.algo.fwgwo.model

class FwSpark extends Spark{

    FwSpark(Firework firework) {
        super(firework.position, firework)
        super.fitnessValue =firework.fwFitnessValue
    }
}
