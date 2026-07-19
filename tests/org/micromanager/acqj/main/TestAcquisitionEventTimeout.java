package org.micromanager.acqj.main;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.micromanager.acqj.api.AcquisitionAPI;

/**
 * Tests for the per-event camera timeout: setter, JSON round trip, copy, and
 * propagation into merged (hardware-sequenced) events.
 *
 * These tests deliberately construct events without an Acquisition so they can
 * run without the native MMCoreJ library (unlike TestAcquisitionEvent, which is
 * excluded from CI for that reason).
 */
public class TestAcquisitionEventTimeout {

   private static final double TIMEOUT_MS = 5000.0;
   private static final double DELTA = 0.0000001;

   @Test
   public void testTimeoutDefaultsToNull() {
      AcquisitionEvent event = new AcquisitionEvent((AcquisitionAPI) null);
      Assert.assertNull(event.getTimeoutMs());
   }

   @Test
   public void testTimeoutSetterAndJSONRoundTrip() {
      AcquisitionEvent event = new AcquisitionEvent((AcquisitionAPI) null);
      event.setTimeIndex(0);
      event.setTimeoutMs(TIMEOUT_MS);
      Assert.assertEquals(TIMEOUT_MS, event.getTimeoutMs(), DELTA);

      AcquisitionEvent deserialized = AcquisitionEvent.fromJSON(event.toJSON(), null);
      Assert.assertNotNull(deserialized.getTimeoutMs());
      Assert.assertEquals(TIMEOUT_MS, deserialized.getTimeoutMs(), DELTA);
   }

   @Test
   public void testTimeoutSurvivesCopy() {
      AcquisitionEvent event = new AcquisitionEvent((AcquisitionAPI) null);
      event.setTimeoutMs(TIMEOUT_MS);
      Assert.assertEquals(TIMEOUT_MS, event.copy().getTimeoutMs(), DELTA);
   }

   @Test
   public void testTimeoutCarriedIntoSequenceEvent() {
      List<AcquisitionEvent> sequence = new ArrayList<AcquisitionEvent>();
      for (int z = 0; z < 3; z++) {
         AcquisitionEvent event = new AcquisitionEvent((AcquisitionAPI) null);
         event.setZ(z, z * 0.5);
         event.setTimeoutMs(TIMEOUT_MS);
         sequence.add(event);
      }
      AcquisitionEvent merged = new AcquisitionEvent(sequence);
      Assert.assertNotNull(merged.getTimeoutMs());
      Assert.assertEquals(TIMEOUT_MS, merged.getTimeoutMs(), DELTA);
   }
}
