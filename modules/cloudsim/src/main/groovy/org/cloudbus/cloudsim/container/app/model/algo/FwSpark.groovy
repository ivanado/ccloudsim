package org.cloudbus.cloudsim.container.app.model.algo

class FwSpark extends Spark{

    FwSpark(Firework firework) {
        super(firework.position, firework)
        super.fitnessValue =firework.fwFitnessValue
    }
}
