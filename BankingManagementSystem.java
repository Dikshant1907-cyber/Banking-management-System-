import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder; // Needed for TitledBorder color change
import javax.swing.table.DefaultTableModel;
import java.awt.Color;
import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.CardLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// ======================================================
// ✅ MAIN BANKING SYSTEM CLASS
// ======================================================
public class BankingManagementSystem extends JFrame {

    private final Color BG_COLOR = new Color(30, 33, 35);
    private final Color PANEL_COLOR = new Color(45, 48, 50);
    private final Color ACCENT_COLOR = new Color(0, 153, 255);
    private final Color TEXT_COLOR = Color.WHITE;

    private JTabbedPane tabbedPane;
    private JPanel dashboardPanel;

    private List<Customer> customers = new ArrayList<>();
    private List<Account> accounts = new ArrayList<>();
    private List<Transaction> transactions = new ArrayList<>();

    // Auto-increment counters
    private int nextCustomerId = 1001;
    private int nextAccountId = 5001;
    private int nextTransactionId = 10001;

    private static final String CUSTOMERS_FILE = "customers.csv";
    private static final String ACCOUNTS_FILE = "accounts.csv";
    private static final String TRANSACTIONS_FILE = "transactions.csv";
    private final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DefaultTableModel customerTableModel;
    private DefaultTableModel accountTableModel;

    private static BankingManagementSystem instance;

    public BankingManagementSystem(String username) {
        if (instance != null) instance.dispose();
        instance = this;

        setTitle("🏦 Banking Management System - Welcome " + username);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_COLOR);

        loadData();
        recalculateNextIds();

