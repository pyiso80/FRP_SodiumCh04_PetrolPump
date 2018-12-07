package chapter4.section6;

import pump.*;
import chapter4.section4.LifeCycle;
import nz.sodium.*;
import java.util.Optional;

public class AccumulatePulsesPump implements Pump {
   public Outputs create(Inputs inputs) {
      LifeCycle lc = new LifeCycle(inputs.sNozzle1,
                                    inputs.sNozzle2,
                                    inputs.sNozzle3);
      Cell<Double> litersDelivered =
                     accumulate(lc.sStart.map(u -> Unit.UNIT),//when flow start, clr total
                                 inputs.sFuelPulses,
                                 inputs.calibration);
      return new Outputs()
         .setDelivery(lc.fillActive
            .map(f -> f.equals(Optional.of(Fuel.ONE))   ? Delivery.FAST1 :
                        f.equals(Optional.of(Fuel.TWO))   ? Delivery.FAST2 :
                        f.equals(Optional.of(Fuel.THREE)) ? Delivery.FAST3 :
                        Delivery.OFF))
         .setSaleQuantityLCD(litersDelivered
            .map(q -> Formatters.formatSaleQuantity(q)));
   }

   //Convert fuel flow rate, indicated number of pulses, to liter
   public static Cell<Double> accumulate(Stream<Unit> sClearAccumulator,
                                          Stream<Integer> sPulses,
                                          Cell<Double> calibration) {
      CellLoop<Integer> total = new CellLoop<>();
      //see (1)
      total.loop(sClearAccumulator
                  .map(u -> 0)
                  .orElse(sPulses.snapshot(total, (pulses_, total_)
                                                      -> pulses_ + total_))
                  .hold(0));
      return total.lift(calibration, (total_, calibration_)
                                          -> total_ * calibration_);
   }
}

/*    (1)
      Stream<Integer> totalPulses = sClearAccumulator
                                 .map(u -> 0)
                                 .orElse(sPulses.snapshot(total, (pulses_, total_)
                                                                     -> pulses_ + total_));
      total.loop(totalPulses.hold(0));
 */

