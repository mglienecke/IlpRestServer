package uk.ac.ed.inf.ilpRestServer.controller;

import com.google.gson.Gson;
import org.springframework.cglib.core.Local;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.ac.ed.inf.ilp.data.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * the global ILP service which provides suppliers, orders and other useful things
 */
@RestController
public class IlpRestService {

    /**
     * load the orders from the JSON file and return the deserialized version
     * @return an array of orders from the file
     */
    private OrderWithOutcome[] getOrders(){
        return new Gson().fromJson(new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("json/orders.json")))), OrderWithOutcome[].class);
    }

    /**
     * returns sample orders (some of them invalid) from a template JSON file. The order outcome is removed
     *
     * @param orderDate optional date in the format YYYY-MM-DD to find orders matching just the date
     * @return an array of orders
     */
    @GetMapping(value = {"/orders/{orderDate}", "/orders"})
    public Order[] orders(@PathVariable(required = false) String orderDate) {
        List<Order> result = new ArrayList<>();

        var orders = getOrders();
        if (orderDate != null){
            result.addAll(Arrays.stream(orders).filter(o -> o.orderDate.equals(orderDate)).map(o -> new Order(o.orderNo, o.orderDate, o.customer, o.creditCardNumber, o.creditCardExpiry, o.cvv, o.priceTotalInPence, o.orderItems.clone())).toList());
        } else {
            for (var o  : orders){
                result.add(new Order(o.orderNo, o.orderDate, o.customer, o.creditCardNumber, o.creditCardExpiry, o.cvv, o.priceTotalInPence, o.orderItems.clone()));
            }
        }
        return result.toArray(new Order[0]);
    }


    /**
     * returns sample orders (some of them invalid) from a template JSON file without removal of information
     *
     * @param orderDate optional date in the format YYYY-MM-DD to find orders matching just the date
     * @return an array of orders
     */
    @GetMapping(value = {"/ordersWithOutcome/{orderDate}", "/ordersWithOutcome"})
    public OrderWithOutcome[] ordersWithOutcome(@PathVariable(required = false) String orderDate) {
        List<OrderWithOutcome> result;

        var orders = getOrders();
        if (orderDate != null){
            result = new ArrayList<>(Arrays.stream(orders).filter(o -> o.orderDate.equals(orderDate)).toList());
        } else {
            result = new ArrayList<>(List.of(orders));
        }
        return result.toArray(new OrderWithOutcome[0]);
    }



    /**
     * get the details for an order
     * @param orderNo the order to search
     * @return the order details or HTTP 404 if not found
     */
    @GetMapping("/orders/{orderNo}/details")
    public OrderWithOutcome orderDetails(@PathVariable String orderNo){
        var currentOrder = Arrays.stream(getOrders()).filter(o -> o.orderNo.equals(orderNo)).findFirst();
        if (currentOrder.isPresent() == false){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found");
        }
        return currentOrder.get();
    }


    /**
     * check if the order outcome provided matches the JSON definition
     * @param orderNo is the order no to be checked. If no or an invalid number is passed false is returned
     * @param outcomeToCheck the outcome which should be checked
     * @return true if the outcome is according to the JSON definition, otherwise (or if the order was not found) then not
     */
    @GetMapping("/orders/{orderNo}/isOrderOutcomeValid/{outcomeToCheck}")
    public Boolean isOrderOutcomeValid(@PathVariable String orderNo, @PathVariable OrderOutcome outcomeToCheck){
        var currentOrder = Arrays.stream(getOrders()).filter(o -> o.orderNo.equals(orderNo)).findFirst();
        return currentOrder.filter(orderWithOutcome -> orderWithOutcome.orderOutcome == outcomeToCheck).isPresent();
    }

    /**
     * get the order outcome for an order
     * @param orderNo the order to search
     * @return the order outcome or HTTP 404 if not found
     */
    @GetMapping("/orders/{orderNo}/outcome")
    public OrderOutcome orderOutcome(@PathVariable String orderNo){
        var currentOrder = Arrays.stream(getOrders()).filter(o -> o.orderNo.equals(orderNo)).findFirst();
        if (currentOrder.isPresent() == false){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found");
        }
        return currentOrder.get().orderOutcome;
    }


    /**
     * returns the restaurants in the system
     *
     * @return array of suppliers
     */
    @GetMapping("/restaurants")
    public Restaurant[] restaurants() {
        return new Gson().fromJson(new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("json/restaurants.json")))), Restaurant[].class);
    }


    /**
     * get the defined boundaries in the system
     * @return a vector of boundaries
     */
    @GetMapping(value = {"/centralArea", "/centralarea"})
    public Boundary[] centralArea() {
        return new Gson().fromJson(new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("json/centralarea.json")))), Boundary[].class);

    }

    /**
     * get the defined boundaries in the system
     * @return a vector of boundaries
     */
    @GetMapping(value = {"/noFlyZones", "/noflyzones"})
    public NoFlyZone[] noFlyZones() {
        return new Gson().fromJson(new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("json/noflyzones.json")))), NoFlyZone[].class);

    }

    /**
     * simple test method to test the service's availability
     * @param input an optional input which will be echoed
     * @return the echo
     */
    @GetMapping(value = {"/test/{input}", "/test"})
    public TestItem test(@PathVariable(required = false) String input) {
        return new TestItem(String.format("Hello from the ILP-REST-Service. Your provided value was: %s", input == null ? "not provided" : input));
    }


    /**
     * a simple alive check
     * @return true (always)
     */
    @GetMapping(value = {"/isAlive"})
    public boolean isAlive() {
        return true;
    }
}