        initializeGUI(username);
        setVisible(true);
    }

    // -------------------- GUI Initialization --------------------
    private void initializeGUI(String username) {

        // --- Header Panel with Title and Logout Button ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PANEL_COLOR);
        headerPanel.setBorder(new EmptyBorder(5, 10, 5, 10));

        JLabel appTitle = new JLabel("BANKING MANAGEMENT SYSTEM", SwingConstants.LEFT);
        appTitle.setForeground(ACCENT_COLOR);
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerPanel.add(appTitle, BorderLayout.WEST);

        JButton logoutButton = createStyledButton("Logout (" + username + ")", new Color(231, 76, 60)); // Red for logout
        logoutButton.addActionListener(e -> handleLogout());
        headerPanel.add(logoutButton, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // --- Tabbed Pane ---
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setBackground(PANEL_COLOR);
        tabbedPane.setForeground(TEXT_COLOR);

        dashboardPanel = createDashboardPanel(username);
        JPanel customerPanel = createCustomerPanel();
        JPanel accountPanel = createAccountPanel();
        JPanel transactionPanel = createTransactionPanel();

        tabbedPane.addTab("🏠 Dashboard", dashboardPanel);
        tabbedPane.addTab("👥 Customers", customerPanel);
        tabbedPane.addTab("💳 Accounts", accountPanel);
        tabbedPane.addTab("💰 Transactions", transactionPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void handleLogout() {
        int response = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out?", "Confirm Logout",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (response == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(LoginFrame::new);
        }
    }

    // --- Dashboard Panel ---
    private JPanel createDashboardPanel(String username) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);

        JLabel title = new JLabel("Welcome, " + username + "!", SwingConstants.CENTER);
        title.setForeground(ACCENT_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setBorder(new EmptyBorder(20, 0, 20, 0));
        panel.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 3, 15, 15));
        grid.setBackground(BG_COLOR);
        grid.setBorder(new EmptyBorder(20, 20, 20, 20));

        long activeAccounts = accounts.stream().filter(a -> "Active".equals(a.status)).count();
        double totalBalance = accounts.stream().mapToDouble(a -> a.balance).sum();
        OptionalDouble avgBalanceOpt = accounts.stream().mapToDouble(a -> a.balance).average();
        double avgBalance = avgBalanceOpt.isPresent() ? avgBalanceOpt.getAsDouble() : 0;

        grid.add(createStatBox("Total Customers", String.valueOf(customers.size())));
        grid.add(createStatBox("Total Accounts", String.valueOf(accounts.size())));
        grid.add(createStatBox("Active Accounts", String.valueOf(activeAccounts)));
        grid.add(createStatBox("Total Transactions", String.valueOf(transactions.size())));
        grid.add(createStatBox("Total Balance", String.format("₹%.2f", totalBalance)));
        grid.add(createStatBox("Average Balance", String.format("₹%.2f", avgBalance)));

        panel.add(grid, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatBox(String label, String value) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(PANEL_COLOR);
        box.setBorder(BorderFactory.createLineBorder(ACCENT_COLOR, 2));

        JLabel v = new JLabel(value, SwingConstants.CENTER);
        v.setFont(new Font("Segoe UI", Font.BOLD, 30));
        v.setForeground(Color.WHITE);

        JLabel l = new JLabel(label, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        l.setForeground(Color.LIGHT_GRAY);
        l.setBorder(new EmptyBorder(0, 0, 5, 0));

        box.add(v, BorderLayout.CENTER);
        box.add(l, BorderLayout.SOUTH);
        return box;
    }

    // ======================================================
    // ✅ CUSTOMER MANAGEMENT TAB
    // ======================================================
    private JPanel createCustomerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);

        JLabel title = new JLabel("Customer Management", SwingConstants.CENTER);
        title.setForeground(ACCENT_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setBorder(new EmptyBorder(15, 0, 15, 0));
        panel.add(title, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Name", "Email", "Phone", "Address"};
        customerTableModel = new DefaultTableModel(cols, 0);
        JTable table = createStyledTable(customerTableModel);

        loadCustomersIntoTable(customerTableModel);
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(PANEL_COLOR);
        panel.add(scroll, BorderLayout.CENTER);

        // Input fields and Form
        JPanel form = new JPanel(new GridLayout(1, 5, 10, 10));
        form.setBackground(BG_COLOR);
        form.setBorder(new EmptyBorder(15, 20, 5, 20));

        JTextField nameField = createStyledTextField("Name");
        JTextField emailField = createStyledTextField("Email");
        JTextField phoneField = createStyledTextField("Phone");
        JTextField addressField = createStyledTextField("Address");

        JLabel nextIdLabel = new JLabel("Next ID: " + nextCustomerId);
        nextIdLabel.setForeground(TEXT_COLOR);
        nextIdLabel.setBorder(BorderFactory.createEmptyBorder(25, 0, 0, 0));

        form.add(nextIdLabel);
        form.add(nameField);
        form.add(emailField);
        form.add(phoneField);
        form.add(addressField);

        JButton addBtn = createStyledButton("Add Customer", ACCENT_COLOR);

        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String address = addressField.getText().trim();

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Customer c = new Customer(nextCustomerId++, name, email, phone, address);
            customers.add(c);
            saveCustomers();
            loadCustomersIntoTable(customerTableModel);
            refreshDashboard();

            nameField.setText("");
            emailField.setText("");
            phoneField.setText("");
            addressField.setText("");
            nextIdLabel.setText("Next ID: " + nextCustomerId);

            JOptionPane.showMessageDialog(this, "✅ Customer added successfully! ID: " + c.id, "Success", JOptionPane.INFORMATION_MESSAGE);
        });

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setBackground(BG_COLOR);
        southPanel.add(form, BorderLayout.NORTH);
        southPanel.add(addBtn, BorderLayout.SOUTH);
        southPanel.setBorder(new EmptyBorder(0, 20, 20, 20));

        panel.add(southPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ======================================================
    // ✅ ACCOUNT MANAGEMENT TAB
    // ======================================================
    private JPanel createAccountPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Account Management", SwingConstants.CENTER);
        title.setForeground(ACCENT_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setBorder(new EmptyBorder(15, 0, 15, 0));
        panel.add(title, BorderLayout.NORTH);

        // --- Table ---
        String[] cols = {"Account ID", "Customer ID", "Type", "Balance", "Status", "Created Date"};
        accountTableModel = new DefaultTableModel(cols, 0);
        JTable table = createStyledTable(accountTableModel);
        loadAccountsIntoTable(accountTableModel);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(PANEL_COLOR);
        panel.add(scroll, BorderLayout.CENTER);

        // --- Form/Action Panel ---
        JPanel southPanel = new JPanel(new BorderLayout(10, 10));
        southPanel.setBackground(BG_COLOR);
        southPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        // Form for creation
        JPanel form = new JPanel(new GridLayout(2, 3, 10, 10));
        form.setBackground(BG_COLOR);

        JTextField customerIdField = createStyledTextField("Customer ID");
        JComboBox<String> accountTypeBox = new JComboBox<>(new String[]{"Savings", "Current"});
        accountTypeBox.setBackground(PANEL_COLOR);
        accountTypeBox.setForeground(TEXT_COLOR);
        // Changed TitledBorder color to white for visibility
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), "Account Type", 
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.PLAIN, 12), Color.WHITE);
        accountTypeBox.setBorder(titledBorder);


        JTextField initialDepositField = createStyledTextField("Initial Deposit (₹)");

        JButton createButton = createStyledButton("Create Account", ACCENT_COLOR);
        JButton viewButton = createStyledButton("View Details", new Color(46, 204, 113));

        JLabel nextIdLabel = new JLabel("Next Acc ID: " + nextAccountId);
        nextIdLabel.setForeground(TEXT_COLOR);
        nextIdLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        form.add(nextIdLabel);
        form.add(customerIdField);
        form.add(accountTypeBox);
        form.add(initialDepositField);
        form.add(createButton);
        form.add(viewButton);

        southPanel.add(form, BorderLayout.CENTER);
        panel.add(southPanel, BorderLayout.SOUTH);

        // --- Actions ---
        createButton.addActionListener(e -> handleAccountCreation(customerIdField.getText(),
                                                                (String) accountTypeBox.getSelectedItem(),
                                                                initialDepositField.getText(),
                                                                customerIdField, initialDepositField));

        viewButton.addActionListener(e -> handleViewAccountDetails(table));

        return panel;
    }

    // ======================================================
    // ✅ TRANSACTION MANAGEMENT TAB
    // ======================================================
    private JPanel createTransactionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG_COLOR);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- Control Panel: Dropdown for selecting transaction type ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        controlPanel.setBackground(PANEL_COLOR);
        controlPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel typeLabel = new JLabel("Select Operation:");
        typeLabel.setForeground(TEXT_COLOR);

        JComboBox<String> transactionTypeBox = new JComboBox<>(
                new String[]{"Deposit", "Withdrawal", "Transfer", "Check Balance", "Transaction History"});
        transactionTypeBox.setBackground(ACCENT_COLOR);
        transactionTypeBox.setForeground(Color.WHITE);

        controlPanel.add(typeLabel);
        controlPanel.add(transactionTypeBox);
        panel.add(controlPanel, BorderLayout.NORTH);

        // --- Card Layout for Different Transaction Forms ---
        JPanel cardPanel = new JPanel(new CardLayout());
        cardPanel.setBackground(BG_COLOR);

        cardPanel.add(createDepositPanel(), "Deposit");
        cardPanel.add(createWithdrawalPanel(), "Withdrawal");
        cardPanel.add(createTransferPanel(), "Transfer");
        cardPanel.add(createBalancePanel(), "Check Balance");
        cardPanel.add(createHistoryPanel(), "Transaction History");

        panel.add(cardPanel, BorderLayout.CENTER);

        // Change card on selection
        transactionTypeBox.addActionListener(e -> {
            CardLayout cl = (CardLayout) cardPanel.getLayout();
            cl.show(cardPanel, (String) transactionTypeBox.getSelectedItem());
        });

        return panel;
    }

    private JPanel createDepositPanel() {
        JPanel panel = createTransactionFormPanel();
        JTextField accountIdField = createStyledTextField("Account ID");
        JTextField amountField = createStyledTextField("Amount (₹)");
        JButton depositButton = createStyledButton("Deposit Funds", ACCENT_COLOR);

        addGBCComponent(panel, new JLabel("Account ID:"), 0, 0, 1);
        addGBCComponent(panel, accountIdField, 1, 0, 2);
        addGBCComponent(panel, new JLabel("Amount (₹):"), 0, 1, 1);
        addGBCComponent(panel, amountField, 1, 1, 2);
        addGBCComponent(panel, depositButton, 1, 2, 2);

        depositButton.addActionListener(e -> handleDeposit(accountIdField.getText(), amountField.getText()));
        return panel;
    }

    private JPanel createWithdrawalPanel() {
        JPanel panel = createTransactionFormPanel();
        JTextField accountIdField = createStyledTextField("Account ID");
        JTextField amountField = createStyledTextField("Amount (₹)");
        JButton withdrawButton = createStyledButton("Withdraw Funds", new Color(231, 76, 60));

        addGBCComponent(panel, new JLabel("Account ID:"), 0, 0, 1);
        addGBCComponent(panel, accountIdField, 1, 0, 2);
        addGBCComponent(panel, new JLabel("Amount (₹):"), 0, 1, 1);
        addGBCComponent(panel, amountField, 1, 1, 2);
        addGBCComponent(panel, withdrawButton, 1, 2, 2);

        withdrawButton.addActionListener(e -> handleWithdrawal(accountIdField.getText(), amountField.getText()));
        return panel;
    }

    private JPanel createTransferPanel() {
        JPanel panel = createTransactionFormPanel();
        JTextField sourceIdField = createStyledTextField("Source Account ID");
        JTextField destIdField = createStyledTextField("Destination Account ID");
        JTextField amountField = createStyledTextField("Amount (₹)");
        JButton transferButton = createStyledButton("Perform Transfer", new Color(243, 156, 18));

        addGBCComponent(panel, new JLabel("Source Account ID:"), 0, 0, 1);
        addGBCComponent(panel, sourceIdField, 1, 0, 2);
        addGBCComponent(panel, new JLabel("Destination Account ID:"), 0, 1, 1);
        addGBCComponent(panel, destIdField, 1, 1, 2);
        addGBCComponent(panel, new JLabel("Amount (₹):"), 0, 2, 1);
        addGBCComponent(panel, amountField, 1, 2, 2);
        addGBCComponent(panel, transferButton, 1, 3, 2);

        transferButton.addActionListener(e -> handleTransfer(sourceIdField.getText(), destIdField.getText(), amountField.getText()));
        return panel;
    }

    private JPanel createBalancePanel() {
        JPanel panel = createTransactionFormPanel();
        JTextField accountIdField = createStyledTextField("Account ID");
        JButton checkButton = createStyledButton("Check Balance", new Color(155, 89, 182));
        JTextArea resultArea = new JTextArea(10, 30);
        resultArea.setEditable(false);
        resultArea.setBackground(PANEL_COLOR);
        resultArea.setForeground(TEXT_COLOR);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
        resultArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.getViewport().setBackground(PANEL_COLOR);

        addGBCComponent(panel, new JLabel("Account ID:"), 0, 0, 1);
        addGBCComponent(panel, accountIdField, 1, 0, 2);
        addGBCComponent(panel, checkButton, 1, 1, 2);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3;
        panel.add(scrollPane, gbc);

        checkButton.addActionListener(e -> handleBalanceCheck(accountIdField.getText(), resultArea));
        return panel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG_COLOR);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        inputPanel.setBackground(PANEL_COLOR);
        inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel accIdLabel = new JLabel("Account ID:");
        accIdLabel.setForeground(TEXT_COLOR);

        JTextField accountIdField = createStyledTextField("Account ID");
        accountIdField.setColumns(15);

        JButton viewButton = createStyledButton("View History", new Color(26, 188, 156));

        inputPanel.add(accIdLabel);
        inputPanel.add(accountIdField);
        inputPanel.add(viewButton);
        panel.add(inputPanel, BorderLayout.NORTH);

        String[] columns = {"ID", "Type", "Amount (₹)", "Balance After (₹)", "Date", "Description"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = createStyledTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(PANEL_COLOR);
        panel.add(scrollPane, BorderLayout.CENTER);

        viewButton.addActionListener(e -> handleTransactionHistory(accountIdField.getText(), model));
        return panel;
    }

    // ======================================================
    // ✅ CORE BUSINESS LOGIC HANDLERS
    // ======================================================

    private void handleViewAccountDetails(JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an account from the table.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int accountId = (int) accountTableModel.getValueAt(selectedRow, 0);
            Account account = findAccount(accountId);
            Customer customer = findCustomer(account.customerId);

            if (account != null && customer != null) {
                String details = String.format(
                    "Account ID: %d\nCustomer ID: %d\nCustomer Name: %s\nAccount Type: %s\nStatus: %s\nCurrent Balance: ₹%.2f\nCreated: %s",
                    account.accountId, customer.id, customer.name, account.accountType, account.status, account.balance, account.createdDate);

                JOptionPane.showMessageDialog(this, details, "Account Details", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not retrieve account details.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeposit(String accountIdStr, String amountStr) {
        try {
            int accountId = Integer.parseInt(accountIdStr.trim());
            double amount = Double.parseDouble(amountStr.trim());
            Account account = findAccount(accountId);

            if (account == null) {
                JOptionPane.showMessageDialog(this, "Account ID not found.", "Error", JOptionPane.ERROR_MESSAGE); return;
            }
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Deposit amount must be positive.", "Error", JOptionPane.ERROR_MESSAGE); return;
            }

            account.balance += amount;
            saveAccounts();

            Transaction t = new Transaction(nextTransactionId++, accountId, "DEPOSIT", amount, account.balance, LocalDateTime.now().format(DTF), "Cash Deposit");
            transactions.add(t);
            saveTransactions();

            loadAccountsIntoTable(accountTableModel);
            refreshDashboard();
            JOptionPane.showMessageDialog(this, String.format("✅ Deposit of ₹%.2f successful.\nNew Balance: ₹%.2f", amount, account.balance), "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid input! ID and Amount must be numbers.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleWithdrawal(String accountIdStr, String amountStr) {
        try {
            int accountId = Integer.parseInt(accountIdStr.trim());
            double amount = Double.parseDouble(amountStr.trim());
            Account account = findAccount(accountId);

            if (account == null) {
                JOptionPane.showMessageDialog(this, "Account ID not found.", "Error", JOptionPane.ERROR_MESSAGE); return;
            }
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Withdrawal amount must be positive.", "Error", JOptionPane.ERROR_MESSAGE); return;
            }
            if (account.balance < amount) {
                JOptionPane.showMessageDialog(this, "Insufficient funds! Balance: ₹" + String.format("%.2f", account.balance), "Error", JOptionPane.ERROR_MESSAGE); return;
            }

            account.balance -= amount;
            saveAccounts();

            Transaction t = new Transaction(nextTransactionId++, accountId, "WITHDRAWAL", amount, account.balance, LocalDateTime.now().format(DTF), "Cash Withdrawal");
            transactions.add(t);
            saveTransactions();

            loadAccountsIntoTable(accountTableModel);
            refreshDashboard();
            JOptionPane.showMessageDialog(this, String.format("✅ Withdrawal of ₹%.2f successful.\nNew Balance: ₹%.2f", amount, account.balance), "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid input! ID and Amount must be numbers.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleTransfer(String sourceIdStr, String destIdStr, String amountStr) {
        try {
            int sourceId = Integer.parseInt(sourceIdStr.trim());
            int destId = Integer.parseInt(destIdStr.trim());
            double amount = Double.parseDouble(amountStr.trim());

            Account sourceAccount = findAccount(sourceId);
            Account destAccount = findAccount(destId);

            if (sourceAccount == null || destAccount == null) {
                JOptionPane.showMessageDialog(this, "One or both Account IDs not found.", "Error", JOptionPane.ERROR_MESSAGE); return;
            }
            if (sourceId == destId) {
                JOptionPane.showMessageDialog(this, "Cannot transfer to the same account.", "Error", JOptionPane.ERROR_MESSAGE); return;
            }
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Transfer amount must be positive.", "Error", JOptionPane.ERROR_MESSAGE); return;
            }
            if (sourceAccount.balance < amount) {
                JOptionPane.showMessageDialog(this, "Insufficient funds in source account.", "Error", JOptionPane.ERROR_MESSAGE); return;
            }

            // Perform transfer
            sourceAccount.balance -= amount;
            destAccount.balance += amount;
            saveAccounts();

            String date = LocalDateTime.now().format(DTF);

            // Source transaction (Debit)
            Transaction tSource = new Transaction(nextTransactionId++, sourceId, "TRANSFER_OUT", amount, sourceAccount.balance, date, "Transfer to " + destId);
            transactions.add(tSource);

            // Destination transaction (Credit)
            Transaction tDest = new Transaction(nextTransactionId++, destId, "TRANSFER_IN", amount, destAccount.balance, date, "Transfer from " + sourceId);
            transactions.add(tDest);
            saveTransactions();

            loadAccountsIntoTable(accountTableModel);
            refreshDashboard();
            JOptionPane.showMessageDialog(this, String.format("✅ Transfer of ₹%.2f successful.\nSource New Balance: ₹%.2f\nDest New Balance: ₹%.2f",
                amount, sourceAccount.balance, destAccount.balance), "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid input! IDs and Amount must be numbers.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleBalanceCheck(String accountIdStr, JTextArea resultArea) {
        resultArea.setText("");
        try {
            int accountId = Integer.parseInt(accountIdStr.trim());
            Account account = findAccount(accountId);

            if (account == null) {
                resultArea.setText("Error: Account ID " + accountId + " not found.");
                return;
            }

            Customer customer = findCustomer(account.customerId);

            String result = String.format(
                "--- Account Status ---\n" +
                "Account ID: %d\n" +
                "Customer: %s (ID: %d)\n" +
                "Type: %s\n" +
                "Status: %s\n" +
                "----------------------\n" +
                "Current Balance: ₹%.2f",
                account.accountId, customer != null ? customer.name : "N/A", account.customerId,
                account.accountType, account.status, account.balance);

            resultArea.setText(result);

        } catch (NumberFormatException ex) {
            resultArea.setText("Error: Invalid input! Account ID must be a number.");
        }
    }

    private void handleTransactionHistory(String accountIdStr, DefaultTableModel model) {
        model.setRowCount(0);
        try {
            int accountId = Integer.parseInt(accountIdStr.trim());
            Account account = findAccount(accountId);

            if (account == null) {
                JOptionPane.showMessageDialog(this, "Account ID not found.", "Error", JOptionPane.ERROR_MESSAGE); return;
            }

            List<Transaction> accountHistory = transactions.stream()
                .filter(t -> t.accountId == accountId)
                .collect(Collectors.toList());

            for (Transaction t : accountHistory) {
                model.addRow(new Object[]{
                    t.transactionId,
                    t.type,
                    String.format("₹%.2f", t.amount),
                    String.format("₹%.2f", t.balanceAfter),
                    t.date,
                    t.description
                });
            }

            if (accountHistory.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No transactions found for Account ID " + accountId + ".", "Info", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid input! Account ID must be a number.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    // --- Account Creation Logic ---
    private void handleAccountCreation(String customerIdStr, String accountType, String initialDepositStr, JTextField customerIdField, JTextField initialDepositField) {
        try {
            int customerId = Integer.parseInt(customerIdStr.trim());
            double initialDeposit = Double.parseDouble(initialDepositStr.trim());

            if (findCustomer(customerId) == null) {
                JOptionPane.showMessageDialog(this, "Customer ID not found!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (initialDeposit < 0) {
                JOptionPane.showMessageDialog(this, "Initial deposit cannot be negative!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String currentDate = LocalDateTime.now().format(DTF);
            Account account = new Account(nextAccountId++, customerId, accountType, initialDeposit, "Active", currentDate);
            accounts.add(account);
            saveAccounts();

            if (initialDeposit > 0) {
                Transaction transaction = new Transaction(nextTransactionId++, account.accountId, "DEPOSIT", initialDeposit, initialDeposit, currentDate, "Initial deposit");
                transactions.add(transaction);
                saveTransactions();
            }

            loadAccountsIntoTable(accountTableModel);
            refreshDashboard();

            customerIdField.setText("");
            initialDepositField.setText("");

            JOptionPane.showMessageDialog(this, "✅ Account created successfully! ID: " + account.accountId, "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid input! Customer ID and Deposit must be numbers.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshDashboard() {
        // Get the original username from the title
        String fullTitle = getTitle();
        String username = fullTitle.substring(fullTitle.lastIndexOf(" ") + 1);

        // Remove and re-add the dashboard panel to force refresh calculations
        int index = tabbedPane.indexOfComponent(dashboardPanel);
        if (index != -1) {
            tabbedPane.removeTabAt(index);
        }
        dashboardPanel = createDashboardPanel(username);
        tabbedPane.insertTab("🏠 Dashboard", null, dashboardPanel, "View Overview", 0);
    }

    // ======================================================
    // ✅ UTILITY & HELPER METHODS
    // ======================================================
    private JPanel createTransactionFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(PANEL_COLOR);
        panel.setBorder(new EmptyBorder(30, 50, 30, 50));
        return panel;
    }

    private void addGBCComponent(JPanel panel, JComponent component, int gridx, int gridy, int gridwidth) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.gridwidth = gridwidth;
        if (component instanceof JLabel) {
            ((JLabel) component).setForeground(TEXT_COLOR);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.weightx = 0;
        } else {
            gbc.weightx = 1.0;
        }
        panel.add(component, gbc);
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setBackground(PANEL_COLOR);
        table.setForeground(Color.WHITE);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(25);
        table.getTableHeader().setBackground(new Color(60, 63, 65));
        table.getTableHeader().setForeground(ACCENT_COLOR);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        return table;
    }

    // UPDATED: TitledBorder text color changed to WHITE
    private JTextField createStyledTextField(String title) {
        JTextField field = new JTextField();
        if (title != null) {
            TitledBorder titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), title, 
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.PLAIN, 12), Color.WHITE); // SET TEXT TO WHITE
            field.setBorder(titledBorder);
        }
        field.setBackground(new Color(60, 63, 65));
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(TEXT_COLOR);
        return field;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton button = new JButton(text);
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        return button;
    }

    private void loadCustomersIntoTable(DefaultTableModel model) {
        model.setRowCount(0);
        for (Customer c : customers) {
            model.addRow(new Object[]{c.id, c.name, c.email, c.phone, c.address});
        }
    }

    private void loadAccountsIntoTable(DefaultTableModel model) {
        model.setRowCount(0);
        for (Account a : accounts) {
            model.addRow(new Object[]{
                a.accountId,
                a.customerId,
                a.accountType,
                String.format("₹%.2f", a.balance),
                a.status,
                a.createdDate
            });
        }
    }

    private void recalculateNextIds() {
        customers.stream().mapToInt(c -> c.id).max().ifPresent(maxId -> nextCustomerId = maxId + 1);
        accounts.stream().mapToInt(a -> a.accountId).max().ifPresent(maxId -> nextAccountId = maxId + 1);
        transactions.stream().mapToInt(t -> t.transactionId).max().ifPresent(maxId -> nextTransactionId = maxId + 1);
    }

    private Account findAccount(int accountId) {
        for (Account a : accounts) {
            if (a.accountId == accountId) return a;
        }
        return null;
    }

    private Customer findCustomer(int customerId) {
        for (Customer c : customers) {
            if (c.id == customerId) return c;
        }
        return null;
    }

    // ======================================================
    // ✅ FILE HANDLING
    // ======================================================
    private void loadData() {
        loadCustomers();
        loadAccounts();
        loadTransactions();
    }

    private void loadCustomers() {
        try (BufferedReader br = new BufferedReader(new FileReader(CUSTOMERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    customers.add(Customer.fromCSV(line));
                }
            }
        } catch (IOException ignored) {}
    }

    private void saveCustomers() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(CUSTOMERS_FILE))) {
            for (Customer c : customers) {
                bw.write(c.toCSV());
                bw.newLine();
            }
        } catch (IOException ignored) {}
    }

    private void loadAccounts() {
        try (BufferedReader br = new BufferedReader(new FileReader(ACCOUNTS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    accounts.add(Account.fromCSV(line));
                }
            }
        } catch (IOException ignored) {}
    }

    private void saveAccounts() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ACCOUNTS_FILE))) {
            for (Account a : accounts) {
                bw.write(a.toCSV());
                bw.newLine();
            }
        } catch (IOException ignored) {}
    }

    private void loadTransactions() {
        try (BufferedReader br = new BufferedReader(new FileReader(TRANSACTIONS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    transactions.add(Transaction.fromCSV(line));
                }
            }
        } catch (IOException ignored) {}
    }

    private void saveTransactions() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(TRANSACTIONS_FILE))) {
            for (Transaction t : transactions) {
                bw.write(t.toCSV());
                bw.newLine();
            }
        } catch (IOException ignored) {}
    }

    // ======================================================
    // ✅ DATA CLASSES
    // ======================================================
    static class Customer {
        int id;
        String name, email, phone, address;
        Customer(int id, String name, String email, String phone, String address) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.address = address;
        }
        static Customer fromCSV(String csv) {
            String[] p = csv.split(",");
            return new Customer(Integer.parseInt(p[0]), p[1], p[2], p[3], p[4]);
        }
        String toCSV() {
            return id + "," + name + "," + email + "," + phone + "," + address;
        }
    }

    static class Account {
        int accountId, customerId;
        String accountType, status, createdDate;
        double balance;
        Account(int accountId, int customerId, String accountType, double balance, String status, String createdDate) {
            this.accountId = accountId;
            this.customerId = customerId;
            this.accountType = accountType;
            this.balance = balance;
            this.status = status;
            this.createdDate = createdDate;
        }
        static Account fromCSV(String csv) {
            String[] p = csv.split(",");
            return new Account(Integer.parseInt(p[0]), Integer.parseInt(p[1]), p[2],
                                     Double.parseDouble(p[3]), p[4], p[5]);
        }
        String toCSV() {
              return accountId + "," + customerId + "," + accountType + "," + balance + "," + status + "," + createdDate;
        }
    }

    static class Transaction {
        int transactionId, accountId;
        String type, date, description;
        double amount, balanceAfter;
        Transaction(int transactionId, int accountId, String type,
                    double amount, double balanceAfter, String date, String description) {
            this.transactionId = transactionId;
            this.accountId = accountId;
            this.type = type;
            this.amount = amount;
            this.balanceAfter = balanceAfter;
            this.date = date;
            this.description = description;
        }
        static Transaction fromCSV(String csv) {
            String[] p = csv.split(",", 7); // Limit split to 7 to handle commas in description (if any)
            return new Transaction(Integer.parseInt(p[0]), Integer.parseInt(p[1]), p[2],
                                     Double.parseDouble(p[3]), Double.parseDouble(p[4]), p[5], p.length > 6 ? p[6] : "");
        }
        String toCSV() {
              return transactionId + "," + accountId + "," + type + "," + amount + "," + balanceAfter + "," + date + "," + description;
        }
    }

    // ======================================================
    // ✅ START LOGIN
    // ======================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginFrame::new);
    }
}

