package uk.ac.ed.inf.ilpRestServer.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.data.TestItem;
import uk.ac.ed.inf.ilp.gsonUtils.LocalDateDeserializer;
import uk.ac.ed.inf.ilp.gsonUtils.LocalDateSerializer;

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
    private Order[] getOrders(){
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateSerializer());
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateDeserializer());
        Gson gson = gsonBuilder.setPrettyPrinting().create();
        return gson.fromJson(new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("json/orders.json")))), Order[].class);
    }

    /**
     * returns sample orders (some of them invalid) from a template JSON file. The order outcome is removed and UNDEFINED preset
     *
     * @param orderDate optional date in the format YYYY-MM-DD to find orders matching just the date
     * @return an array of orders
     */
    @GetMapping(value = {"/orders/{orderDate}", "/orders"})
    public Order[] orders(@PathVariable(required = false) String orderDate) {
        List<Order> result = new ArrayList<>();

        var orders = getOrders();
        if (orderDate != null){
            result.addAll(Arrays.stream(orders).filter(o -> o.getOrderDate().equals(orderDate)).toList());
        } else {
            result.addAll(Arrays.stream(orders).toList());
        }

        result.forEach(o -> o.setOrderStatus(OrderStatus.UNDEFINED));
        return result.toArray(new Order[0]);
    }


    /**
     * returns sample orders (some of them invalid) from a template JSON file without removal of information
     *
     * @param orderDate optional date in the format YYYY-MM-DD to find orders matching just the date
     * @return an array of orders
     */
    @GetMapping(value = {"/ordersWithOutcome/{orderDate}", "/ordersWithOutcome"})
    public Order[] ordersWithOutcome(@PathVariable(required = false) String orderDate) {
        List<Order> result;

        var orders = getOrders();
        if (orderDate != null){
            var compDate = LocalDate.parse(orderDate);
            result = new ArrayList<>(Arrays.stream(orders).filter(o -> o.getOrderDate().equals(compDate)).toList());
        } else {
            result = new ArrayList<>(List.of(orders));
        }
        return result.toArray(new Order[0]);
    }



    /**
     * get the details for an order
     * @param orderNo the order to search
     * @return the order details or HTTP 404 if not found
     */
    @GetMapping("/orders/{orderNo}/details")
    public Order orderDetails(@PathVariable String orderNo){
        var currentOrder = Arrays.stream(getOrders()).filter(o -> o.getOrderNo().equals(orderNo)).findFirst();
        if (currentOrder.isPresent() == false){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found");
        }
        return currentOrder.get();
    }


    /**
     * check if the order outcome provided matches the JSON definition
     * @param orderNo is the order no to be checked. If no or an invalid number is passed false is returned
     * @param statusToCheck the status which should be checked
     * @return true if the outcome is according to the JSON definition, otherwise (or if the order was not found) then not
     */
    @GetMapping("/orders/{orderNo}/isOrderOutcomeValid/{statusToCheck}")
    public Boolean isOrderOutcomeValid(@PathVariable String orderNo, @PathVariable OrderStatus statusToCheck){
        var currentOrder = Arrays.stream(getOrders()).filter(o -> o.getOrderNo().equals(orderNo)).findFirst();
        return currentOrder.filter(orderWithOutcome -> orderWithOutcome.getOrderStatus() == statusToCheck).isPresent();
    }

    /**
     * get the order outcome for an order
     * @param orderNo the order to search
     * @return the order outcome or HTTP 404 if not found
     */
    @GetMapping("/orders/{orderNo}/status")
    public OrderStatus orderOutcome(@PathVariable String orderNo){
        var currentOrder = Arrays.stream(getOrders()).filter(o -> o.getOrderNo().equals(orderNo)).findFirst();
        if (currentOrder.isPresent() == false){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found");
        }
        return currentOrder.get().getOrderStatus();
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
     * get the central area as a named region
     * @return the named region for the central area
     */
    @GetMapping(value = {"/centralArea", "/centralarea"})
    public NamedRegion centralArea() {
        return new Gson().fromJson(new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("json/centralarea.json")))), NamedRegion.class);
    }

    /**
     * get the defined no-fly-zones as named regions
     * @return a vector of named regions
     */
    @GetMapping(value = {"/noFlyZones", "/noflyzones"})
    public NamedRegion[] noFlyZones() {
        return new Gson().fromJson(new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("json/noflyzones.json")))), NamedRegion[].class);

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
