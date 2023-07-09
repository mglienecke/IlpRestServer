package uk.ac.ed.inf.ilpRestServer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.andreinc.mockneat.MockNeat;
import uk.ac.ed.inf.ilp.constant.InvalidOrderReasonCode;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Pizza;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.gsonUtils.LocalDateDeserializer;
import uk.ac.ed.inf.ilp.gsonUtils.LocalDateSerializer;
import uk.ac.ed.inf.ilpRestServer.controller.IlpRestService;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static net.andreinc.mockneat.types.enums.CreditCardType.*;

/**
 * a sample order data generator (JSON-format)
 */
public class IlpOrdersSampleDataGenerator {

    private static int restaurantIndex = 0;

    public static Order CreateSampleOrder(LocalDate currentDate, OrderStatus orderStatus, InvalidOrderReasonCode reasonCodeForFailure, MockNeat mock, Restaurant[] restaurants){
        var order = new Order();
        order.setOrderNo(String.format("%08X", ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE)));
        order.setOrderDate(currentDate);
        order.setCvv(mock.cvvs().get());
        order.setCreditCardNumber(mock.creditCards().types(VISA_16, MASTERCARD).get());
        order.setCustomer(mock.names().get());
        order.setCreditCardExpiry(String.format("%02d/%02d", ThreadLocalRandom.current().nextInt(1, 12), ThreadLocalRandom.current().nextInt(24, 29)));

        // every order has the defined outcome
        order.setOrderStatus(orderStatus);
        order.setInvalidOrderReasonCode(reasonCodeForFailure);

        // get a random restaurant
        var currentRestaurant = restaurants[restaurantIndex++];
        if (restaurantIndex >= restaurants.length){
            restaurantIndex = 0;
        }

        // and load the order items plus the price
        order.setPizzasInOrder(currentRestaurant.menu());
        order.setPriceTotalInPence(Arrays.stream(currentRestaurant.menu()).map(m -> m.priceInPence()).reduce(0, Integer::sum) + SystemConstants.ORDER_CHARGE_IN_PENCE);

