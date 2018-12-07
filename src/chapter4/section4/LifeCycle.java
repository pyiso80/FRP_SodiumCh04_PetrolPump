package chapter4.section4;

import pump.*;
import nz.sodium.*;
import java.util.Optional;

public class LifeCycle {
   public final Stream<Fuel> sStart;
   public final Cell<Optional<Fuel>> fillActive;
   public final Stream<End> sEnd;

   public enum End { END }

   //generate an event to indicate a nozzle of some fuel type has been lifted
   //Note: it doesn't mean fuel starts flowing as soon as a nozzle is lifted
   private static Stream<Fuel> whenLifted (Stream<UpDown> sNozzle, Fuel nozzleFuel) {
      return sNozzle.filter(u -> u == UpDown.UP)
                   .map(u -> nozzleFuel);
   }

   //generate an event to indicate "stop the fuel flow" from a nozzle must stop
   //when that nozzle is down
   private static Stream<End> whenSetDown(
   Stream<UpDown> sNozzle, Fuel nozzleFuel, Cell<Optional<Fuel>> fillActive) {
      return Stream.filterOptional( sNozzle.snapshot( fillActive,
                                   (u, fillActive_)
                                       -> u == UpDown.DOWN
                                          && fillActive_.equals(Optional.of(nozzleFuel))
                                             ? Optional.of(End.END) : Optional.empty()));
   }

   public LifeCycle(//..
   Stream<UpDown> sNozzle1, Stream<UpDown> sNozzle2, Stream<UpDown> sNozzle3) {
      //indication of any one of the three nozzles has been lifted
      Stream<Fuel> sLiftNozzle = whenLifted(sNozzle1, Fuel.ONE)
                                 .orElse(whenLifted(sNozzle2, Fuel.TWO)
                                 .orElse(whenLifted(sNozzle3, Fuel.THREE)));
      //hold currently flowing fuel type, or nothing if not anything flowing
      CellLoop<Optional<Fuel>> fillActive = new CellLoop<>();
      this.fillActive = fillActive;

      //Indicate the actual flow of fuel should happens when the nozzle lifted
      //and there is no other nozzle currently being lifted. Also change the status
      //of the system to active (i.e fuel is flowing)
      this.sStart = Stream.filterOptional( sLiftNozzle.snapshot(fillActive,
                                          (newFuel, fillActive_)
                                             -> fillActive_.isPresent()
                                                 ? Optional.empty() : Optional.of(newFuel)));
      //When a nozzle from which fuel is flowing is held back down, fuel flow must stop.
      //Note: while the fuel has already been flowing from a nozzle, lifting up/holding
      //down others should not end current flow of fuel
      this.sEnd = whenSetDown(sNozzle1, Fuel.ONE, fillActive)
                     .orElse(whenSetDown(sNozzle2, Fuel.TWO, fillActive)
                     .orElse(whenSetDown(sNozzle3, Fuel.THREE, fillActive)));
      fillActive.loop(
         sEnd.map(e -> Optional.<Fuel>empty())
             .orElse(sStart.map(f -> Optional.of(f)))
             .hold(Optional.empty())
      );
   }
}

