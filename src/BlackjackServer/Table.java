package BlackjackServer;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * Table objects represent a Blackjack table that players can join.
 *
 * @author Jordan Segalman
 */

public class Table implements Runnable {
    private static final int NUMBER_OF_DECKS = 6;
    private static final int MINIMUM_NUMBER_OF_CARDS_BEFORE_SHUFFLE = 78;
    private static final int MAXIMUM_SCORE = 21;
    private static final int DEALER_HIT_THRESHOLD = 17;
    private static final int MINIMUM_BET = 500;
    private ArrayList<Player> table = new ArrayList<>();    // holds the players at the table
    private Shoe shoe;                                      // shoe being used to deal cards
    private BlackjackHand dealerHand = new BlackjackHand(); // dealer hand to hold cards
    private boolean dealerHasBlackjack;                     // true if dealer has Blackjack, false if does not
    private CountDownLatch placedBetsLatch;                 // latch to wait for all players to place their bets
    private CountDownLatch placedInsuranceBetsLatch;        // latch to wait for all players to place their insurance bets
    private CountDownLatch turnLatch;                       // latch to wait for all players to be ready for their turns
    private CountDownLatch continuePlayingLatch;            // latch to wait for all players to determine if they will keep playing

    /**
     * Table thread run method.
     */

    @Override
    public void run() {
        shoe = new Shoe(NUMBER_OF_DECKS);
        shoe.shuffle();
        do {
            playBlackjack();
        } while (numPlayers() > 0);
    }

    /**
     * Plays Blackjack.
     */

    private void playBlackjack() {
        setup();
        for (Player player : table) {
            player.startLatchCountDown();
        }
        try {
            placedBetsLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (Player player : table) {
            player.betLatchCountDown();
        }
        dealInitialCards();
        for (Player player : table) {
            player.dealLatchCountDown();
        }
        try {
            placedInsuranceBetsLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (Player player : table) {
            player.insuranceBetLatchCountDown();
        }
        try {
            turnLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (Player player : table) {
            player.takeTurn(player.originalPlayerHand());
        }
        dealerTurn();
        for (Player player : table) {
            player.dealerTurnLatchCountDown();
        }
        try {
            continuePlayingLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the table up for a new round of Blackjack.
     */

    private void setup() {
        if (shoe.remainingCards() < MINIMUM_NUMBER_OF_CARDS_BEFORE_SHUFFLE) {
            shoe = new Shoe(NUMBER_OF_DECKS);
            shoe.shuffle();
        }
        dealerHand.clear();
        dealerHasBlackjack = false;
        placedBetsLatch = new CountDownLatch(numPlayers());
        placedInsuranceBetsLatch = new CountDownLatch(numPlayers());
        turnLatch = new CountDownLatch(numPlayers());
        continuePlayingLatch = new CountDownLatch(numPlayers());
    }

    /**
     * Deals the first two cards to each player and the dealer.
     */

    private void dealInitialCards() {
        for (int i = 0; i < 2; i++) {
            dealerHand.addCard(shoe.dealCard());
            for (Player player : table) {
                player.originalPlayerHand().addCard(shoe.dealCard());
            }
        }
        if (dealerHand.blackjackValue() == MAXIMUM_SCORE) {
            dealerHasBlackjack = true;
        }
    }

    /**
     * Performs the dealer's turn.
     */

    private void dealerTurn() {
        while ((dealerHand.isSoft() && dealerHand.blackjackValue() == DEALER_HIT_THRESHOLD) || dealerHand.blackjackValue() < DEALER_HIT_THRESHOLD) {
            dealerHand.addCard(shoe.dealCard());
        }
    }

    /**
     * Adds a player to the table.
     *
     * @param player Player to add to table
     */

    public void addPlayer(Player player) {
        table.add(player);
    }

    /**
     * Removes a player from the table.
     *
     * @param player Player to remove from table
     */

    public void removePlayer(Player player) {
        table.remove(player);
    }

    /**
     * Returns the number of players at the table.
     *
     * @return the number of players at the table
     */

    public int numPlayers() {
        return table.size();
    }

    /**
     * Returns the minimum bet of the table.
     *
     * @return the minimum bet of the table
     */

    public double minimumBet() {
        return MINIMUM_BET;
    }

    /**
     * Returns whether or not the dealer has Blackjack.
     *
     * @return true if the dealer has Blackjack, false if does not
     */

    public boolean dealerHasBlackjack() {
        return dealerHasBlackjack;
    }

    /**
     * Returns the card the dealer is showing.
     *
     * @return the card the dealer is showing
     */

    public Card dealerShownCard() {
        return dealerHand.getCard(0);
    }

    /**
     * Returns the dealer hand.
     *
     * @return the dealer hand
     */

    public BlackjackHand dealerHand() {
        return dealerHand;
    }

    /**
     * Returns a card dealt from the shoe.
     *
     * @return a card dealt from the shoe
     */

    public Card dealCard() {
        return shoe.dealCard();
    }

    /**
     * Decrements the placed bets latch.
     */

    public void placedBetsLatchCountDown() {
        placedBetsLatch.countDown();
    }

    /**
     * Decrements the placed insurance bets latch.
     */

    public void placedInsuranceBetsLatchCountDown() {
        placedInsuranceBetsLatch.countDown();
    }

    /**
     * Decrements the turn latch.
     */

    public void turnLatchCountDown() {
        turnLatch.countDown();
    }

    /**
     * Decrements the continue playing latch.
     */

    public void continuePlayingLatchCountDown() {
        continuePlayingLatch.countDown();
    }
}