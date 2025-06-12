package com.microservices.order_service.service;

import com.microservices.order_service.dto.InventoryResponse;
import com.microservices.order_service.dto.OrderLineItemsDto;
import com.microservices.order_service.dto.OrderRequest;
import com.microservices.order_service.model.Order;
import com.microservices.order_service.model.OrderLineItems;
import com.microservices.order_service.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItemsList(orderLineItems);
        List<String> skuCodes= order.getOrderLineItemsList().stream().map(OrderLineItems::getSkuCode)
                .toList();

        System.out.println("🧪 Calling inventory-service via Eureka...");
        System.out.println("WebClient builder class: " + webClientBuilder.getClass().getName());

        // Call Inventory service, and place order if product is in.
        // By default webClient will make asynchronous request.
        InventoryResponse[] inventoryResponseArray  = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCodes", skuCodes.toArray()).build()
                        )
                        .retrieve()
                        .bodyToMono(InventoryResponse[].class)
                        .block();

//        URI uri = UriComponentsBuilder.fromHttpUrl("http://inventory-service/api/inventory")
//                .queryParam("skuCodes", skuCodes)
//                .build()
//                .toUri();


//        System.out.println("CALLING: " + uri);  // Debug log

//        InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
//                .uri(uri)
//                .retrieve()
//                .bodyToMono(InventoryResponse[].class)
//                .block();

        boolean allProductsInStock = Arrays.stream(inventoryResponseArray).allMatch(InventoryResponse::isInStock);

        if(allProductsInStock) {
            orderRepository.save(order);
        }
        else{
            throw new IllegalArgumentException("Product is not in stock, try  again alter");
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
