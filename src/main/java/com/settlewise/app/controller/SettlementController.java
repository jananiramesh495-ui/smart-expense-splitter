package com.settlewise.app.controller;

import com.settlewise.app.service.SettlementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping("/group/{groupId}")
    public List<SettlementService.Transaction> getSettlement(@PathVariable Long groupId) {
        return settlementService.calculateSettlement(groupId);
    }
    @GetMapping("/group/{groupId}/balances")
    public Map<String, Double> getBalances(@PathVariable Long groupId) {
        return settlementService.getNetBalances(groupId);
    }
}
