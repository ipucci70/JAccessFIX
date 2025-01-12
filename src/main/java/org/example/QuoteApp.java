package org.example;

import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.*;

public class QuoteApp extends MessageCracker implements Application {
    @Override
    public void onCreate(SessionID sessionId) {
        System.out.println("Session created: " + sessionId);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        System.out.println("Logon successful: " + sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        System.out.println("Logout: " + sessionId);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        System.out.println("Admin Message: " + message);
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) {
        System.out.println("From Admin: " + message);
    }

    @Override
    public void toApp(Message message, SessionID sessionId) {
        System.out.println("To App: " + message);
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        System.out.println("From App: " + message);
        crack(message, sessionId);
    }

    // Handle Quote Request (MsgType = R)
    @Override
    public void onMessage(QuoteRequest message, SessionID sessionId) throws FieldNotFound {
        System.out.println("Processing Quote Request...");

        String quoteReqId = message.getQuoteReqID().getValue();
        System.out.println("QuoteReqID: " + quoteReqId);

        for (QuoteRequest.NoRelatedSym group : message.getGroups(NoRelatedSym.FIELD)) {
            String symbol = group.getSymbol().getValue();
            System.out.println("Requested Symbol: " + symbol);

            // Respond with a Quote
            Quote quote = new Quote();
            quote.set(new QuoteID("QUOTE-" + System.currentTimeMillis()));
            quote.set(new QuoteReqID(quoteReqId));
            quote.set(new Symbol(symbol));
            quote.set(new BidPx(100.0)); // Example bid price
            quote.set(new OfferPx(105.0)); // Example offer price

            try {
                Session.sendToTarget(quote, sessionId);
                System.out.println("Sent Quote for symbol: " + symbol);
            } catch (SessionNotFound e) {
                System.err.println("Error sending Quote: " + e.getMessage());
            }
        }
    }

    // Handle Quote (MsgType = S)
    @Override
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
        response.set(new QuoteRespType(QuoteRespType.ACCEPTED)); // Example: Accepted

        try {
            Session.sendToTarget(response, sessionId);
            System.out.println("Sent Quote Response for Quote ID: " + quoteId);
        } catch (SessionNotFound e) {
            System.err.println("Error sending Quote Response: " + e.getMessage());
        }
    }

    // Handle Quote Response (MsgType = AJ)
    @Override
    public void onMessage(QuoteResponse message, SessionID sessionId) throws FieldNotFound {
        System.out.println("Processing Quote Response...");

        String quoteRespId = message.getQuoteRespID().getValue();
        int responseType = message.getQuoteRespType().getValue();

        System.out.printf("Received Quote Response: ID=%s, Type=%d%n", quoteRespId, responseType);
    }
}
