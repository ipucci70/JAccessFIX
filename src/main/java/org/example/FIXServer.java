package org.example;

import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.QuoteRequest;
import quickfix.fix44.Quote;
import quickfix.fix44.QuoteResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;

public class FIXServer extends MessageCracker implements Application {

    private static final Logger LOG = LogManager.getLogger(FIXServer.class);

    @Override
    public void onCreate(SessionID sessionId) {
        LOG.info("Session created: " + sessionId);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        LOG.info("Logon successful: " + sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        LOG.info("Logout: " + sessionId);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        LOG.info("Admin Message: " + sessionId);
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) {
        LOG.info("From Admin: " + sessionId);
    }

    @Override
    public void toApp(Message message, SessionID sessionId) {
        LOG.info("To App: " + sessionId);
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        LOG.info("From App: " + sessionId);
        crack(message, sessionId);
    }


    // Handle Quote Request (MsgType = R)
    //@Override
    public void onMessage(QuoteRequest quoteRequest, SessionID sessionId) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        System.out.println("Processing Quote Request...");

        String quoteReqId = quoteRequest.getQuoteReqID().getValue();
        LOG.info("QuoteReqID: " + quoteReqId);

        try {
            if (quoteRequest.isSetField(NoRelatedSym.FIELD)) {
                int noRelatedSymCount = quoteRequest.getInt(NoRelatedSym.FIELD);
                LOG.info("Number of Related Symbols: " + noRelatedSymCount);

                // Loop through each NoRelatedSym group
                for (int i = 1; i <= noRelatedSymCount; i++) {
                    Group relatedSymGroup = quoteRequest.getGroup(i, NoRelatedSym.FIELD);

                    // Retrieve fields within the group (e.g., Symbol)
                    if (relatedSymGroup.isSetField(Symbol.FIELD)) {
                        String symbol = relatedSymGroup.getString(Symbol.FIELD);
                        LOG.info("Symbol: " + symbol);
                    }
                }
            }
        }catch(FieldNotFound e) {
            e.printStackTrace();
        }

    }

    // Handle Quote (MsgType = S)
    //@Override
    public void onMessage(Quote message, SessionID sessionId) throws FieldNotFound {
        System.out.println("Processing Quote...");

        String quoteId = message.getQuoteID().getValue();
        String symbol = message.getSymbol().getValue();
        double bidPrice = message.isSetField(BidPx.FIELD) ? message.getBidPx().getValue() : 0.0;
        double offerPrice = message.isSetField(OfferPx.FIELD) ? message.getOfferPx().getValue() : 0.0;

        System.out.printf("Received Quote: ID=%s, Symbol=%s, Bid=%.2f, Offer=%.2f%n",
                quoteId, symbol, bidPrice, offerPrice);

        // Respond with a Quote Response
        QuoteResponse response = new QuoteResponse();
        response.set(new QuoteRespID("RESP-" + quoteId));
        response.set(new QuoteID(quoteId));
        response.set(new QuoteRespType(QuoteRespType.HIT_LIFT)); // Example: Accepted

        try {
            Session.sendToTarget(response, sessionId);
            System.out.println("Sent Quote Response for Quote ID: " + quoteId);
        } catch (SessionNotFound e) {
            System.err.println("Error sending Quote Response: " + e.getMessage());
        }
    }

    // Handle Quote Response (MsgType = AJ)
    //@Override
    public void onMessage(QuoteResponse message, SessionID sessionId) throws FieldNotFound {
        LOG.info("Processing Quote Response...");

        String quoteRespId = message.getQuoteRespID().getValue();
        int responseType = message.getQuoteRespType().getValue();

        LOG.info("Received Quote Response: ID=%s, Type=%d%n", quoteRespId, responseType);
    }


    public static void main(String[] args) throws ConfigError {
        String configFile = "quickfix.cfg"; // Path to the FIX configuration file
        SessionSettings settings = new SessionSettings(configFile);

        LOG.debug("This is a DEBUG message.");
        LOG.info("This is an INFO message.");
        LOG.warn("This is a WARN message.");
        LOG.error("This is an ERROR message.");
        LOG.fatal("This is a FATAL message.");

        // Simulate application logic
        try {
            simulateError();
        } catch (Exception e) {
            LOG.error("An exception occurred: ", e);
        }

    
        Application application = new FIXServer();
        MessageStoreFactory storeFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new ScreenLogFactory(settings);
        MessageFactory messageFactory = new DefaultMessageFactory();

        ThreadedSocketAcceptor acceptor = new ThreadedSocketAcceptor(application, storeFactory, settings, logFactory, messageFactory);
        acceptor.start();
    
        System.out.println("FIX Application started. Press <Enter> to stop.");
        LOG.info("FIX Application started. Press <Enter> to stop.");
        try {
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        acceptor.stop();
        System.out.println("FIX Application stopped.");
    }

    private static void simulateError() throws Exception {
        throw new Exception("Simulated exception");
    }
}
