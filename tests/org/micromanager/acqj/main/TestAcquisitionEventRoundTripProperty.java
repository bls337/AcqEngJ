package org.micromanager.acqj.main;

import java.util.Random;
import org.junit.Assert;
import org.junit.Test;
import org.micromanager.acqj.example.BlackHoleDataSink;

/**
 * Property-style tests for AcquisitionEvent JSON round-trip completeness.
 *
 * <p>Generation is hand-rolled with a fixed seed so this runs on AcqEngJ's CURRENT
 * JUnit-4 / Java-8 toolchain with zero dependency or config changes -- i.e. these bugs
 * are findable today, before any Java-17 / JUnit-6 / jqwik upgrade. Each property's
 * jqwik form is noted in its javadoc; jqwik would replace the loop with {@code @ForAll}
 * generators and add automatic shrinking to the minimal failing input.
 */
public class TestAcquisitionEventRoundTripProperty {

   private static AcquisitionEvent roundTrip(AcquisitionEvent e, Acquisition acq) {
      return AcquisitionEvent.fromJSON(e.toJSON(), acq);
   }

   /**
    * PROPERTY: for any minimumStartTime, fromJSON(toJSON(e)) preserves it exactly.
    * jqwik: {@code @Property void p(@ForAll @LongRange(min=0,max=1_000_000_000_000L) long t)}
    */
   @Test
   public void minimumStartTimeSurvivesRoundTripExactly() {
      final Random rng = new Random(20260721L);
      final int n = 2000;
      final Acquisition acq = new Acquisition(new BlackHoleDataSink());
      int violations = 0;
      long worstErrMs = 0;
      long worstInputMs = 0;
      for (int i = 0; i < n; i++) {
         final long t = (long) (rng.nextDouble() * 1_000_000_000L); // 0 .. ~11.6 days, in ms
         final AcquisitionEvent e = new AcquisitionEvent(acq);
         e.setMinimumStartTime(t);
         final AcquisitionEvent back = roundTrip(e, acq);
         final long err = Math.abs(e.getMinimumStartTimeAbsolute() - back.getMinimumStartTimeAbsolute());
         if (err != 0) {
            violations++;
            if (err > worstErrMs) {
               worstErrMs = err;
               worstInputMs = t;
            }
         }
      }
      Assert.assertEquals(
            "PROPERTY VIOLATED: minimumStartTime must survive JSON round-trip exactly. "
            + violations + "/" + n + " random values were corrupted; worst drift " + worstErrMs
            + " ms at input " + worstInputMs + " ms. Cause: AcquisitionEvent.toJSON writes "
            + "min_start_time = miniumumStartTimeMs_/1000 (integer division => truncates to whole "
            + "seconds); fromJSON multiplies back by 1000. testEventSerialization masks this with a "
            + "2000 ms assertion tolerance.",
            0, violations);
   }

   /**
    * CONTROL PROPERTY (expected to PASS): exposure survives round-trip exactly.
    * Shows the suite is not vacuously red -- it validates the correctly-handled fields too.
    */
   @Test
   public void exposureSurvivesRoundTripExactly() {
      final Random rng = new Random(1337L);
      final Acquisition acq = new Acquisition(new BlackHoleDataSink());
      for (int i = 0; i < 2000; i++) {
         final double exposureMs = rng.nextDouble() * 1000.0;
         final AcquisitionEvent e = new AcquisitionEvent(acq);
         e.setExposure(exposureMs);
         final AcquisitionEvent back = roundTrip(e, acq);
         Assert.assertEquals("exposure must survive round-trip exactly (input=" + exposureMs + ")",
               exposureMs, back.getExposure(), 0.0);
      }
   }
}