        return order;
    }

    // private Map<OrderStatus, InvalidOrderReasonCode>

    public static void main(String[] args) throws IOException {
        System.out.println("ILP sample order data generator");

        // needed for the Pizzas and prices
        var restaurants = new IlpRestService().restaurants();


        // we use a smaller period than 2 years as the result gets huge
        var startDate = LocalDate.of(2023, 1, 1);

        // needed to create mock data
        MockNeat mock = MockNeat.threadLocal();

        // where our result will be returned
        List<Order> orderList = new ArrayList<>();

        // iterate over each date in the range
        var currentDate = startDate;
        while (currentDate.isBefore(LocalDate.of(2023, 5, 31))) {


            // traverse over all possible order outcomes to generate one record for each outcome possible
            for (var outcome : OrderStatus.values()) {

                // create a valid order with default values
                var order = CreateSampleOrder(currentDate, outcome, InvalidOrderReasonCode.NO_ERROR, mock, restaurants);

                // now modify the wrong entries according to the enum
                switch (outcome) {
                    case INVALID -> {
                        for (var reasonCode : InvalidOrderReasonCode.values()) {
                            switch (reasonCode) {
                                case CVV -> {
                                    var len = Integer.toString(ThreadLocalRandom.current().nextInt(1, 8));

                                    // 3 digit CVV would be valid
                                    if (len.equals("3")) {
                                        len = "4";
                                    }

                                    String formatString = "%" + len + "." + len + "s";
                                    order.setCvv(String.format(formatString, ThreadLocalRandom.current().nextInt(0, 99999999)));
                                }
                                case CARD_NUMBER -> {
                                    var len = ThreadLocalRandom.current().nextInt(1, 16);
                                    String formatString = "%" + len + "." + len + "s";
                                    order.setCreditCardNumber(String.format(formatString, ThreadLocalRandom.current().nextLong(1, 9999999999999999L)));
                                }
                                case TOTAL -> {
                                    // by adding something to the order this invalidates the total -> just make sure it is not 0 (which would be correct again)
                                    var add = ThreadLocalRandom.current().nextInt(-100, 1000);
                                    if (add == 0) {
                                        add = 1;
                                    }
                                    order.setPriceTotalInPence(order.getPriceTotalInPence() + add);
                                }

                                case EXPIRY_DATE -> {
                                    // expiry date is always in the past
                                    order.setCreditCardExpiry(String.format("%02d/%02d", ThreadLocalRandom.current().nextInt(1, 20), ThreadLocalRandom.current().nextInt(2, 19)));
                                }

                                case PIZZA_NOT_DEFINED -> {
                                    // add a pizza which does not exist
                                    var currentPizzas = getPizzasInOrderAsMutableList(order);
                                    currentPizzas.add(new Pizza("Pizza-Surprise ", new Random(10000).nextInt()));
                                    order.setPizzasInOrder(currentPizzas.toArray(new Pizza[0]));
                                }

                                case MAX_PIZZA_COUNT_EXCEEDED -> {
                                    // just multiply the ordered pizzas (5 is always more than the valid maximum)
                                    var currentPizzas =  getPizzasInOrderAsMutableList(order);
                                    currentPizzas.add(new Pizza("Pizza-Surprise ", new Random(10000).nextInt()));
                                    currentPizzas.add(new Pizza("Pizza Extra2 ", new Random(10000).nextInt()));
                                    currentPizzas.add(new Pizza("Pizza Extra3 ", new Random(10000).nextInt()));
                                    currentPizzas.add(new Pizza("Pizza Extra4 ", new Random(10000).nextInt()));
                                    order.setPizzasInOrder(currentPizzas.toArray(new Pizza[0]));
                                    order.setPriceTotalInPence(currentPizzas.stream().map(m -> m.priceInPence()).reduce(0, Integer::sum) + SystemConstants.ORDER_CHARGE_IN_PENCE);
                                }

                                case MULTIPLE_RESTAURANTS -> {
                                    // mix pizzas from a different supplier. Find the restaurant the order is from, then take the next
                                    var currentRestaurant = Arrays.stream(restaurants).filter(r -> r.menu()[0].name().equals(order.getPizzasInOrder()[0].name())).findFirst().get();
                                    if (restaurants[0].equals(currentRestaurant)) {
                                        currentRestaurant = restaurants[1];
                                    } else {
                                        currentRestaurant = restaurants[0];
                                    }

                                    var currentPizzas =  getPizzasInOrderAsMutableList(order);
                                    var pizzaToAdd = currentRestaurant.menu()[0];
                                    currentPizzas.add(new Pizza(pizzaToAdd.name(), pizzaToAdd.priceInPence()));
                                    order.setPizzasInOrder(currentPizzas.toArray(new Pizza[0]));
                                    order.setPriceTotalInPence(currentPizzas.stream().map(m -> m.priceInPence()).reduce(0, Integer::sum) + SystemConstants.ORDER_CHARGE_IN_PENCE);
                                }

                                case RESTAURANT_CLOSED -> {
                                    var currentDayOfWeek = currentDate.getDayOfWeek();
                                    Restaurant currentRestaurant = null;
                                    for (var restaurant: restaurants) {
                                        if (Arrays.stream(restaurant.openingDays()).noneMatch(d -> d.equals(currentDayOfWeek))) {
                                            currentRestaurant = restaurant;
                                            break;
                                        }
                                    }

                                    if (currentRestaurant == null) {
                                        throw new RuntimeException("No restaurant found which is not open on: " + currentDayOfWeek.toString());
                                    }


                                    order.setPizzasInOrder(new Pizza[] { currentRestaurant.menu()[0] });
                                    order.setPriceTotalInPence(Arrays.stream(order.getPizzasInOrder()).map(m -> m.priceInPence()).reduce(0, Integer::sum) + SystemConstants.ORDER_CHARGE_IN_PENCE);
                                }

                                default -> {
                                    // Uups...
                                }
                            }
                        }
                    }


                 /**
                  * Will be handled later
                  case ValidButNotDelivered -> {
                        order.orderOutcome = OrderOutcome.Delivered;
                    }

                    case Delivered -> {
                        ;
                    }
                */
                    default -> {
                        continue;
                    }
                }

                orderList.add(order);
            }

            // find another date
            currentDate = currentDate.plusDays(1);
        }

        currentDate = startDate;
        while (currentDate.isBefore(LocalDate.of(2023, 5, 31))) {
            for (int orderCount = 0; orderCount < 30; orderCount ++){
                // create a valid order with default values
                orderList.add(CreateSampleOrder(currentDate, OrderStatus.DELIVERED, InvalidOrderReasonCode.NO_ERROR, mock, restaurants));
            }

            // find another date
            currentDate = currentDate.plusDays(1);
        }

        var writer = new BufferedWriter(new FileWriter("orders.json"));

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateSerializer());
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateDeserializer());
        Gson gson = gsonBuilder.setPrettyPrinting().create();
        writer.write(gson.toJson(orderList.toArray()));
        writer.flush();
    }

    /**
     * get all pizzas in an order as a mutable list. This is a shortcut function
     * @return mutable list of all pizzas
     */
    private static List<Pizza> getPizzasInOrderAsMutableList(Order order) {
        return Arrays.stream(order.getPizzasInOrder()).collect(Collectors.toList());
    }
}
