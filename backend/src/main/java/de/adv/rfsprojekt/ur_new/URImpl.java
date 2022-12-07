package de.adv.rfsprojekt.ur_new;

import de.adv.rfsprojekt.ur_new.entities.URConnection;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static de.adv.rfsprojekt.ur_new.urscript_commands.SetupCommands.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class URImpl implements UR {

    private final String host;
    private final int port;

    private final URConnection urConnection;

    @ConfigProperty(name = "ur.dashboard_port")
    int dashboardPort;


    public URImpl(String host, int port, boolean enabled) throws IOException {

        this.host = host;
        this.port = port;

        if (enabled) {
            Socket socket = new Socket(host, port);
            urConnection = new URConnectionImpl(socket.getInputStream(), socket.getOutputStream());
        } else {
            urConnection = null;
        }
    }

    /**
     * ToDo Bei Sicherheitsstopp muss Roboter wieder entriegelt werden. Wird in alter Lib in powerOn Methode geregelt
     */
    public void powerOn() throws IOException, InterruptedException {
        try (Socket dashSocket = new Socket(host, dashboardPort);) {
            OutputStream os = dashSocket.getOutputStream();
            InputStream is = dashSocket.getInputStream();
            String message;

            os.write(POWER_ON().getBytes(UTF_8));
            os.flush();
            Thread.sleep(50);


            os.write(BRAKE_RELEASE().getBytes(UTF_8));
            os.flush();
            Thread.sleep(50);
        }


    }


    public void powerOff() throws IOException {
        try (Socket dashSocket = new Socket(host, dashboardPort);) {
            OutputStream os = dashSocket.getOutputStream();
            os.write(POWER_OFF().getBytes(UTF_8));
            os.flush();
        }
    }

    public URScriptBuilder buildScript() {
        return new URScriptBuilderImpl(urConnection);
    }

    public GripperCommander commandGripper() {
        return new GripperCommanderImpl(urConnection);
    }


    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public URConnection getURConnection() {
        return urConnection;
    }


}
