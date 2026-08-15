package com.settlewise.app.service;

import com.settlewise.app.model.Expense;
import com.settlewise.app.model.Group;
import com.settlewise.app.model.User;
import com.settlewise.app.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserService userService;
    private final GroupService groupService;

    public ExpenseService(ExpenseRepository expenseRepository, UserService userService, GroupService groupService) {
        this.expenseRepository = expenseRepository;
        this.userService = userService;
        this.groupService = groupService;
    }

    public Expense addExpense(Double amount, Long paidByUserId, Long groupId) {
        User paidBy = userService.getUserById(paidByUserId);
        Group group = groupService.getGroupById(groupId);

        Expense expense = new Expense(amount, paidBy, group);
        return expenseRepository.save(expense);
    }

    public List<Expense> getExpensesByGroup(Long groupId) {
        return expenseRepository.findByGroupId(groupId);
    }
}