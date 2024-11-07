package cc.blynk.server.core.plugins;

import cc.blynk.server.core.model.enums.PinType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PlaatoStructureTest {
    private PlaatoStructure plaato;

    @Before
    public void init() throws Exception {
        this.plaato = new PlaatoStructure();
    }

    @Test
    public void testBubbles() throws Exception {
        plaato.setHardwarePinData((short) 100, "5");
        plaato.setHardwarePinData((short) 100, "5");
        plaato.setHardwarePinData((short) 100, "7");
        assertEquals("2", plaato.pullPinData((short) 110));
        plaato.setHardwarePinData((short) 100, "4");
        plaato.setHardwarePinData((short) 100, "4");
        plaato.setHardwarePinData((short) 100, "5");
        assertEquals("7", plaato.pullPinData((short) 110));
    }

    @Test
    public void testBubblesReset() throws Exception {
        plaato.setHardwarePinData((short) 100, "50");
        plaato.setHardwarePinData((short) 100, "50");
        plaato.setHardwarePinData((short) 100, "3");
        assertEquals("3", plaato.pullPinData((byte) 110));
    }

    @Test
    public void testTemperatureCelsius() throws Exception {
        plaato.setHardwarePinData((short) 100, "50");
        plaato.setHardwarePinData((short) 101, "36");
        plaato.setAppPinData((short) 114, 2);
        assertEquals("31.73379898071289", plaato.pullPinData((byte) 103));
        assertEquals("°C", plaato.pullPinData((short) 108));
    }

    @Test
    public void testTemperatureFahrenheit() throws Exception {
        plaato.setHardwarePinData((short) 100, "50");
        plaato.setHardwarePinData((short) 101, "36");
        plaato.setAppPinData((short) 114, 1);
        assertEquals("89.1208381652832", plaato.pullPinData((short) 103));
        assertEquals("°F", plaato.pullPinData((short) 108));
    }

    @Test
    public void testVolumeLiters() throws Exception {
        plaato.setAppPinData((short) 115, 2);
        plaato.setAppPinData((short) 111, 20);
        assertEquals("2.0", plaato.pullPinData((byte) 104));
        assertEquals("L", plaato.pullPinData((byte) 109));
    }

    @Test
    public void testVolumeGallons() throws Exception {
        plaato.setAppPinData((short) 115, 1);
        plaato.setAppPinData((short) 111, 150);
        assertEquals("15.000000251220705", plaato.pullPinData((byte) 104));
        assertEquals("gal", plaato.pullPinData((byte) 109));
    }

    @Test
    public void testReset() throws Exception {
        plaato.setHardwarePinData((short) 100, "5");
        plaato.setHardwarePinData((short) 100, "500");
        plaato.setAppPinData((short) 116, 1);
        assertEquals("0", plaato.pullPinData((byte) 110));
    }

    @Test
    public void testOriginalGravity() throws Exception {
        plaato.setHardwarePinData((short) 100, "700");
        plaato.setAppPinData((short) 115, 2);
        plaato.setAppPinData((short) 111, 20);
        plaato.setAppPinData((short) 112, 30);
        assertEquals(1.029F, Float.parseFloat(plaato.pullPinData((byte) 105)), 0.001);
    }

    @Test
    public void testBigSpecificGravityResetsBubbles() throws Exception {
        plaato.setHardwarePinData((short) 100, "0");
        plaato.setHardwarePinData((short) 100, "15000");
        plaato.setAppPinData((short) 115, 2);
        plaato.setAppPinData((short) 111, 20);
        plaato.setAppPinData((short) 112, 30);
        assertEquals(1.029F, Float.parseFloat(plaato.pullPinData((short) 105)), 0.001);
        assertEquals(1.020176513671875F, Float.parseFloat(plaato.pullPinData((short) 106)), 0.001);

        plaato.setAppPinData((short) 113, 40);

        assertEquals("0", plaato.pullPinData((short) 110));
        assertEquals(1.029F, Float.parseFloat(plaato.pullPinData((short) 105)), 0.001);
        assertEquals(1.029F, Float.parseFloat(plaato.pullPinData((short) 106)), 0.001);
    }

    @Test
    public void testSpecificGravity() throws Exception {
        plaato.setHardwarePinData((short) 100, "0");
        plaato.setHardwarePinData((short) 100, "15000");
        plaato.setAppPinData((short) 115, 2);
        plaato.setAppPinData((short) 111, 20);
        plaato.setAppPinData((short) 112, 15);
        assertEquals(1.014F, Float.parseFloat(plaato.pullPinData((short) 105)), 0.001);
        assertEquals(1.005176513671875F, Float.parseFloat(plaato.pullPinData((short) 106)), 0.001);
        assertEquals("1.1167328", plaato.pullPinData((short) 107));

        plaato.setAppPinData((short) 113, 12);

        assertEquals("5100", plaato.pullPinData((short) 110));
        assertEquals(1.014F, Float.parseFloat(plaato.pullPinData((short) 105)), 0.001);
        assertEquals(1.011F, Float.parseFloat(plaato.pullPinData((short) 106)), 0.001);
        assertEquals("0.38189226", plaato.pullPinData((short) 107));
    }

    @Test
    public void testSpecificGravityWithLearning() throws Exception {
        plaato.setHardwarePinData((short) 100, "0");
        plaato.setHardwarePinData((short) 100, "15000");
        plaato.setAppPinData((short) 115, 2);
        plaato.setAppPinData((short) 111, 20);
        plaato.setAppPinData((short) 112, 15);
        assertEquals(1.014F, Float.parseFloat(plaato.pullPinData((short) 105)), 0.001);
        assertEquals(1.005176513671875F, Float.parseFloat(plaato.pullPinData((short) 106)), 0.001);

        plaato.setAppPinData((short) 117, 1);
        plaato.setAppPinData((short) 113, 4);

        assertEquals("15000", plaato.pullPinData((short) 110));
        assertEquals("681815", plaato.pullPinData((short) 118));
        assertEquals(1.014F, Float.parseFloat(plaato.pullPinData((short) 105)), 0.001);
        assertEquals(1.003F, Float.parseFloat(plaato.pullPinData((short) 106)), 0.001);

        plaato.setAppPinData((short) 117, 0);
        plaato.setAppPinData((short) 113, 12);

        assertEquals("4090", plaato.pullPinData((short) 110));
        assertEquals("681815", plaato.pullPinData((short) 118));
        assertEquals(1.014F, Float.parseFloat(plaato.pullPinData((short) 105)), 0.001);
        assertEquals(1.011F, Float.parseFloat(plaato.pullPinData((short) 106)), 0.001);

        plaato.setAppPinData((short) 117, 1);
        plaato.setAppPinData((short) 113, 4);

        assertEquals("14999", plaato.pullPinData((short) 110));
        assertEquals("681815", plaato.pullPinData((short) 118));
        assertEquals(1.014F, Float.parseFloat(plaato.pullPinData((short) 105)), 0.001);
        assertEquals(1.003F, Float.parseFloat(plaato.pullPinData((short) 106)), 0.001);
    }

    /**
     * According to wikipedia for common strength beer  OG could be 1.050 and FG could be 1.010
     */
    @Test
    public void testRealBeer() throws Exception {
        plaato.setHardwarePinData((short) 100, "0");
        plaato.setHardwarePinData((short) 100, "81000");
        plaato.setAppPinData((short) 115, 2);
        plaato.setAppPinData((short) 111, 30);
        plaato.setAppPinData((short) 112, 51);
        assertEquals(1.050F, Float.parseFloat(plaato.pullPinData((short) 105)), 0.001);
        assertEquals(1.0182352294921875, Float.parseFloat(plaato.pullPinData((short) 106)), 0.001);
        assertEquals("4.2746983", plaato.pullPinData((short) 107));
    }

    @Test
    public void testPullIncorrectPin() {
        assertNull(plaato.pullPinData((short) 99));
    }

    @Test
    public void testBPM() throws Exception {
        plaato.setHardwarePinData((short) 100, "5");
        assertEquals("0", plaato.pullPinData((short) 102));
        Thread.sleep(2000);
        plaato.setHardwarePinData((short) 100, "15");
        assertEquals("300", plaato.pullPinData((short) 102));
    }

    @Test
    public void testIsReservedByApp() throws Exception {
        assertTrue(PlaatoStructure.isReservedByApp(PinType.VIRTUAL, (short) 107));
        assertFalse(PlaatoStructure.isReservedByApp(PinType.VIRTUAL, (short) 120));
        assertFalse(PlaatoStructure.isReservedByApp(PinType.VIRTUAL, (short) 101));
        assertFalse(PlaatoStructure.isReservedByApp(PinType.ANALOG, (short) 107));
        assertFalse(PlaatoStructure.isReservedByApp(PinType.DIGITAL, (short) 107));
    }

    @Test
    public void testIsReservedByHardware() throws Exception {
        assertTrue(PlaatoStructure.isReservedByHardware(PinType.VIRTUAL, (short) 101));
        assertFalse(PlaatoStructure.isReservedByHardware(PinType.VIRTUAL, (short) 103));
        assertFalse(PlaatoStructure.isReservedByHardware(PinType.VIRTUAL, (short) 99));
        assertFalse(PlaatoStructure.isReservedByHardware(PinType.ANALOG, (short) 100));
        assertFalse(PlaatoStructure.isReservedByHardware(PinType.DIGITAL, (short) 100));
    }
}