package cc.blynk.integration.tcp.plaato;

import cc.blynk.integration.BaseTest;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.core.model.widgets.FrequencyWidget;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.MobileAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ScheduledExecutorService;

import static cc.blynk.integration.TestUtil.b;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class PlaatoPluginIntegrationTest3 extends BaseTest {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareAndHttpAPIServer(holder).start();
        this.appServer = new MobileAndHttpsServer(holder).start();
        this.clientPair = initAppAndHardPair("user_profile_json_plaato_3.txt");
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }

    @Test
    public void testInitialValuesAfterHardwareSend() throws Exception {
        ScheduledExecutorService ses = startReadingWidgetsWorker();
        Thread.sleep(100);
        clientPair.hardwareClient.send("hardware vw 100 5");
        clientPair.hardwareClient.send("hardware vw 101 18.7");

        //fermentation activity
        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 102 0"))));

        //alcohol level
        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 107 0.0064994292"))));

        //temperature
        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 103 15.933710098266602"))));

        //just labels
        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 109 L"))));
        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 108 °C"))));

        //specific gravity
        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 106 1.0099486"))));

        //Original Gravity
        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 105 1.01"))));

        verify(clientPair.appClient.responseMock, times(8)).channelRead(any(), any());

        clientPair.appClient.send("deleteWidget 1\0 155");
    }

}
