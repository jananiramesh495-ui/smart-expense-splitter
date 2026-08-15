package com.settlewise.app.service;

import com.settlewise.app.model.Expense;
import com.settlewise.app.model.User;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SettlementService {

    private final ExpenseService expenseService;
    private final GroupService groupService;

    public SettlementService(ExpenseService expenseService, GroupService groupService) {
        this.expenseService = expenseService;
        this.groupService = groupService;
    }

    // Simple class to hold one settlement instruction
    public static class Transaction {
        public String from;
        public String to;
        public double amount;

        public Transaction(String from, String to, double amount) {
            this.from = from;
            this.to = to;
            this.amount = amount;
        }
    }

    // Simple class to hold a person's current balance (used inside the heap)
    private static class Balance {
        String personName;
        double amount;

        Balance(String personName, double amount) {
            this.personName = personName;
            this.amount = amount;
        }
    }

    public List<Transaction> calculateSettlement(Long groupId) {

        // Step 1: Get all members of the group
        List<User> members = groupService.getGroupById(groupId).getMembers();

        // Step 2: Get all expenses for this group
        List<Expense> expenses = expenseService.getExpensesByGroup(groupId);

        // Step 3: Calculate net balance for each member
        Map<String, Double> netBalances = new HashMap<>();
        for (User member : members) {
            netBalances.put(member.getName(), 0.0);
        }

        int memberCount = members.size();

        for (Expense expense : expenses) {
            double amount = expense.getAmount();
            double share = amount / memberCount;

            // Use merge() instead of get()+put() so a payer who isn't
            // officially in "members" (e.g. added after the fact, or a data
            // mismatch) doesn't cause a NullPointerException.
            String payerName = expense.getPaidBy().getName();
            netBalances.merge(payerName, amount, Double::sum);

            for (User member : members) {
                String name = member.getName();
                netBalances.merge(name, -share, Double::sum);
            }
        }

        // Step 4: Separate into creditors (max-heap) and debtors (max-heap by absolute value)
        PriorityQueue<Balance> creditors = new PriorityQueue<>((a, b) -> Double.compare(b.amount, a.amount));
        PriorityQueue<Balance> debtors = new PriorityQueue<>((a, b) -> Double.compare(b.amount, a.amount));

        for (Map.Entry<String, Double> entry : netBalances.entrySet()) {
            double balance = entry.getValue();

            if (balance > 0.01) {
                creditors.add(new Balance(entry.getKey(), balance));
            } else if (balance < -0.01) {
                debtors.add(new Balance(entry.getKey(), -balance)); // store as positive for easy comparison
            }
        }

        // Step 5: Greedy settlement using the two heaps
        List<Transaction> transactions = new ArrayList<>();

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Balance creditor = creditors.poll();
            Balance debtor = debtors.poll();

            double settledAmount = Math.min(creditor.amount, debtor.amount);

            transactions.add(new Transaction(debtor.personName, creditor.personName, settledAmount));

            creditor.amount -= settledAmount;
            debtor.amount -= settledAmount;

            if (creditor.amount > 0.01) {
                creditors.add(creditor);
            }
            if (debtor.amount > 0.01) {
                debtors.add(debtor);
            }
        }

        return transactions;
    }

    public Map<String, Double> getNetBalances(Long groupId) {

        List<User> members = groupService.getGroupById(groupId).getMembers();
        List<Expense> expenses = expenseService.getExpensesByGroup(groupId);

        Map<String, Double> netBalances = new HashMap<>();
        for (User member : members) {
            netBalances.put(member.getName(), 0.0);
        }

        int memberCount = members.size();

        for (Expense expense : expenses) {
            double amount = expense.getAmount();
            double share = amount / memberCount;

            String payerName = expense.getPaidBy().getName();
            netBalances.merge(payerName, amount, Double::sum);

            for (User member : members) {
                String name = member.getName();
                netBalances.merge(name, -share, Double::sum);
            }
        }

        return netBalances;
    }
}