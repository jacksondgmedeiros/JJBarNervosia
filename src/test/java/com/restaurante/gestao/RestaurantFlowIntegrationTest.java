package com.restaurante.gestao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RestaurantFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateOrdersAndAggregateAtCashier() throws Exception {
        String order1 = """
                {"tableNumber":10,"itemName":"Pizza","quantity":1,"unitPrice":50.00,"notes":"metade sem queijo"}
                """;

        String order2 = """
                {"tableNumber":10,"itemName":"Refrigerante","quantity":2,"unitPrice":8.00,"notes":""}
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(order1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tableNumber").value(10));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(order2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemName").value("Refrigerante"));

        mockMvc.perform(get("/api/kitchen/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kitchenStatus").value("PENDING"));

        mockMvc.perform(patch("/api/kitchen/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kitchenStatus").value("DONE"));

        mockMvc.perform(get("/api/cashier/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tableNumber").value(10))
                .andExpect(jsonPath("$[0].items.length()").value(2))
                .andExpect(jsonPath("$[0].total").value(66.00));
    }
}
