package indigo

import kotlin.system.exitProcess

fun main() {
    Game.play()
}


object Game {
    private val deck = Deck()
    private val player1 = Player(computer=false)
    private val player2 = Player(computer=true)
    private val table = Table()
    private val dealer = Dealer(deck)
    private var playFirst = false
    private var playerWonLast: Player? = null

    fun play() {
        deck.createDeck()
        table += deck.get(4)
        dealer.deal(player1)
        dealer.deal(player2)
        println("Indigo Card Game")

        var roundWon = false
        if (playFirst()) {
            roundWon = playerMoves()
        }

        checkScore(player1, roundWon)

        roundWon = computerMoves()

        checkScore(player2, roundWon)

        while (true) {
            roundWon = playerMoves()
            checkScore(player1, roundWon)

            roundWon = computerMoves()
            checkScore(player2, roundWon)

            if (player1.isHandEmpty()) {
                dealer.deal(player1)
            }

            if (player2.isHandEmpty()) {
                dealer.deal(player2)
            }
        }
    }

    private fun playFirst(): Boolean {
        var answer = ""
        while (answer !in listOf("yes", "no")) {
            println("Play first?")
            answer = readln()
        }

        playFirst = answer == "yes"

        table.printCards(initial = true)
        return answer == "yes"
    }

    private fun gameOver(player: Player) {
        val other = if (player == player1) player2 else player1

        if (player.cardsWon.size > other.cardsWon.size) {
            player.score += 3
        } else if (player.cardsWon.size < other.cardsWon.size) {
            other.score += 3
        } else if (playFirst) {
            player.score += 3
        } else {
            other.score += 3
        }

        if (playerWonLast?.computer == true) {
            player2.addCardsWon(table.removeAll())
        } else {
            player1.addCardsWon(table.removeAll())
        }

        checkScore(player, roundWon = true, gameOver = true)
        exit()
    }

    private fun exit() {
        println("Game Over")
        exitProcess(0)
    }

    private fun checkScore(player: Player, roundWon: Boolean, gameOver: Boolean = false) {
        if (!roundWon) {
            return
        }

        if (!gameOver) {
            println("${player.name} wins cards")
            playerWonLast = player
        }

        println("Score: Player ${player1.score} - Computer ${player2.score}")
        println("Cards: Player ${player1.cardsWon.size} - Computer ${player2.cardsWon.size}")
    }

    private fun playerMoves(): Boolean {
        table.printCards()

        if (deck.isEmpty() && player1.isHandEmpty() && player2.isHandEmpty()) {
            gameOver(player1)
        }


        println("Cards in hand: ${player1.handOptions()}")

        var index = ""
        while (index != "exit" && (!index.matches(Regex("""[1-9]+""")) || index.toInt() !in 1..player1.size())) {
            println("Choose a card to play (1-${player1.size()}):")
            index = readln()
        }
        if (index == "exit") {
            exit()
        }
        val card = player1.removeCard(index.toInt() - 1)

        return if (card == table.top()) {
            player1.addCardWon(card)
            player1.addCardsWon(table.removeAll())
            true
        } else {
            table += card
            false
        }
    }

    private fun computerMoves(): Boolean {
        table.printCards()

        if (deck.isEmpty() && player1.isHandEmpty() && player2.isHandEmpty()) {
            gameOver(player2)
        }

        println(player2.handOptions(indexed = false))

        val card: Card = if (table.isEmpty() || player2.candidates(table.top()) == null) {
            val cardsBySuit = player2.cardsGroupedBySuit()
            val cardsByRank = player2.cardsGroupedByRank()
            val cardToRemove = cardsBySuit?.first()?.first()
                ?: (cardsByRank?.first()?.first() ?: player2.removeCard(0))
            player2.removeExactCard(cardToRemove)
            cardToRemove
        } else if (player2.candidates(table.top()) != null) {
            val cardToRemove = player2.candidates(table.top())!!.first().first()
            player2.removeExactCard(cardToRemove)
            cardToRemove
        } else {
            player2.removeCard(0)
        }

        println("Computer plays ${card.icon()}\n")

        return if (card == table.top()) {
            player2.addCardWon(card)
            player2.addCardsWon(table.removeAll())
            true
        } else {
            table += card
            false
        }
    }
}

class Dealer(private val deck: Deck) {
    fun deal(player: Player) {
        if (deck.isEmpty()) {
            return
        }
        player += deck.get(6)
    }
}


class Deck {
    private val cards = mutableListOf<Card>()

    fun createDeck() {
        for (suit in Card.Suit.values()) {
            for (rank in Card.Rank.values()) {
                cards.add(Card(suit, rank))
            }
        }
        cards.reverse()

        shuffle()
    }

    private fun shuffle() {
        cards.shuffle()
    }

    fun get(count: Int): MutableList<Card> {
        val removedCards = mutableListOf<Card>()
        for (i in 0 until count) {
            removedCards.add(cards.removeAt(0))
        }

        return removedCards
    }

    fun isEmpty(): Boolean {
        return cards.isEmpty()
    }
}

class Player(val computer: Boolean) {
    val name: String = if (computer) {
        "Computer"
    } else {
        "Player"
    }

    private val hand = mutableListOf<Card>()
    val cardsWon = mutableListOf<Card>()
    var score = 0

    operator fun minus(index: Int) {
        hand.removeAt(index)
    }

    operator fun plusAssign(cards: MutableList<Card>) {
        hand.addAll(cards)
    }

    fun size(): Int {
        return hand.size
    }

