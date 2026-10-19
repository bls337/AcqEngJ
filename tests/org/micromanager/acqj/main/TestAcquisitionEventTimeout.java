package org.micromanager.acqj.main;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.micromanager.acqj.api.AcquisitionAPI;

/**
 * Tests for the AcquisitionEvent timeout.
 */
public class TestAcquisitionEventTimeout {

   private static final Double TIMEOUT_MS = 5000.0;

   @Test
   public void testTimeoutDefaultsToNull() {
      AcquisitionEvent event = new AcquisitionEvent((AcquisitionAPI) null);
      Assert.assertNull(event.getTimeoutMs());
   }

   @Test
   public void testTimeoutSetterAndJSONRoundTrip() {
      AcquisitionEvent event = new AcquisitionEvent((AcquisitionAPI) null);
      event.setTimeoutMs(TIMEOUT_MS);
      Assert.assertEquals(TIMEOUT_MS, event.getTimeoutMs());

      // Python callers set the timeout through this key
      Assert.assertEquals(TIMEOUT_MS, event.toJSON().opt("timeout"));
      AcquisitionEvent deserialized = AcquisitionEvent.fromJSON(event.toJSON(), null);
      Assert.assertEquals(TIMEOUT_MS, deserialized.getTimeoutMs());
   }

   @Test
   public void testNullClearsTimeout() {
      AcquisitionEvent event = new AcquisitionEvent((AcquisitionAPI) null);
      event.setTimeoutMs(TIMEOUT_MS);
      event.setTimeoutMs(null);
      Assert.assertNull(event.getTimeoutMs());
   }

   @Test
   public void testNullTimeoutRoundTripsThroughJSON() {
      AcquisitionEvent event = new AcquisitionEvent((AcquisitionAPI) null);
      Assert.assertFalse(event.toJSON().has("timeout"));
      Assert.assertNull(AcquisitionEvent.fromJSON(event.toJSON(), null).getTimeoutMs());
   }

   @Test
   public void testTimeoutSurvivesCopy() {
      AcquisitionEvent event = new AcquisitionEvent((AcquisitionAPI) null);
      event.setTimeoutMs(TIMEOUT_MS);
      Assert.assertEquals(TIMEOUT_MS, event.copy().getTimeoutMs());
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
      Assert.assertEquals(TIMEOUT_MS, merged.getTimeoutMs());
   }
}
