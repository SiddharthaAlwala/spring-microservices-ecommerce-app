package com.microservices.order_service.dto;

import com.microservices.order_service.model.OrderLineItems;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class OrderRequest {
    private List<OrderLineItemsDto> orderLineItemsDtoList;
}
