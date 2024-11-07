package cc.blynk.integration.tcp.plaato;

import cc.blynk.integration.BaseTest;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.FrequencyWidget;
import cc.blynk.server.core.model.widgets.MobileSyncWidget;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.MobileAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.ScheduledExecutorService;

import static cc.blynk.integration.TestUtil.b;
import static cc.blynk.integration.TestUtil.createDevice;
import static cc.blynk.integration.TestUtil.deviceOffline;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.integration.TestUtil.sleep;
import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Response.OK;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PlaatoPluginIntegrationTest extends BaseTest {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareAndHttpAPIServer(holder).start();
        this.appServer = new MobileAndHttpsServer(holder).start();
        this.clientPair = initAppAndHardPair("user_profile_json_plaato.txt");
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }

    @Test
    public void testBubbleSetOnePinRead() throws Exception {
        ScheduledExecutorService ses = startReadingWidgetsWorker();
        Thread.sleep(100);
        clientPair.hardwareClient.send("hardware vw 100 0");
        clientPair.hardwareClient.send("hardware vw 100 5");
        //clientPair.hardwareClient.send("hardware vw 20 5");
        clientPair.appClient.send("createWidget 1\0{\"id\":155, \"frequency\":400, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"GAUGE\", \"pinType\":\"VIRTUAL\", \"pin\":110}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 110 5"))));
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), any());
        clientPair.appClient.send("deleteWidget 1\0 155");
    }

    @Test
    public void testInitialValuesAreCorrect() throws Exception {
        ScheduledExecutorService ses = startReadingWidgetsWorker();
        Thread.sleep(100);
        clientPair.appClient.send("createWidget 1\0{\"id\":155, \"frequency\":400, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"GAUGE\", \"pinType\":\"VIRTUAL\", \"pin\":104}");
        clientPair.appClient.send("createWidget 1\0{\"id\":156, \"frequency\":400, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"GAUGE\", \"pinType\":\"VIRTUAL\", \"pin\":105}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));
        verify(clientPair.appClient.responseMock, never()).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 104 0.0"))));
        verify(clientPair.appClient.responseMock, never()).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 105 -1000.0"))));
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), any());
        clientPair.appClient.send("deleteWidget 1\0 155");
    }


    @Test
    @Ignore("LCD is not supported right now")
    public void testTemperatureSetMultipinRead() throws Exception {
        ScheduledExecutorService ses = startReadingWidgetsWorker();
        Thread.sleep(100);
        clientPair.hardwareClient.send("hardware vw 101 36");
        clientPair.appClient.send("hardware 1 vw 114 2");
        //clientPair.hardwareClient.send("hardware vw 20 5");
        clientPair.appClient.send("createWidget 1\0{\"type\":\"LCD\",\"id\":1923810267,\"x\":0,\"y\":6,\"color\":600084223,\"width\":8,\"height\":2,\"tabId\":0,\"" +
                    "pins\":[" +
                    "{\"pin\":103,\"pinType\":\"VIRTUAL\",\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":1023, \"value\":\"10\"}]," +
                    "\"advancedMode\":false,\"textLight\":false,\"textLightOn\":false,\"frequency\":1000}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));
        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 103 31.73379898071289"))));
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), any());
        clientPair.appClient.send("deleteWidget 1\0 155");
    }

    /**
     * It is very important that alcohol amount is correct
     */
    @Test
    public void testAlcoholIsCorrect() throws Exception {
        ScheduledExecutorService ses = startReadingWidgetsWorker();
        Thread.sleep(100);
        clientPair.hardwareClient.send("hardware vw 100 0");
        clientPair.hardwareClient.send("hardware vw 100 13000");
        clientPair.hardwareClient.send("hardware vw 101 36");
        clientPair.appClient.send("hardware 1 vw 112 25");
        clientPair.appClient.send("hardware 1 vw 111 20");
        clientPair.appClient.send("createWidget 1\0{\"id\":155, \"frequency\":400, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"GAUGE\", \"pinType\":\"VIRTUAL\", \"pin\":107}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));
        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 107 0.26340848"))));
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("vw 107 0.26340848"))));
        clientPair.appClient.send("deleteWidget 1\0 155");
    }

    @Test
    @Ignore("Not using device selector anymore")
    public void testAlcoholIsCorrectWithMultiDevicesAndDeviceSelector() throws Exception {
        Device device2 = new Device(1, "My Device", BoardType.ESP8266);
        device2.status = Status.OFFLINE;

        clientPair.appClient.send("createDevice 1\0" + device2.toString());
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice, 0);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(createDevice(1, device)));

        clientPair.appClient.send("createWidget 1\0{\"id\":200000, \"deviceIds\":[0], \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        clientPair.appClient.send("createWidget 1\0{\"id\":155, \"frequency\":400, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"GAUGE\", \"pinType\":\"VIRTUAL\", \"pin\":107}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.MAPPER.readValue(response, Device[].class);
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEquals(1, devices[1].id);
        assertEqualDevice(device2, devices[1]);
        assertNotNull(device2.plaato);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        hardClient2.send("login " + devices[1].token);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.reset();

        final ScheduledExecutorService ses = startReadingWidgetsWorker();
        sleep(100);

        clientPair.hardwareClient.send("hardware vw 100 0");
        clientPair.hardwareClient.send("hardware vw 100 13000");
        clientPair.hardwareClient.send("hardware vw 101 36");
        clientPair.appClient.send("hardware 1 vw 112 25");
        clientPair.appClient.send("hardware 1 vw 111 20");

        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 107 0.26340848"))));
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("vw 107 0.26340848"))));

        clientPair.appClient.send("hardware 1 vu 200000 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(new HardwareMessage(3, b("vu 200000 1"))));
        verify(hardClient2.responseMock, never()).channelRead(any(), eq(new HardwareMessage(3, b("vu 200000 1"))));

        hardClient2.send("hardware vw 100 0");
        hardClient2.send("hardware vw 100 14000");
        hardClient2.send("hardware vw 101 37");
        clientPair.appClient.send("hardware 1-1 vw 112 25");
        clientPair.appClient.send("hardware 1-1 vw 111 20");

        //waiting for worker
        sleep(1000);

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-1 vw 107 0.2836347"))));
        verify(hardClient2.responseMock, never()).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("vw 107 0.2836347"))));

        clientPair.appClient.reset();

        clientPair.appClient.send("hardware 1 vu 200000 0");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(MobileSyncWidget.SYNC_DEFAULT_MESSAGE_ID, APP_SYNC, b("1-0 vw 100 13000"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(MobileSyncWidget.SYNC_DEFAULT_MESSAGE_ID, APP_SYNC, b("1-0 vw 101 36"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(MobileSyncWidget.SYNC_DEFAULT_MESSAGE_ID, APP_SYNC, b("1-0 vw 107 0.31971326"))));

        clientPair.appClient.reset();

        clientPair.appClient.send("hardware 1 vu 200000 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(MobileSyncWidget.SYNC_DEFAULT_MESSAGE_ID, APP_SYNC, b("1-1 vw 100 14000"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(MobileSyncWidget.SYNC_DEFAULT_MESSAGE_ID, APP_SYNC, b("1-1 vw 101 37"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(MobileSyncWidget.SYNC_DEFAULT_MESSAGE_ID, APP_SYNC, b("1-1 vw 107 0.34424537"))));
    }

    @Test
    public void testAlcoholIsCorrectWithMultiDevicesAndDeviceSelectorWhenDevicesAreOffline() throws Exception {
        Device device2 = new Device(1, "My Device", BoardType.ESP8266);
        device2.status = Status.OFFLINE;

        clientPair.appClient.send("createDevice 1\0" + device2.toString());
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice, 0);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(createDevice(1, device)));

        clientPair.appClient.send("createWidget 1\0{\"id\":200000, \"deviceIds\":[0], \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        clientPair.appClient.send("createWidget 1\0{\"id\":155, \"frequency\":400, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"GAUGE\", \"pinType\":\"VIRTUAL\", \"pin\":107}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.MAPPER.readValue(response, Device[].class);
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEquals(1, devices[1].id);
        assertEqualDevice(device2, devices[1]);
        assertNotNull(device2.plaato);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        hardClient2.send("login " + devices[1].token);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.reset();

        final ScheduledExecutorService ses = startReadingWidgetsWorker();
        sleep(100);

        clientPair.hardwareClient.send("hardware vw 100 0");
        clientPair.hardwareClient.send("hardware vw 100 13000");
        clientPair.hardwareClient.send("hardware vw 101 36");
        clientPair.appClient.send("hardware 1 vw 112 25");
        clientPair.appClient.send("hardware 1 vw 111 20");

        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 107 0.26340848"))));
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("vw 107 0.26340848"))));

        clientPair.hardwareClient.stop();
        hardClient2.stop();

        clientPair.appClient.reset();

        //expecting that reading worker will anyway update app state
        verify(clientPair.appClient.responseMock, after(1100)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 107 0.26340848"))));
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("vw 107 0.26340848"))));
    }

    @Test
    public void testOfflineTimingIsCorrectForMultipleDevices() throws Exception {
        Device device2 = new Device(1, "My Device", BoardType.ESP8266);
        device2.status = Status.OFFLINE;

        clientPair.appClient.send("createDevice 1\0" + device2.toString());
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice,0 );
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(createDevice(1, device)));

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody(2);

        Device[] devices = JsonParser.MAPPER.readValue(response, Device[].class);
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEquals(1, devices[1].id);
        assertEqualDevice(device2, devices[1]);
        assertNotNull(device2.plaato);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.send("login " + devices[1].token);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        hardClient2.send("internal " + b("ver 0.3.1 h-beat 10 buff-in 256 dev Arduino cpu ATmega328P con W5100"));
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.hardwareClient.stop();

        sleep(1000);
        verify(clientPair.appClient.responseMock, never()).channelRead(any(), eq(deviceOffline(0, b("1-0"))));

        //waiting for "offline event". timeout is 2 seconds
        sleep(1000);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(deviceOffline(0, b("1-0"))));

        clientPair.appClient.reset();
        hardClient2.stop();

        sleep(1000);
        verify(clientPair.appClient.responseMock, never()).channelRead(any(), eq(deviceOffline(0, b("1-1"))));

        //waiting for "offline event". timeout is 2 seconds
        sleep(1000);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(deviceOffline(0, b("1-1"))));
    }

    @Test
    public void testOfflineTimingIsCorrectForMultipleDevices2() throws Exception {
        Device device2 = new Device(1, "My Device", BoardType.ESP8266);
        device2.status = Status.OFFLINE;

        clientPair.appClient.send("createDevice 1\0" + device2.toString());
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice, 0);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(createDevice(1, device)));

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody(2);

        Device[] devices = JsonParser.MAPPER.readValue(response, Device[].class);
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEquals(1, devices[1].id);
        assertEqualDevice(device2, devices[1]);
        assertNotNull(device2.plaato);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.send("login " + devices[1].token);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        hardClient2.send("internal " + b("ver 0.3.1 h-beat 1 buff-in 256 dev Arduino cpu ATmega328P con W5100"));
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.hardwareClient.channel.close();
        hardClient2.channel.close();

        verify(clientPair.appClient.responseMock, timeout(5000).times(1)).channelRead(any(), eq(deviceOffline(0, b("1-0"))));
        verify(clientPair.appClient.responseMock, timeout(5000).times(1)).channelRead(any(), eq(deviceOffline(0, b("1-1"))));
    }


    @Test
    public void testPlaatoHardwareStaysOnlineAfterReconnect() throws Exception {
        Device device2 = new Device(1, "My Device", BoardType.ESP8266);

        clientPair.appClient.createDevice(1, device2);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.send("login " + device.token);
        hardClient2.verifyResult(ok(1));

        hardClient2.send("internal " + b("ver 0.3.1 h-beat 1 buff-in 256 dev Arduino cpu ATmega328P con W5100"));
        hardClient2.verifyResult(ok(2));

        hardClient2.stop();
        //we should not receive anything within 1.5 second interval
        verify(clientPair.appClient.responseMock, after(1700).never()).channelRead(any(), eq(deviceOffline(0, b("1-1"))));

        hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.send("login " + device.token);
        hardClient2.verifyResult(ok(1));
        hardClient2.stop();

        //device was reconnected, so we should not get any offline notifications
        verify(clientPair.appClient.responseMock, after(1700).never()).channelRead(any(), eq(deviceOffline(0, b("1-1"))));

        hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.send("login " + device.token);
        hardClient2.verifyResult(ok(1));
        hardClient2.stop();

        //device was reconnected, so we should not get any offline notifications
        verify(clientPair.appClient.responseMock, after(1700).never()).channelRead(any(), eq(deviceOffline(0, b("1-1"))));
    }

    private static void assertEqualDevice(Device expected, Device real) {
        assertEquals(expected.id, real.id);
        //assertEquals(expected.name, real.name);
        assertEquals(expected.boardType, real.boardType);
        Assert.assertNotNull(real.token);
        assertEquals(expected.status, real.status);
    }
}
