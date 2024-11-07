package cc.blynk.integration.tcp.plaato;

import cc.blynk.integration.BaseTest;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.core.model.widgets.FrequencyWidget;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.MobileAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ScheduledExecutorService;

import static cc.blynk.integration.TestUtil.b;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class PlaatoPluginIntegrationTest2 extends BaseTest {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareAndHttpAPIServer(holder).start();
        this.appServer = new MobileAndHttpsServer(holder).start();
        this.clientPair = initAppAndHardPair("user_profile_json_plaato_2.txt");
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }

    @Test
    public void testCaseAddValuesToESP() throws Exception {
        ScheduledExecutorService ses = startReadingWidgetsWorker();
        clientPair.appClient.send("hardware 1-252521 vw 114 2");// °C
        clientPair.appClient.send("hardware 1-252521 vw 115 2");// L - Liter
        clientPair.appClient.send("hardware 1-252521 vw 111 250");// 25.0
        clientPair.appClient.send("hardware 1-252521 vw 112 6");// 5

        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 102 --"))));
        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 107 0.0"))));

        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 104 25.0"))));
        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 103 --"))));
        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 109 L"))));
        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 108 °C"))));
        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-0 vw 105 1.005"))));
    }

    @Test
    @Ignore("profile is wrong right now")
    public void testCaseAddValuesToMultipleESP() throws Exception {
        ScheduledExecutorService ses = startReadingWidgetsWorker();
        clientPair.appClient.send("hardware 1-252521 vw 114 2");// °C
        clientPair.appClient.send("hardware 1-252521 vw 115 2");// L - Liter
        clientPair.appClient.send("hardware 1-252521 vw 111 250");// 25.0
        clientPair.appClient.send("hardware 1-252521 vw 112 6");// 5

        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1 vw 102 --"))));
        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1 vw 107 0.0"))));

        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1 vw 104 25.0"))));
        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1 vw 103 --"))));
        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1 vw 109 L"))));
        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1 vw 108 °C"))));
        verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1 vw 105 1.005"))));

        clientPair.appClient.reset();

        clientPair.appClient.send("hardware 1 vu 252521 10786");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("getToken 1-10786");
        String token = clientPair.appClient.getBody(2);
        assertNotNull(token);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.send("login " + token);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.reset();

        clientPair.appClient.send("hardware 1-252521 vw 114 2");// °C
        clientPair.appClient.send("hardware 1-252521 vw 115 2");// L - Liter
        clientPair.appClient.send("hardware 1-252521 vw 111 250");// 25.0
        clientPair.appClient.send("hardware 1-252521 vw 112 6");// 5

        //verify(clientPair.appClient.responseMock, timeout(1200)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1 vw 102 0"))));
        //verify(clientPair.appClient.responseMock, timeout(1200)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1 vw 107 0.0"))));

        verify(clientPair.appClient.responseMock, timeout(2200)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-10786 vw 104 25.0"))));
        verify(clientPair.appClient.responseMock, timeout(2200)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-10786 vw 103 --"))));
        verify(clientPair.appClient.responseMock, timeout(2200)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-10786 vw 109 L"))));
        verify(clientPair.appClient.responseMock, timeout(2200)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-10786 vw 108 °C"))));
        verify(clientPair.appClient.responseMock, timeout(2200)).channelRead(any(), eq(produce(FrequencyWidget.READING_MSG_ID, HARDWARE, b("1-10786 vw 105 1.005"))));

    }

}
