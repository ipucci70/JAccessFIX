package org.example;

import quickfix.*;
import quickfix.field.MsgType;
import quickfix.fix44.Logon;
import quickfix.fix44.MessageCracker;

public class FixClientApp extends MessageCracker implements Application {
    @Override
    public void onCreate(SessionID sessionId) {
        System.out.println("Session created: " + sessionId);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        System.out.println("Logged on to session: " + sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        System.out.println("Logged out from session: " + sessionId);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        if (isLogonMessage(message)) {
            Logon logon = (Logon) message;
            logon.setString(553, "yourUsername"); // Set username for logon (if required)
            logon.setString(554, "yourPassword"); // Set password for logon (if required)
        }
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) {
        System.out.println("Admin message received: " + message);
    }

    @Override
    public void toApp(Message message, SessionID sessionId) {
        System.out.println("Application message sent: " + message);
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) {
        System.out.println("Application message received: " + message);
        try {
            crack(message, sessionId);  // Process the received message
        } catch (UnsupportedMessageType e) {
            throw new RuntimeException(e);
        } catch (FieldNotFound e) {
            throw new RuntimeException(e);
        } catch (IncorrectTagValue e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isLogonMessage(Message message) {
        try {
            return message.getHeader().getString(MsgType.FIELD).equals(MsgType.LOGON);
        } catch (FieldNotFound e) {
            throw new RuntimeException(e);
        }
    }
}
