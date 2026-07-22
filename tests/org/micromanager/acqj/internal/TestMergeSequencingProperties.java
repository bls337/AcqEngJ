package org.micromanager.acqj.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import org.junit.Assert;
import org.micromanager.acqj.api.AcquisitionAPI;
import org.micromanager.acqj.example.BlackHoleDataSink;
import org.micromanager.acqj.main.Acquisition;
import org.micromanager.acqj.main.AcquisitionEvent;

/**
 * Property-based tests on the hardware-sequencing MERGE surface -- the "bug magnet"
 * the review flagged (AEJ-08/09/10/21/32). Uses the native-free EngineTestHarness.merge()
 * (the real isSequencable + merge constructor), so it runs in CI with no MMCoreJ native lib.
 *
 * <p>Goal: find merge bugs the code review did NOT already enumerate, by generating
 * combinations across the sequenceable dimensions and asserting merge invariants.
 */
public class TestMergeSequencingProperties {

   // One shared harness with every dimension sequenceable; merge() is stateless w.r.t.
   // the engine's lastEvent_, so reuse across tries is safe and avoids per-try allocation.
   private static final RecordingCMMCore CORE;
   private static final Acquisition ACQ;
   private static final EngineTestHarness HARNESS;

   static {
      try {
         CORE = new RecordingCMMCore("Z", "XY", "", "Cam")
               .withStageSequenceable(true)
               .withXYStageSequenceable(true)
               .withExposureSequenceable(true)
               .withSequenceMaxLength(1000);
         ACQ = new Acquisition(new BlackHoleDataSink(), false);
         HARNESS = new EngineTestHarness(CORE, ACQ);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private static AcquisitionEvent ev() {
      return new AcquisitionEvent((AcquisitionAPI) ACQ);
   }

   /** Control (expected GREEN): a pure z ramp merges and is recognized as z-sequenced. */
   @Property
   void zVariationIsZSequenced(@ForAll @IntRange(min = 2, max = 8) int n) throws Exception {
      List<AcquisitionEvent> events = new ArrayList<>();
      for (int i = 0; i < n; i++) {
         AcquisitionEvent e = ev();
         e.setZ(i, i * 0.5);
         events.add(e);
      }
      List<AcquisitionEvent> merged = HARNESS.merge(events.iterator());
      Assume.that(merged.size() == 1 && merged.get(0).getSequence() != null);
      Assert.assertTrue("z ramp must be z-sequenced", merged.get(0).isZSequenced());
   }

   /**
    * Targeted probe for AEJ-10: when exactly ONE of X/Y varies across the sequence, the
    * merged event must still be XY-sequenced (the stage needs to move each frame). The
    * merge ctor computes {@code xySequenced_ = xVaries && yVaries}, so single-axis
    * variation is expected to (wrongly) report false.
    */
   @Property
   void singleAxisXyVariationIsSequenced(
         @ForAll @IntRange(min = 2, max = 6) int n,
         @ForAll boolean varyX) throws Exception {
      List<AcquisitionEvent> events = new ArrayList<>();
      for (int i = 0; i < n; i++) {
         AcquisitionEvent e = ev();
         e.setX(varyX ? i * 10.0 : 5.0);
         e.setY(varyX ? 5.0 : i * 10.0);
         events.add(e);
      }
      List<AcquisitionEvent> merged = HARNESS.merge(events.iterator());
      Assume.that(merged.size() == 1 && merged.get(0).getSequence() != null);
      Assert.assertTrue(
            "single-axis XY variation must be hardware-sequenced (n=" + n + ", varyX=" + varyX
            + ") -- else every frame is exposed at the first XY position",
            merged.get(0).isXYSequenced());
   }

   /**
    * Broad fuzz: whatever we throw at the merge decision + constructor, it must not throw
    * (a crash on a valid event stream is a bug -- AEJ-15 class). Randomizes which
    * dimensions are set per event.
    */
   @Property(tries = 2000)
   void mergeNeverThrowsOnValidEvents(
         @ForAll @IntRange(min = 1, max = 8) int n,
         @ForAll long seed) throws Exception {
      Random r = new Random(seed);
      List<AcquisitionEvent> events = new ArrayList<>();
      for (int i = 0; i < n; i++) {
         AcquisitionEvent e = ev();
         if (r.nextBoolean()) {
            e.setX(r.nextInt(3) * 10.0);
         }
         if (r.nextBoolean()) {
            e.setY(r.nextInt(3) * 10.0);
         }
         if (r.nextBoolean()) {
            e.setZ(i, r.nextInt(3) * 0.5);
         }
         if (r.nextBoolean()) {
            e.setExposure(r.nextInt(3) * 10.0 + 1.0);
         }
         events.add(e);
      }
      // The property is simply: this call completes without throwing.
      HARNESS.merge(events.iterator());
   }
}
