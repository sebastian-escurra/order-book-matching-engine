# Limit Order Book and Matching Engine Simulator (Java)

A small simulator of how an exchange matches buy and sell limit orders for **one instrument**.
It implements **price-time priority** and **partial executions**, and it is verified by an automated test suite.

Written in plain Java (classes, objects, lists, loops). No external libraries.

## Project structure

| Class | Role |
|---|---|
| `Order` | One limit order: id, type (`"BUY"`/`"SELL"`), price, remaining quantity, arrival number |
| `Trade` | One execution: price, quantity, buy order id, sell order id |
| `OrderBook` | Two lists of resting orders (buys and sells), always kept sorted. The best order is at index 0 |
| `MatchingEngine` | Validates new orders, matches them against the book, records the trades |
| `Main` | Five demo scenarios: partial fills, price priority, no match / resting price, cancel and invalid orders, time priority |
| `OrderBookTests` | 25 automated checks, including a randomized test |

## Matching rules

1. A **BUY** matches a **SELL** when `buy price >= sell price` (and the reverse for a new SELL).
2. **Price priority:** the best price is served first (highest buy, lowest sell).
3. **Time priority:** at the same price, the order that arrived first is served first (FIFO).
4. **Trade price = price of the order already resting in the book.** The order that arrives and "crosses" gets the resting order's price, which can be better than its own limit.
5. **Partial executions:** the traded quantity is `min(buy quantity, sell quantity)`. A partly filled resting order keeps its place in the queue. If the *new* order is only partly filled, the remainder is added to the book.
6. A new order sweeps as many price levels as needed, as long as the prices still cross.

## Example

```java
engine.processOrder(new Order(1, "BUY", 100.0, 10));   // arrives first, waits in the book
engine.processOrder(new Order(2, "BUY", 100.0, 10));   // same price, arrives second, waits
engine.processOrder(new Order(3, "SELL", 100.0, 10));  // could trade with order 1 or order 2
```

The sell trades with order 1, because it arrived first. Order 2 keeps waiting:

```
TRADE EXECUTED: Trade{price=100.0, quantity=10, buyOrderId=1, sellOrderId=3}
```

## How it works (design decisions)

- **Sorted by insertion.** Instead of re-sorting the whole list after every order, each new order is inserted at its correct position: behind every order with a better *or equal* price. Equal price means "behind the older ones", which gives time priority without comparing timestamps.
- **The engine assigns the arrival number**, not the user. A user cannot jump the queue by typing an older timestamp.
- **The lists are private.** `OrderBook` exposes only what the engine needs (`getBestBuy`, `removeBestSell`, `cancelOrder`...). Nobody outside can break the sorting.
- **Validation at the entry point.** The engine rejects (and prints the reason for): a type other than exactly `BUY`/`SELL`, a price <= 0 or NaN, a quantity <= 0, a `null` order, and an id that is already resting in the book.
- **Trades are returned and stored.** `processOrder` returns the trades produced by that order, and the engine keeps the full `tradeHistory`. Printing is left to `Main`.

## Complexity

Let `n` be the number of resting orders.

| Operation | Cost | Why |
|---|---|---|
| Look at the best order | O(1) | it is at index 0 |
| Insert an order | O(n) | walk to the right position, then shift elements of the `ArrayList` |
| Remove the best order | O(n) | removing index 0 shifts the whole `ArrayList` |
| Cancel an order | O(n) | search by id |
| Duplicate-id check (per new order) | O(n) | search by id |

## How to run

**NetBeans:** open the folder as a project. Right-click `Main.java` or `OrderBookTests.java` and choose *Run File*.

**Command line** (from the project folder):

```
mkdir out
javac -d out src/orderbooksimulator/*.java
java -cp out orderbooksimulator.Main
java -cp out orderbooksimulator.OrderBookTests
```

## Tests

`OrderBookTests` prints `PASS`/`FAIL` for 25 checks:

- full match, partial fill of the resting order, partial fill of the incoming order, no match
- time priority, price priority, partial fills keeping their place in the queue
- trade price equals the resting order's price (both directions)
- one order sweeping several price levels
- cancelling, rejecting invalid orders, rejecting duplicate ids
- a **randomized test**: 5,000 random orders (fixed seed, so it is reproducible), checking after *every* order that
  (a) the book is never crossed (best buy < best sell),
  (b) both sides are always in price-time order,
  (c) quantity is conserved: `submitted = traded + still resting`, for the buy side and the sell side.

The tests were also checked to make sure they are not empty: deliberately introducing three bugs (wrong time priority, wrong trade price, wrong matching condition) made the tests fail each time.

## Limitations and possible extensions

- **Speed.** Insertion, removal, cancel and the duplicate-id check are O(n), so processing slows down as the book grows. A faster design would keep one queue per price level in a sorted structure (such as a `TreeMap`), making the best price and insertion O(log k) for k price levels, and use a `HashMap` by id to make cancel O(1).
- **Prices are `double`.** Prices are only compared here, never added up, so it works. A production system would use integer ticks (for example, prices in cents) to avoid rounding issues such as `0.1 + 0.2 != 0.3`.
- **Only limit orders.** No market orders, no order modification.
- **Duplicate ids are only detected among resting orders.** An id that was fully executed can be reused.
- Single instrument, single thread, no persistence.
