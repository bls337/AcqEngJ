package org.micromanager.acqj.main;

import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.StringLength;
import org.junit.Assert;
import org.micromanager.acqj.example.BlackHoleDataSink;

/**
 * jqwik property-based round-trip tests for AcquisitionEvent (PoC 2026-07-21).
 *
 * <p>Runs on the JUnit Platform via surefire; no Java 17 required. Unlike a hand-rolled
 * loop, jqwik GENERATES inputs and SHRINKS a failure to the minimal counterexample, so
 * a red property reports e.g. {@code Shrunk Sample: [1]} instead of a random large value.
 *
 * <p>Each field is its own property so an independent drop in any one is isolated rather
 * than masked by the first failure.
 */
public class TestAcquisitionEventProperties {

   // One shared Acquisition: constructing thousands would leak notification threads.
   private static final Acquisition ACQ = new Acquisition(new BlackHoleDataSink());

   private static AcquisitionEvent roundTrip(AcquisitionEvent e) {
      return AcquisitionEvent.fromJSON(e.toJSON(), ACQ);
   }

   /** Known bug: expected RED, jqwik should shrink to the minimal failing t (= 1). */
   @Property
   void minimumStartTimeSurvivesRoundTrip(
         @ForAll @LongRange(min = 0, max = 1_000_000_000_000L) long t) {
      AcquisitionEvent e = new AcquisitionEvent(ACQ);
      e.setMinimumStartTime(t);
      Assert.assertEquals("min_start_time must survive round-trip",
            (long) e.getMinimumStartTimeAbsolute(),
            (long) roundTrip(e).getMinimumStartTimeAbsolute());
   }

   /** Control: expected GREEN (exposure is written/read as a JSON double). */
   @Property
   void exposureSurvivesRoundTrip(
         @ForAll @DoubleRange(min = 0.0, max = 100_000.0) double exposureMs) {
      AcquisitionEvent e = new AcquisitionEvent(ACQ);
      e.setExposure(exposureMs);
      Assert.assertEquals(exposureMs, roundTrip(e).getExposure(), 0.0);
   }

   /** Hunt: does the z index + position survive exactly? */
   @Property
   void zSurvivesRoundTrip(
         @ForAll @IntRange(min = 0, max = 100_000) int zIndex,
         @ForAll @DoubleRange(min = -100_000.0, max = 100_000.0) double zUm) {
      AcquisitionEvent e = new AcquisitionEvent(ACQ);
      e.setZ(zIndex, zUm);
      AcquisitionEvent back = roundTrip(e);
      Assert.assertEquals("zIndex", zIndex, (int) back.getZIndex());
      Assert.assertEquals("zPosition", zUm, back.getZPosition(), 1e-9);
   }

   /** Hunt: does the time index survive exactly? */
   @Property
   void timeIndexSurvivesRoundTrip(@ForAll @IntRange(min = 0, max = 1_000_000) int tIndex) {
      AcquisitionEvent e = new AcquisitionEvent(ACQ);
      e.setTimeIndex(tIndex);
      Assert.assertEquals(tIndex, (int) roundTrip(e).getTIndex());
   }

   /**
    * Hunt (higher severity): two DIFFERENT per-event properties on the SAME device must
    * both be retained. Review item AEJ-08 predicts the second is silently dropped on 0.39.4
    * (ThreeTuple.compareTo collapses same-device tuples). No serialization needed -- the
    * loss happens on the set. Tiger rigs concentrate many properties on one device label.
    */
   @Property
   void twoPropertiesOnSameDeviceBothRetained(
         @ForAll @AlphaChars @StringLength(min = 1, max = 4) String propA,
         @ForAll @AlphaChars @StringLength(min = 1, max = 4) String propB) {
      Assume.that(!propA.equals(propB));
      AcquisitionEvent e = new AcquisitionEvent(ACQ);
      e.setProperty("DeviceX", propA, "1");
      e.setProperty("DeviceX", propB, "2");
      Assert.assertEquals(
            "both distinct properties on one device must survive (propA=" + propA
            + ", propB=" + propB + ")",
            2, e.getAdditonalProperties().size());
   }
}