// ======================================================
// ✅ LOGIN FRAME (UPDATED)
// ======================================================
class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private final String USERS_FILE = "users.csv";
    private final String USER_DETAILS_FILE = "user_details.csv";

    public LoginFrame() {
        setTitle("🔐 Bank Login");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setLayout(new BorderLayout());

        // Dark theme colors
        Color BG_COLOR = new Color(30, 33, 35);
        Color PANEL_COLOR = new Color(45, 48, 50);
        Color ACCENT_COLOR = new Color(0, 153, 255);
        Color TEXT_COLOR = Color.WHITE;

        // --- 1. Top Title Panel ---
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(PANEL_COLOR);
        headerPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        
        JLabel mainTitle = new JLabel("BANKING MANAGEMENT LOGIN", SwingConstants.CENTER); 
        mainTitle.setForeground(ACCENT_COLOR);
        mainTitle.setFont(new Font("Segoe UI", Font.BOLD, 36));
        headerPanel.add(mainTitle);
        add(headerPanel, BorderLayout.NORTH);

        // --- 2. Login Form Panel (Centered) ---
        JPanel formContainer = new JPanel(new GridBagLayout());
        formContainer.setBackground(BG_COLOR);

        // Login box content
        JPanel panel = new JPanel(new BorderLayout(10, 20));
        panel.setBackground(new Color(25, 25, 25));
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));
        panel.setPreferredSize(new java.awt.Dimension(350, 400));

        // UPDATED: "Account Holder Login"
        JLabel title = new JLabel("Account Holder Login", SwingConstants.CENTER);
        title.setForeground(ACCENT_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        panel.add(title, BorderLayout.NORTH);

        // --- Input Fields Panel (Grouped) ---
        JPanel inputPanel = new JPanel(new GridLayout(3, 1, 10, 15)); 
        inputPanel.setBackground(panel.getBackground());

        usernameField = createStyledTextField("Username");
        emailField = createStyledTextField("Email"); 
        passwordField = createStyledPasswordField("Password");

        inputPanel.add(usernameField);
        inputPanel.add(emailField); 
        inputPanel.add(passwordField);
        panel.add(inputPanel, BorderLayout.CENTER);

        // --- Button Panel (Grouped at the bottom) ---
        JPanel buttonGroupPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        buttonGroupPanel.setBackground(panel.getBackground());

        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register"); 
        JButton forgotPasswordButton = new JButton("Forgot Password?"); 

        styleButton(loginBtn, ACCENT_COLOR); 
        styleButton(registerBtn, new Color(60, 63, 65)); 
        styleButton(forgotPasswordButton, new Color(46, 204, 113)); 

        buttonGroupPanel.add(loginBtn);
        buttonGroupPanel.add(registerBtn);
        buttonGroupPanel.add(forgotPasswordButton);

        panel.add(buttonGroupPanel, BorderLayout.SOUTH);

        formContainer.add(panel);
        add(formContainer, BorderLayout.CENTER);

        loginBtn.addActionListener(e -> handleLogin());
        registerBtn.addActionListener(e -> new RegisterFrame(this));
        
        forgotPasswordButton.addActionListener(e -> new ForgotPasswordFrame(this));

        setVisible(true);
    }

    // Method to find and return the email associated with a username
    private String findEmail(String username) {
        try (BufferedReader br = new BufferedReader(new FileReader(USER_DETAILS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                // user_details.csv structure: Username, FirstName, LastName, Phone, Email, Address
                if (parts.length >= 5 && parts[0].equals(username)) {
                    return parts[4]; // Email is at index 4
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    // Method to check if user details exist
    private boolean userDetailsExist(String username) {
        return findEmail(username) != null;
    }

    // UPDATED: TitledBorder text color changed to WHITE
    private JTextField createStyledTextField(String title) {
        JTextField field = new JTextField();
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), title, 
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.PLAIN, 12), Color.WHITE); // SET TEXT TO WHITE
        field.setBorder(titledBorder);
        field.setBackground(new Color(60, 63, 65));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        return field;
    }

    // UPDATED: TitledBorder text color changed to WHITE
    private JPasswordField createStyledPasswordField(String title) {
        JPasswordField field = new JPasswordField();
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), title, 
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.PLAIN, 12), Color.WHITE); // SET TEXT TO WHITE
        field.setBorder(titledBorder);
        field.setBackground(new Color(60, 63, 65));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        return field;
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) { 
            JOptionPane.showMessageDialog(this, "Please fill the Username and Password fields!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            boolean found = false;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2 && parts[0].equals(username) && parts[1].equals(password)) {
                    found = true;
                    break;
                }
            }

            if (found) {
                JOptionPane.showMessageDialog(this, "✅ Login Successful! Welcome " + username);
                dispose();

                if (userDetailsExist(username)) {
                    new BankingManagementSystem(username);
                } else {
                    // Force user to complete Step 2 details
                    new CustomerDetailsFrame(username);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Username or Password.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "System error: User database not found. Please register first.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

// ======================================================
// ✅ FORGOT PASSWORD FRAME (UPDATED: Password Length)
// ======================================================
class ForgotPasswordFrame extends JFrame {
    private JTextField usernameField;
    private JTextField emailField;
    private JPasswordField newPasswordField, confirmNewPasswordField;
    private final String USERS_FILE = "users.csv";
    private final String USER_DETAILS_FILE = "user_details.csv";
    private JFrame loginFrame;

    public ForgotPasswordFrame(JFrame loginFrame) {
        this.loginFrame = loginFrame;
        setTitle("🔑 Reset Password");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(500, 500);
        setLocationRelativeTo(null);
        getContentPane().setLayout(new BorderLayout());

        // Dark theme colors
        Color BG_COLOR = new Color(30, 33, 35);
        Color ACCENT_COLOR = new Color(0, 153, 255);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 20));
        mainPanel.setBackground(BG_COLOR);
        mainPanel.setBorder(new EmptyBorder(30, 40, 30, 40));

        JLabel title = new JLabel("Reset Account Password", SwingConstants.CENTER);
        title.setForeground(ACCENT_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        mainPanel.add(title, BorderLayout.NORTH);

        // Form Panel
        JPanel formPanel = new JPanel(new GridLayout(6, 1, 10, 15));
        formPanel.setBackground(mainPanel.getBackground());
        
        // UPDATED GUIDE TEXT
        String newPasswordGuide = "New Password (4+ chars, 1 Cap, 1 Digit)";

        usernameField = createStyledTextField("Username for Verification");
        emailField = createStyledTextField("Email for Verification");
        newPasswordField = createStyledPasswordField(newPasswordGuide);
        confirmNewPasswordField = createStyledPasswordField("Confirm New Password");

        JButton resetButton = new JButton("Reset Password");
        styleButton(resetButton, new Color(46, 204, 113));

        formPanel.add(usernameField);
        formPanel.add(emailField);
        formPanel.add(new JLabel("--- Enter New Password ---", SwingConstants.CENTER));
        formPanel.add(newPasswordField);
        formPanel.add(confirmNewPasswordField);
        formPanel.add(resetButton);

        mainPanel.add(formPanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);

        resetButton.addActionListener(e -> handlePasswordReset());
        setVisible(true);
    }

    // UPDATED: TitledBorder text color changed to WHITE
    private JTextField createStyledTextField(String title) {
        JTextField field = new JTextField();
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), title, 
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.PLAIN, 12), Color.WHITE); // SET TEXT TO WHITE
        field.setBorder(titledBorder);
        field.setBackground(new Color(60, 63, 65));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        return field;
    }

    // UPDATED: TitledBorder text color changed to WHITE
    private JPasswordField createStyledPasswordField(String title) {
        JPasswordField field = new JPasswordField();
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), title, 
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.PLAIN, 12), Color.WHITE); // SET TEXT TO WHITE
        field.setBorder(titledBorder);
        field.setBackground(new Color(60, 63, 65));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        return field;
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
    }

    // UPDATED: Password length is now 4
    private boolean validatePassword(String password) {
        // Requires: At least one uppercase letter (A-Z), At least one digit (0-9), Minimum 4 characters in length.
        Pattern pattern = Pattern.compile("^(?=.*[A-Z])(?=.*[0-9]).{4,}$");
        return pattern.matcher(password).matches();
    }
    
    // Helper to find the email associated with a username in user_details.csv
    private String getStoredEmail(String username) {
        try (BufferedReader br = new BufferedReader(new FileReader(USER_DETAILS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                // user_details.csv: Username, FirstName, LastName, Phone, Email, Address
                if (parts.length >= 5 && parts[0].equals(username)) {
                    return parts[4];
                }
            }
        } catch (IOException ignored) {}
        return null;
    }

    private void handlePasswordReset() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String newPassword = new String(newPasswordField.getPassword());
        String confirmPassword = new String(confirmNewPasswordField.getPassword());

        if (username.isEmpty() || email.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required for password reset.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 1. Validate Password
        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!validatePassword(newPassword)) {
            // UPDATED: Error message for 4 chars
            JOptionPane.showMessageDialog(this, "New Password Policy: Must be 4+ chars, have 1 uppercase, 1 digit.", "Password Policy Violation", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Verify Credentials against user_details.csv
        String storedEmail = getStoredEmail(username);
        if (storedEmail == null || !storedEmail.equalsIgnoreCase(email)) {
            JOptionPane.showMessageDialog(this, "Verification failed: Username and Email do not match our records.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 3. Update Password in users.csv
        try {
            File usersFile = new File(USERS_FILE);
            List<String> updatedLines = new ArrayList<>();
            boolean passwordUpdated = false;

            try (BufferedReader br = new BufferedReader(new FileReader(usersFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 2 && parts[0].equals(username)) {
                        // Found user, update the password (parts[1])
                        updatedLines.add(username + "," + newPassword);
                        passwordUpdated = true;
                    } else {
                        updatedLines.add(line);
                    }
                }
            }

            if (passwordUpdated) {
                // Write all lines back to the file
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(usersFile, false))) {
                    for (String line : updatedLines) {
                        bw.write(line);
                        bw.newLine();
                    }
                }
                JOptionPane.showMessageDialog(this, "✅ Password reset successfully! You can now log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Error: Username not found in the user database.", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "An error occurred during file operations.", "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}


// ======================================================
// ✅ REGISTRATION FRAME (UPDATED: Password Length)
// ======================================================
class RegisterFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField, confirmPasswordField;
    private JLabel guidelinesLabel;
    private final String USERS_FILE = "users.csv";
    private JFrame loginFrame;

    public RegisterFrame(JFrame loginFrame) {
        this.loginFrame = loginFrame;
        setTitle("📝 Register New Account Holder");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setLayout(new BorderLayout());

        // Dark theme colors
        Color BG_COLOR = new Color(30, 33, 35);
        Color PANEL_COLOR = new Color(45, 48, 50);
        Color ACCENT_COLOR = new Color(0, 153, 255);

        // --- 1. Top Title Panel ---
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(PANEL_COLOR);
        headerPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        JLabel mainTitle = new JLabel("BANKING MANAGEMENT SYSTEM", SwingConstants.CENTER);
        mainTitle.setForeground(ACCENT_COLOR);
        mainTitle.setFont(new Font("Segoe UI", Font.BOLD, 36));
        headerPanel.add(mainTitle);
        add(headerPanel, BorderLayout.NORTH);

        // --- 2. Registration Form Panel (Centered) ---
        JPanel formContainer = new JPanel(new GridBagLayout());
        formContainer.setBackground(BG_COLOR);

        JPanel panel = new JPanel(new GridLayout(6, 1, 10, 10));
        panel.setBackground(new Color(25, 25, 25));
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));
        panel.setPreferredSize(new java.awt.Dimension(400, 450));

        // UPDATED: "Create Account Holder Account"
        JLabel title = new JLabel("Create Account Holder Account (Step 1 of 2)", SwingConstants.CENTER);
        title.setForeground(ACCENT_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));

        usernameField = createStyledTextField("Username");
        passwordField = createStyledPasswordField("Password");
        confirmPasswordField = createStyledPasswordField("Confirm Password");

        // UPDATED: New password length rule
        guidelinesLabel = new JLabel("Password must be 4+ chars, have 1 uppercase, 1 digit.", SwingConstants.CENTER);
        guidelinesLabel.setForeground(Color.ORANGE);
        guidelinesLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));

        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                checkPasswordStrength(new String(passwordField.getPassword()));
            }
        });

        JButton registerBtn = new JButton("Register & Proceed to Details");
        styleButton(registerBtn, ACCENT_COLOR);

        panel.add(title);
        panel.add(usernameField);
        panel.add(passwordField);
        panel.add(confirmPasswordField);
        panel.add(guidelinesLabel);
        panel.add(registerBtn);

        formContainer.add(panel);
        add(formContainer, BorderLayout.CENTER);

        registerBtn.addActionListener(e -> handleRegistration());
        setVisible(true);
    }

    private void checkPasswordStrength(String password) {
        boolean isValid = validatePassword(password);
        if (isValid) {
            guidelinesLabel.setText("Password Strength: STRONG ✅");
            guidelinesLabel.setForeground(Color.GREEN);
        } else {
            // UPDATED: New password length rule
            guidelinesLabel.setText("Password must be 4+ chars, have 1 uppercase, 1 digit.");
            guidelinesLabel.setForeground(Color.ORANGE);
        }
    }

    // UPDATED: TitledBorder text color changed to WHITE
    private JTextField createStyledTextField(String title) {
        JTextField field = new JTextField();
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), title, 
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.PLAIN, 12), Color.WHITE); // SET TEXT TO WHITE
        field.setBorder(titledBorder);
        field.setBackground(new Color(60, 63, 65));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        return field;
    }

    // UPDATED: TitledBorder text color changed to WHITE
    private JPasswordField createStyledPasswordField(String title) {
        JPasswordField field = new JPasswordField();
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), title, 
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.PLAIN, 12), Color.WHITE); // SET TEXT TO WHITE
        field.setBorder(titledBorder);
        field.setBackground(new Color(60, 63, 65));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        return field;
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
    }

    // UPDATED: Password length is now 4
    private boolean validatePassword(String password) {
        // Requires: At least one uppercase letter (A-Z), At least one digit (0-9), Minimum 4 characters in length.
        Pattern pattern = Pattern.compile("^(?=.*[A-Z])(?=.*[0-9]).{4,}$");
        return pattern.matcher(password).matches();
    }

    private void handleRegistration() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!validatePassword(password)) {
            JOptionPane.showMessageDialog(this, guidelinesLabel.getText(), "Password Policy Violation", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            File file = new File(USERS_FILE);

            // Check for existing username
            if (file.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split(",");
                        if (parts.length > 0 && parts[0].equals(username)) {
                            JOptionPane.showMessageDialog(this, "Username already exists!", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }
            } else {
                file.createNewFile();
            }

            // Save new user
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
                bw.write(username + "," + password);
                bw.newLine();
            }

            // UPDATED: Success message
            JOptionPane.showMessageDialog(this, "✅ Account registration successful! Now enter your details.", "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            // Proceed to the Customer Details screen (Step 2)
            new CustomerDetailsFrame(username);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "An error occurred during file operations.", "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}


// ======================================================
// ✅ CUSTOMER DETAILS (STEP 2)
// ======================================================
class CustomerDetailsFrame extends JFrame {
    private JTextField firstNameField, lastNameField, phoneField, addressField, emailField;
    private String loggedInUsername;
    private final String USER_DETAILS_FILE = "user_details.csv";

    public CustomerDetailsFrame(String username) {
        this.loggedInUsername = username;
        setTitle("👤 Account Holder Details - Step 2 of 2");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(30, 33, 35));

        // Dark theme colors
        Color BG_COLOR = new Color(30, 33, 35);
        Color PANEL_COLOR = new Color(45, 48, 50);
        Color ACCENT_COLOR = new Color(0, 153, 255);

        // --- 1. Top Title Panel ---
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(PANEL_COLOR);
        headerPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        JLabel mainTitle = new JLabel("BANKING MANAGEMENT SYSTEM", SwingConstants.CENTER);
        mainTitle.setForeground(ACCENT_COLOR);
        mainTitle.setFont(new Font("Segoe UI", Font.BOLD, 36));
        headerPanel.add(mainTitle);
        add(headerPanel, BorderLayout.NORTH);

        // --- 2. Details Form Panel (Centered) ---
        JPanel formContainer = new JPanel(new GridBagLayout());
        formContainer.setBackground(BG_COLOR);

        JPanel panel = new JPanel(new GridLayout(7, 1, 10, 10));
        panel.setBackground(new Color(25, 25, 25));
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));
        panel.setPreferredSize(new java.awt.Dimension(400, 500));

        // UPDATED: "Account Holder Profile Details"
        JLabel title = new JLabel("Account Holder Profile Details", SwingConstants.CENTER);
        title.setForeground(new Color(46, 204, 113));
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));

        firstNameField = createStyledTextField("First Name (First Letter Capital)");
        lastNameField = createStyledTextField("Last Name");
        phoneField = createStyledTextField("Phone");
        emailField = createStyledTextField("Email");
        addressField = createStyledTextField("Address");

        JButton saveButton = new JButton("Save & Open Main System");
        styleButton(saveButton, new Color(46, 204, 113));

        panel.add(title);
        panel.add(firstNameField);
        panel.add(lastNameField);
        panel.add(phoneField);
        panel.add(emailField);
        panel.add(addressField);
        panel.add(saveButton);

        formContainer.add(panel);
        add(formContainer, BorderLayout.CENTER);

        saveButton.addActionListener(e -> handleSaveDetails());
        setVisible(true);
    }

    // UPDATED: TitledBorder text color changed to WHITE
    private JTextField createStyledTextField(String title) {
        JTextField field = new JTextField();
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), title, 
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.PLAIN, 12), Color.WHITE); // SET TEXT TO WHITE
        field.setBorder(titledBorder);
        field.setBackground(new Color(60, 63, 65));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        return field;
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
    }

    private void handleSaveDetails() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String address = addressField.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || phone.isEmpty() || address.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!Character.isUpperCase(firstName.charAt(0))) {
            JOptionPane.showMessageDialog(this, "First Name must start with a capital letter.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Save logic: Check if user details already exist and overwrite if so (optional: remove old line)
        try {
            File detailsFile = new File(USER_DETAILS_FILE);
            List<String> lines = new ArrayList<>();

            if (detailsFile.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(detailsFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split(",");
                        if (parts.length > 0 && parts[0].equals(loggedInUsername)) {
                            // Skip the old line for the current user
                            continue;
                        }
                        lines.add(line);
                    }
                }
            }

            // Append the new/updated details
            String newDetails = loggedInUsername + "," + firstName + "," + lastName + "," + phone + "," + email + "," + address;
            lines.add(newDetails);

            // Write all lines back to the file
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(detailsFile, false))) {
                for (String line : lines) {
                    bw.write(line);
                    bw.newLine();
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving user details.", "File Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this, "Details saved. Opening main system.");
        dispose();
        // Open the main application
        new BankingManagementSystem(loggedInUsername);
    }
}