package com.logistimo.events.generators;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by smriti on 23/11/17.
 */
public class AssetEventGeneratorTest {

  AssetEventGenerator assetEventGenerator = new AssetEventGenerator(1l, "domain");

  @Test
  public void testIsEventDurationValid() {
    Boolean isEventValid = assetEventGenerator.isEventDurationValid(5, 1508742992000l);
    assertEquals(isEventValid, true);
  }

  @Test
  public void testIsEventDurationValidForCurrentTime() {
    Boolean isEventValid = assetEventGenerator.isEventDurationValid(1, System.currentTimeMillis());
    assertEquals(isEventValid, false);
  }

}
