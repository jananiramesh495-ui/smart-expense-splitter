package com.settlewise.app.dto;

import com.settlewise.app.model.Expense;
import com.settlewise.app.service.ExpenseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public Expense addExpense(@RequestBody ExpenseRequest request) {
        return expenseService.addExpense(
                request.getAmount(),
                request.getCategory(),
                request.getPaidByUserId(),
                request.getGroupId()
        );
    }

    @GetMapping("/group/{groupId}")
    public List<Expense> getExpensesByGroup(@PathVariable Long groupId) {
        return expenseService.getExpensesByGroup(groupId);
    }

    // Small helper class to receive the JSON body for creating an expense
    public static class ExpenseRequest {
        private Double amount;
        private String category;
        private Long paidByUserId;
        private Long groupId;

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public Long getPaidByUserId() {
            return paidByUserId;
        }

        public void setPaidByUserId(Long paidByUserId) {
            this.paidByUserId = paidByUserId;
        }

        public Long getGroupId() {
            return groupId;
        }

        public void setGroupId(Long groupId) {
            this.groupId = groupId;
        }
    }
}