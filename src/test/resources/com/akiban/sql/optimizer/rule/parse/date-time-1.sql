SELECT DATE('2011-01-01'), TIME('12:59'), TIMESTAMP('2011-01-01', '01:01:01'),
 YEAR(order_date), MONTH(order_date), DAY(order_date),
 HOUR(current_time), MINUTE(CURRENT TIME), SECOND(CURRENT_TIMESTAMP)
FROM orders
