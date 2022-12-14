package de.adv.rfsprojekt.websocket;

import com.google.gson.Gson;
import de.adv.rfsprojekt.service.manualMovement.ManualMovementController;
import de.adv.rfsprojekt.service.shared.ErrorAnalyzer;
import de.adv.rfsprojekt.ur_new.rtde.entities.packages.MessagePacket;
import de.adv.rfsprojekt.ur_new.rtde.entities.packages.data.DataPackage;
import de.adv.rfsprojekt.websocket.entities.*;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * ToDO Clientname kann redundant sein, eventuell ändern
 **/
@ServerEndpoint("/manual/{clientname}")
@ApplicationScoped
public class ManualMovementSocket {
    Map<String, Session> sessions = new ConcurrentHashMap<>();

    @Inject
    ManualMovementController mmC;

    @Inject
    Gson gson;


    @OnOpen
    public void onOpen(Session session, @PathParam("clientname") String clientname) {
        sessions.put(clientname, session);
    }

    @OnClose
    public void onClose(Session session, @PathParam("clientname") String clientname) {
        sessions.remove(clientname);
    }


     @OnError
     public void onError(Session session, @PathParam("clientname") String clientname, Throwable throwable) {
        var errorMessage = new ErrorMessage(new ErrorPayload(throwable.getMessage()));

        broadcast(errorMessage);
     }


    @OnMessage
    public void onMessage(String message, @PathParam("clientname") String clientname) {
        var type = MessageType.valueOf(StringUtils.substringsBetween(message, "\"", "\"")[1]);
        var messageClass = switch (type) {
            case ERROR, INFO -> ErrorMessage.class;
            case COMMAND -> ManualMovementCommand.class;
        };
        var m = gson.fromJson(message, messageClass);
        if (m instanceof ManualMovementCommand command) {
            try {
                mmC.executeMove(command.getPayload());
            } catch (IOException | InterruptedException e) {
                sessions.get(clientname).getAsyncRemote().sendText("Upsi Fehler");
            }
        }
    }

    private void broadcast(WebsocketMessage<?> message) {
        sessions.values().forEach(s -> {
            s.getAsyncRemote().sendObject(gson.toJson(message), result ->  {
                if (result.getException() != null) {
                    System.out.println("Unable to send message: " + result.getException());
                }
            });
        });
    }
}