    fun handOptions(indexed: Boolean = true): String {
        val options = StringBuilder()

        if (indexed) {
            for (i in 1 .. size()) {
                options.append("$i)${hand[i-1].icon()} ")
            }
        } else {
            options.append(hand.joinToString(" ") { it.icon() })
        }

        return options.toString().trimEnd()
    }

    fun addCardWon(card: Card) {
        if (card.rank in Card.worthyRanks()) {
            score += 1
        }
        cardsWon.add(card)
    }

    fun addCardsWon(cards: MutableList<Card>) {
        for (card in cards) {
            addCardWon(card)
        }
    }

    fun isHandEmpty(): Boolean {
        return hand.isEmpty()
    }

    fun removeCard(index: Int): Card {
        return hand.removeAt(index)
    }

    fun cardsGroupedBySuit(): MutableList<MutableList<Card>>? {
        val cards = MutableList<MutableList<Card>>(size = Card.Suit.values().size) { mutableListOf() }

        for ((index, suit) in Card.Suit.values().withIndex()) {
            for (card in hand) {
                if (card.suit == suit) {
                    cards[index].add(card)
                }
            }
        }

        if (cards.all { it.size <= 1 }) {
            return null
        }

        return cards.filter { it.size > 1 }.sortedBy { it.size }.reversed().toMutableList()
    }

    fun cardsGroupedByRank(): MutableList<MutableList<Card>>? {
        val cards = MutableList<MutableList<Card>>(size = Card.Rank.values().size) { mutableListOf() }

        for ((index, rank) in Card.Rank.values().withIndex()) {
            for (card in hand) {
                if (card.rank == rank) {
                    cards[index].add(card)
                }
            }
        }

        if (cards.all { it.size <= 1 }) {
            return null
        }

        return cards.filter { it.size > 1 }.sortedBy { it.size }.reversed().toMutableList()
    }

    fun candidates(top: Card?): MutableList<MutableList<Card>>? {
        if (top == null) {
            return null
        }

        val candidates = mutableListOf<Card>()
        for (card in hand) {
            if (card.rank == top.rank || card.suit == top.suit) {
                candidates.add(card)
            }
        }

        if (candidates.isEmpty()) {
            return null
        }

        val candidatesGroupedBySuit = MutableList<MutableList<Card>>(size = Card.Suit.values().size) { mutableListOf() }
        val candidatesGroupedByRank = MutableList<MutableList<Card>>(size = Card.Rank.values().size) { mutableListOf() }

        for ((index, suit) in Card.Suit.values().withIndex()) {
            for (card in candidates) {
                if (card.suit == suit) {
                    candidatesGroupedBySuit[index].add(card)
                }
            }
        }

        for ((index, rank) in Card.Rank.values().withIndex()) {
            for (card in candidates) {
                if (card.rank == rank) {
                    candidatesGroupedByRank[index].add(card)
                }
            }
        }

        return if (candidatesGroupedBySuit.any { it.size > 1 }) {
            candidatesGroupedBySuit.filter { it.size > 1 }.sortedBy { it.size }.reversed().toMutableList()
        } else if (candidatesGroupedByRank.any { it.size > 1 }) {
            candidatesGroupedByRank.filter { it.size > 1 }.sortedBy { it.size }.reversed().toMutableList()
        } else {
            mutableListOf(candidates)
        }
    }

    fun removeExactCard(cardToRemove: Card) {
        for ((index, card) in hand.withIndex()) {
            if (card.suit == cardToRemove.suit && card.rank == cardToRemove.rank) {
                hand.removeAt(index)
                return
            }
        }
    }
}

class Table {
    private val cards = mutableListOf<Card>()

    fun top(): Card? {
        return cards.lastOrNull()
    }

    operator fun plusAssign(card: Card) {
        cards.add(card)
    }

    operator fun plusAssign(cards: MutableList<Card>) {
        this.cards.addAll(cards)
    }

    fun printCards(initial: Boolean = false) {
        val result = mutableListOf<String>()
        for (card in cards) {
            result.add(card.icon())
        }
        println(
            if (initial) {
                "Initial cards on the table: ${result.joinToString(separator = " ")}\n"
            } else {
                if (top() == null) {
                    "\nNo cards on the table"
                } else {
                    "${cards.size} cards on the table, and the top card is ${top()?.icon()}"
                }
            }
        )
    }

    fun removeAll(): MutableList<Card> {
        val removedCards = cards.toMutableList()
        cards.clear()
        return removedCards
    }

    fun isEmpty(): Boolean {
        return cards.isEmpty()
    }
}

class Card(val suit: Suit, val rank: Rank) {
    enum class Suit(val icon: String) {
        SPADES("♠"), HEARTS("♥"), DIAMONDS("♦"), CLUBS("♣");
    }
    enum class Rank(val icon: String) {
        ACE("A"), TWO("2"), THREE("3"), FOUR("4"), FIVE("5"), SIX("6"), SEVEN("7"),
        EIGHT("8"), NINE("9"), TEN("10"), JACK("J"), QUEEN("Q"), KING("K");
    }

    fun icon(): String {
        return rank.icon + suit.icon
    }

    override fun equals(other: Any?): Boolean {
        return !(other == null || other !is Card || (rank != other.rank) && suit != other.suit)
    }

    override fun hashCode(): Int {
        var result = suit.hashCode()
        result = 31 * result + rank.hashCode()
        return result
    }

    companion object {
        fun worthyRanks(): List<Rank> {
            return listOf(Rank.ACE, Rank.JACK, Rank.QUEEN, Rank.KING, Rank.TEN)
        }
    }
}