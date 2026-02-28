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
                {"tableNumber":10,"customerName":"Cliente Teste","productId":1,"quantity":1,"notes":"sem gelo"}
                """;

        String order2 = """
                {"tableNumber":10,"customerName":"Cliente Teste","productId":2,"quantity":2,"notes":""}
                """;

        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(order1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tableNumber").value(10));

        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(order2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/kitchen/orders")
                        .header("X-User-Id", 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(patch("/api/waiter/sessions/1/finalize")
                        .header("X-User-Id", 2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Existem itens ainda não enviados para cozinha"));

        mockMvc.perform(patch("/api/waiter/sessions/1/send-to-kitchen")
                        .header("X-User-Id", 2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/kitchen/orders")
                        .header("X-User-Id", 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tableNumber").value(10))
                .andExpect(jsonPath("$[0].items.length()").value(2))
                .andExpect(jsonPath("$[0].items[0].kitchenStatus").value("PENDING"));

        mockMvc.perform(patch("/api/waiter/sessions/1/finalize")
                        .header("X-User-Id", 2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cashier/sessions")
                        .header("X-User-Id", 4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tableNumber").value(10))
                .andExpect(jsonPath("$[0].customerName").value("Cliente Teste"))
                .andExpect(jsonPath("$[0].items.length()").value(2));
    }
}
