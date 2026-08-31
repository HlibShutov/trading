package com.trading.matcher;

import com.trading.matcher.service.MatcherService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.trading.matcher.model.Market;
import com.trading.matcher.model.Order;
import com.trading.matcher.model.OrderBook;
import com.trading.matcher.model.OrderSide;
import com.trading.matcher.producer.TradeEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class MatcherApplicationTests {
	private TradeEventPublisher tradeEventPublisher;
	private MatcherService matcherService;

	@BeforeEach
	void setUp() {
		tradeEventPublisher = mock(TradeEventPublisher.class);
		matcherService = new MatcherService(tradeEventPublisher);
	}

	@Test
	void shouldMatchBuyAndSellAtSamePrice() {
		Order sell = sellOrder(
				1L,
				2L,
				"1",
				"100"
		);

		Order buy = buyOrder(
				2L,
				3L,
				"1",
				"100"
		);

		matcherService.placeOrder(sell);
		matcherService.placeOrder(buy);

		assertEquals(
				new BigDecimal("0"),
				buy.getRemainingQuantity()
		);

		assertEquals(
				new BigDecimal("0"),
				sell.getRemainingQuantity()
		);

		verify(tradeEventPublisher).publish(any());
	}

	@Test
	void shouldMatchWhenBuyPriceIsHigherThanSellPrice() {
		Order sell = sellOrder(
				1L,
				2L,
				"1",
				"100"
		);

		Order buy = buyOrder(
				2L,
				3L,
				"1",
				"110"
		);

		matcherService.placeOrder(sell);
		matcherService.placeOrder(buy);

		assertEquals(
				new BigDecimal("0"),
				buy.getRemainingQuantity()
		);

		assertEquals(
				new BigDecimal("0"),
				sell.getRemainingQuantity()
		);

		verify(tradeEventPublisher).publish(any());
	}

	@Test
	void shouldNotMatchWhenPricesDoNotCross() {
		Order sell = sellOrder(
				1L,
				2L,
				"1",
				"110"
		);

		Order buy = buyOrder(
				2L,
				3L,
				"1",
				"100"
		);

		matcherService.placeOrder(sell);
		matcherService.placeOrder(buy);

		assertEquals(
				new BigDecimal("1"),
				buy.getRemainingQuantity()
		);

		assertEquals(
				new BigDecimal("1"),
				sell.getRemainingQuantity()
		);

		verify(tradeEventPublisher, never()).publish(any());
	}

	@Test
	void shouldPartiallyFillSellOrder() {
		Order sell = sellOrder(
				1L,
				2L,
				"2",
				"100"
		);

		Order buy = buyOrder(
				2L,
				3L,
				"1",
				"100"
		);

		matcherService.placeOrder(sell);
		matcherService.placeOrder(buy);

		assertEquals(
				new BigDecimal("0"),
				buy.getRemainingQuantity()
		);

		assertEquals(
				new BigDecimal("1"),
				sell.getRemainingQuantity()
		);

		verify(tradeEventPublisher).publish(any());
	}

	@Test
	void shouldPartiallyFillBuyOrder() {
		Order sell = sellOrder(
				1L,
				2L,
				"1",
				"100"
		);

		Order buy = buyOrder(
				2L,
				3L,
				"2",
				"100"
		);

		matcherService.placeOrder(sell);
		matcherService.placeOrder(buy);

		assertEquals(
				new BigDecimal("0"),
				sell.getRemainingQuantity()
		);

		assertEquals(
				new BigDecimal("1"),
				buy.getRemainingQuantity()
		);

		verify(tradeEventPublisher).publish(any());
	}

	@Test
	void shouldMatchAgainstMultipleSellOrders() {
		Order sell1 = sellOrder(
				1L,
				2L,
				"1",
				"100"
		);

		Order sell2 = sellOrder(
				2L,
				3L,
				"1",
				"101"
		);

		Order buy = buyOrder(
				3L,
				4L,
				"2",
				"110"
		);

		matcherService.placeOrder(sell1);
		matcherService.placeOrder(sell2);
		matcherService.placeOrder(buy);

		assertEquals(
				new BigDecimal("0"),
				sell1.getRemainingQuantity()
		);

		assertEquals(
				new BigDecimal("0"),
				sell2.getRemainingQuantity()
		);

		assertEquals(
				new BigDecimal("0"),
				buy.getRemainingQuantity()
		);

		verify(tradeEventPublisher, times(2))
				.publish(any());
	}

	@Test
	void shouldRespectPricePriority() {
		Order expensiveSell = sellOrder(
				1L,
				2L,
				"1",
				"101"
		);

		Order cheapSell = sellOrder(
				2L,
				3L,
				"1",
				"99"
		);

		Order buy = buyOrder(
				3L,
				4L,
				"1",
				"110"
		);

		matcherService.placeOrder(expensiveSell);
		matcherService.placeOrder(cheapSell);
		matcherService.placeOrder(buy);

		ArgumentCaptor<com.trading.matcher.model.TradeExecutedEvent> captor =
				ArgumentCaptor.forClass(
						com.trading.matcher.model.TradeExecutedEvent.class
				);

		verify(tradeEventPublisher, times(1))
				.publish(captor.capture());

		assertEquals(
				2L,
				captor.getValue().sellOrderId()
		);

		assertEquals(
				"99",
				captor.getValue().price()
		);

		assertEquals(
				new BigDecimal("0"),
				cheapSell.getRemainingQuantity()
		);

		assertEquals(
				new BigDecimal("1"),
				expensiveSell.getRemainingQuantity()
		);
	}

	@Test
	void shouldKeepUnfilledBuyOrderInOrderBook() {
		Order buy = buyOrder(
				1L,
				2L,
				"1",
				"100"
		);

		matcherService.placeOrder(buy);

		OrderBook orderBook =
				getOrderBook(new Market("BTC", "USDT"));

		assertEquals(
				1,
				orderBook.getBuyQueue().size()
		);

		assertSame(
				buy,
				orderBook.getBuyQueue().peek()
		);
	}

	@Test
	void shouldKeepUnfilledSellOrderInOrderBook() {
		Order sell = sellOrder(
				1L,
				2L,
				"1",
				"100"
		);

		matcherService.placeOrder(sell);

		OrderBook orderBook =
				getOrderBook(new Market("BTC", "USDT"));

		assertEquals(
				1,
				orderBook.getSellQueue().size()
		);

		assertSame(
				sell,
				orderBook.getSellQueue().peek()
		);
	}

	@Test
	void shouldNotMatchOrdersFromDifferentMarkets() {
		Order btcSell = new Order(
				1L,
				2L,
				OrderSide.SELL,
				"BTC",
				"USDT",
				new BigDecimal("1"),
				new BigDecimal("100")
		);

		Order ethBuy = new Order(
				2L,
				3L,
				OrderSide.BUY,
				"ETH",
				"USDT",
				new BigDecimal("1"),
				new BigDecimal("100")
		);

		matcherService.placeOrder(btcSell);
		matcherService.placeOrder(ethBuy);

		assertEquals(
				new BigDecimal("1"),
				btcSell.getRemainingQuantity()
		);

		assertEquals(
				new BigDecimal("1"),
				ethBuy.getRemainingQuantity()
		);

		verify(tradeEventPublisher, never()).publish(any());
	}

	private Order buyOrder(
			Long orderId,
			Long userId,
			String quantity,
			String price
	) {
		return new Order(
				orderId,
				userId,
				OrderSide.BUY,
				"BTC",
				"USDT",
				new BigDecimal(quantity),
				new BigDecimal(price)
		);
	}

	private Order sellOrder(
			Long orderId,
			Long userId,
			String quantity,
			String price
	) {
		return new Order(
				orderId,
				userId,
				OrderSide.SELL,
				"BTC",
				"USDT",
				new BigDecimal(quantity),
				new BigDecimal(price)
		);
	}

	private OrderBook getOrderBook(Market market) {
		try {
			var field =
					MatcherService.class
							.getDeclaredField("orderBooks");

			field.setAccessible(true);

			@SuppressWarnings("unchecked")
			var orderBooks =
					(java.util.Map<Market, OrderBook>)
							field.get(matcherService);

			return orderBooks.get(market);

		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
