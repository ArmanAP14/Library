package client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import common.Protocol;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ClientGui extends JFrame {

    private static final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
    private final LibraryClient client = new LibraryClient();

    private JTextField hostField;
    private JTextField portField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton connectBtn;
    private JButton loginBtn;
    private JButton registerBtn;
    private JButton logoutBtn;
    private JLabel statusLabel;

    private JTabbedPane mainTabs;

    private JLabel balanceLabel;
    private JTextField chargeField;
    private JButton chargeBtn;
    private JButton refreshBalanceBtn;

    private JTable marketTable;
    private DefaultTableModel marketModel;
    private JButton buyBookBtn;
    private JButton refreshMarketBtn;

    private JTable libraryTable;
    private DefaultTableModel libraryModel;
    private JComboBox<String> libraryFilter;
    private JButton refreshLibraryBtn;
    private JTextField progressPageField;
    private JButton submitProgressBtn;

    private JTable clubsTable;
    private DefaultTableModel clubsModel;
    private JComboBox<String> clubsScope;
    private JTextField clubNameField;
    private JButton refreshClubsBtn;
    private JButton createClubBtn;
    private JButton joinClubBtn;
    private JButton viewClubBtn;

    private JSplitPane clubsSplitPane;
    private JPanel clubDetailPanel;
    private JLabel clubDetailLabel;
    private JTable membersTable;
    private DefaultTableModel membersModel;
    private JTextArea fundraiserArea;
    private JTextField joinUserField;
    private JButton acceptJoinBtn;
    private JButton denyJoinBtn;
    private JTextField donateField;
    private JTextField fundraiserBookField;
    private JButton refreshMembersBtn;
    private JButton removeMemberBtn;
    private JButton refreshFundraiserBtn;
    private JButton createFundraiserBtn;
    private JButton donateBtn;
    private JButton exitClubBtn;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            ClientGui gui = new ClientGui();
            gui.setVisible(true);
        });
    }

    public ClientGui() {
        super("Library Client");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1000, 680);
        setLocationRelativeTo(null);
        buildUi();
        loadMarketBooksLocal();
        setLoggedIn(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                client.disconnect();
            }
        });
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.add(buildTopPanel(), BorderLayout.NORTH);
        root.add(buildMainPanel(), BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Connection & Account"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JLabel("Host:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.3;
        hostField = new JTextField("localhost", 12);
        panel.add(hostField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.15;
        portField = new JTextField(String.valueOf(Protocol.DEFAULT_TCP_PORT), 6);
        panel.add(portField, gbc);

        gbc.gridx = 4; gbc.weightx = 0;
        connectBtn = new JButton("Connect");
        connectBtn.addActionListener(e -> onConnect());
        panel.add(connectBtn, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.3;
        usernameField = new JTextField(12);
        panel.add(usernameField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.15;
        passwordField = new JPasswordField(10);
        panel.add(passwordField, gbc);

        JPanel authButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        loginBtn = new JButton("Login");
        loginBtn.addActionListener(e -> onLogin());
        registerBtn = new JButton("Register");
        registerBtn.addActionListener(e -> onRegister());
        logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> onLogout());
        authButtons.add(loginBtn);
        authButtons.add(registerBtn);
        authButtons.add(logoutBtn);
        gbc.gridx = 4; gbc.gridy = 1;
        panel.add(authButtons, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 5;
        statusLabel = new JLabel("Not connected");
        panel.add(statusLabel, gbc);

        return panel;
    }

    private JComponent buildMainPanel() {
        mainTabs = new JTabbedPane();
        mainTabs.addTab("Account", buildAccountPanel());
        mainTabs.addTab("Book Market", buildMarketPanel());
        mainTabs.addTab("My Library", buildLibraryPanel());
        mainTabs.addTab("Clubs", buildClubsPanel());
        return mainTabs;
    }

    @SuppressWarnings("unchecked")
    private JPanel buildAccountPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        balanceLabel = new JLabel("Balance: —");
        refreshBalanceBtn = new JButton("Refresh");
        refreshBalanceBtn.addActionListener(e -> refreshBalance());
        top.add(balanceLabel);
        top.add(refreshBalanceBtn);
        panel.add(top, BorderLayout.NORTH);

        JPanel chargePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        chargePanel.setBorder(BorderFactory.createTitledBorder("Charge Account"));
        chargePanel.add(new JLabel("Amount:"));
        chargeField = new JTextField(10);
        chargePanel.add(chargeField);
        chargeBtn = new JButton("Charge");
        chargeBtn.addActionListener(e -> {
            try {
                double amount = Double.parseDouble(chargeField.getText().trim());
                runTask("account_charge", () -> {
                    Map<String, Object> resp = client.chargeAccount(amount);
                    if (isOk(resp)) {
                        Double balance = getDouble((Map<String, Object>) resp.get("data"), "balance");
                        SwingUtilities.invokeLater(() -> balanceLabel.setText("Balance: " + formatMoney(balance)));
                    }
                    return resp;
                });
            } catch (NumberFormatException ex) {
                showError("Enter a valid amount.");
            }
        });
        chargePanel.add(chargeBtn);
        panel.add(chargePanel, BorderLayout.CENTER);
        return panel;
    }

    @SuppressWarnings("unchecked")
    private JPanel buildMarketPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        marketModel = new DefaultTableModel(new String[]{"ID", "Title", "Author", "Pages", "Genre", "Year", "Price"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        marketTable = new JTable(marketModel);
        marketTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(marketTable), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buyBookBtn = new JButton("Buy Selected");
        buyBookBtn.addActionListener(e -> {
            int row = marketTable.getSelectedRow();
            if (row < 0) { showError("Select a book to buy."); return; }
            String bookId = (String) marketModel.getValueAt(row, 0);
            runTask("book_buy", () -> {
                Map<String, Object> resp = client.buyBook(bookId);
                if (isOk(resp)) {
                    Double balance = getDouble((Map<String, Object>) resp.get("data"), "balance");
                    SwingUtilities.invokeLater(() -> {
                        if (balance != null) balanceLabel.setText("Balance: " + formatMoney(balance));
                        refreshLibrary();
                    });
                }
                return resp;
            });
        });

        refreshMarketBtn = new JButton("Refresh Market (Server)");
        refreshMarketBtn.addActionListener(e -> refreshMarketFromServer());

        buttons.add(buyBookBtn);
        buttons.add(refreshMarketBtn);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildLibraryPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        libraryModel = new DefaultTableModel(new String[]{"Book ID", "Title", "Last Page"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        libraryTable = new JTable(libraryModel);
        libraryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(libraryTable), BorderLayout.CENTER);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        libraryFilter = new JComboBox<>(new String[]{"Unread", "Reading", "Read"});
        refreshLibraryBtn = new JButton("Refresh");
        refreshLibraryBtn.addActionListener(e -> refreshLibrary());
        top.add(new JLabel("Show:"));
        top.add(libraryFilter);
        top.add(refreshLibraryBtn);
        panel.add(top, BorderLayout.NORTH);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setBorder(BorderFactory.createTitledBorder("Update Reading Progress"));
        bottom.add(new JLabel("Page:"));
        progressPageField = new JTextField(8);
        bottom.add(progressPageField);
        submitProgressBtn = new JButton("Submit");
        submitProgressBtn.addActionListener(e -> {
            int row = libraryTable.getSelectedRow();
            if (row < 0) { showError("Select a book first."); return; }
            try {
                int page = Integer.parseInt(progressPageField.getText().trim());
                String bookId = (String) libraryModel.getValueAt(row, 0);
                runTask("progress_submit", () -> {
                    Map<String, Object> resp = client.submitProgress(bookId, page);
                    if (isOk(resp)) SwingUtilities.invokeLater(this::refreshLibrary);
                    return resp;
                });
            } catch (NumberFormatException ex) {
                showError("Enter a valid page number.");
            }
        });
        bottom.add(submitProgressBtn);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildClubsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JPanel listPanel = new JPanel(new BorderLayout(8, 8));
        clubsModel = new DefaultTableModel(new String[]{"ID", "Name", "Owner", "Members"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        clubsTable = new JTable(clubsModel);
        clubsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listPanel.add(new JScrollPane(clubsTable), BorderLayout.CENTER);

        JPanel listTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        clubsScope = new JComboBox<>(new String[]{"My Clubs", "All Clubs"});
        refreshClubsBtn = new JButton("Refresh");
        refreshClubsBtn.addActionListener(e -> refreshClubs());
        listTop.add(clubsScope);
        listTop.add(refreshClubsBtn);
        listPanel.add(listTop, BorderLayout.NORTH);

        JPanel listBottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        clubNameField = new JTextField(15);
        createClubBtn = new JButton("Create Club");
        createClubBtn.addActionListener(e -> {
            String name = clubNameField.getText().trim();
            if (name.isEmpty()) { showError("Enter a club name."); return; }
            runTask("club_create", () -> {
                Map<String, Object> resp = client.createClub(name);
                if (isOk(resp)) SwingUtilities.invokeLater(this::refreshClubs);
                return resp;
            });
        });
        joinClubBtn = new JButton("Request Join");
        joinClubBtn.addActionListener(e -> {
            String clubId = selectedClubId();
            if (clubId == null) return;
            runTask("join", () -> client.joinClub(clubId));
        });
        viewClubBtn = new JButton("View Club");
        viewClubBtn.addActionListener(e -> {
            String clubId = selectedClubId();
            if (clubId == null) return;
            runTask("club_view", () -> {
                Map<String, Object> resp = client.viewClub(clubId);
                if (isOk(resp)) SwingUtilities.invokeLater(this::showClubDetails);
                return resp;
            });
        });
        listBottom.add(new JLabel("New club:"));
        listBottom.add(clubNameField);
        listBottom.add(createClubBtn);
        listBottom.add(joinClubBtn);
        listBottom.add(viewClubBtn);
        listPanel.add(listBottom, BorderLayout.SOUTH);

        clubDetailPanel = buildClubDetailPanel();
        clubDetailPanel.setVisible(false);

        clubsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, clubDetailPanel);
        clubsSplitPane.setResizeWeight(0.5);
        panel.add(clubsSplitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildClubDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Club Details"));

        clubDetailLabel = new JLabel("Select a club and click View Club");
        panel.add(clubDetailLabel, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(2, 1, 8, 8));
        membersModel = new DefaultTableModel(new String[]{"Username", "Owner"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        membersTable = new JTable(membersModel);
        center.add(new JScrollPane(membersTable));

        fundraiserArea = new JTextArea(6, 20);
        fundraiserArea.setEditable(false);
        fundraiserArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        center.add(new JScrollPane(fundraiserArea));
        panel.add(center, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(4, 1, 4, 4));

        JPanel memberActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        refreshMembersBtn = new JButton("Refresh Members");
        refreshMembersBtn.addActionListener(e -> refreshMembers());
        removeMemberBtn = new JButton("Remove Selected Member");
        removeMemberBtn.addActionListener(e -> {
            int row = membersTable.getSelectedRow();
            if (row < 0) { showError("Select a member from the list."); return; }
            String userId = (String) membersModel.getValueAt(row, 0);
            String clubId = client.getCurrentClubId();
            if (clubId == null) { showError("Not viewing a club."); return; }
            runTask("member_remove", () -> {
                Map<String, Object> resp = client.removeMember(userId);
                if (isOk(resp)) SwingUtilities.invokeLater(this::refreshMembers);
                return resp;
            });
        });
        memberActions.add(refreshMembersBtn);
        memberActions.add(removeMemberBtn);
        actions.add(memberActions);

        JPanel joinRequestActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        joinUserField = new JTextField(10);
        acceptJoinBtn = new JButton("Accept Join");
        acceptJoinBtn.addActionListener(e -> {
            String userId = joinUserField.getText().trim();
            if (userId.isEmpty()) { showError("Enter a username to accept."); return; }
            String clubId = client.getCurrentClubId();
            if (clubId == null) { showError("Not viewing a club."); return; }
            runTask("accept_join_request", () -> {
                Map<String, Object> resp = client.acceptJoinRequest(userId, clubId);
                if (isOk(resp)) {
                    joinUserField.setText("");
                    SwingUtilities.invokeLater(this::refreshMembers);
                }
                return resp;
            });
        });
        denyJoinBtn = new JButton("Deny Join");
        denyJoinBtn.addActionListener(e -> {
            String userId = joinUserField.getText().trim();
            if (userId.isEmpty()) { showError("Enter a username to deny."); return; }
            String clubId = client.getCurrentClubId();
            if (clubId == null) { showError("Not viewing a club."); return; }
            runTask("deny_join_request", () -> {
                Map<String, Object> resp = client.denyJoinRequest(userId, clubId);
                if (isOk(resp)) joinUserField.setText("");
                return resp;
            });
        });
        joinRequestActions.add(new JLabel("Requesting User:"));
        joinRequestActions.add(joinUserField);
        joinRequestActions.add(acceptJoinBtn);
        joinRequestActions.add(denyJoinBtn);
        actions.add(joinRequestActions);

        JPanel fundraiserActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fundraiserBookField = new JTextField(10);
        createFundraiserBtn = new JButton("Create Fundraiser");
        createFundraiserBtn.addActionListener(e -> {
            String bookId = fundraiserBookField.getText().trim();
            if (bookId.isEmpty()) { showError("Enter a book ID."); return; }
            runTask("fundraiser_create", () -> {
                Map<String, Object> resp = client.createFundraiser(bookId);
                if (isOk(resp)) SwingUtilities.invokeLater(this::refreshFundraiser);
                return resp;
            });
        });
        refreshFundraiserBtn = new JButton("Refresh Fundraiser");
        refreshFundraiserBtn.addActionListener(e -> refreshFundraiser());
        fundraiserActions.add(new JLabel("Book ID:"));
        fundraiserActions.add(fundraiserBookField);
        fundraiserActions.add(createFundraiserBtn);
        fundraiserActions.add(refreshFundraiserBtn);
        actions.add(fundraiserActions);

        JPanel donateActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        donateField = new JTextField(8);
        donateBtn = new JButton("Donate");
        donateBtn.addActionListener(e -> {
            try {
                double amount = Double.parseDouble(donateField.getText().trim());
                runTask("donate", () -> {
                    Map<String, Object> resp = client.donate(amount);
                    if (isOk(resp)) {
                        SwingUtilities.invokeLater(() -> {
                            refreshFundraiser();
                            refreshBalance();
                        });
                    }
                    return resp;
                });
            } catch (NumberFormatException ex) {
                showError("Enter a valid donation amount.");
            }
        });
        exitClubBtn = new JButton("Exit Club View");
        exitClubBtn.addActionListener(e -> {
            client.exitClubView();
            clubDetailPanel.setVisible(false);
            clubDetailLabel.setText("Select a club and click View Club");
            membersModel.setRowCount(0);
            fundraiserArea.setText("");
            clubsSplitPane.setDividerLocation(1.0);
        });
        donateActions.add(new JLabel("Amount:"));
        donateActions.add(donateField);
        donateActions.add(donateBtn);
        donateActions.add(exitClubBtn);
        actions.add(donateActions);

        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private void loadMarketBooksLocal() {
        try {
            List<Map<String, Object>> books = BooksCatalog.loadDefault();
            populateMarketTable(books);
        } catch (IOException ignored) {}
    }

    @SuppressWarnings("unchecked")
    private void refreshMarketFromServer() {
        runTask("books_market_list", () -> {
            Map<String, Object> resp = client.listMarketBooks();
            if (isOk(resp)) {
                List<Object> books = (List<Object>) ((Map<String, Object>) resp.get("data")).get("books");
                SwingUtilities.invokeLater(() -> {
                    marketModel.setRowCount(0);
                    if (books != null) {
                        for (Object item : books) {
                            Map<String, Object> b = (Map<String, Object>) item;
                            marketModel.addRow(new Object[]{
                                    getString(b, "id"),
                                    getString(b, "title"),
                                    getString(b, "author"),
                                    getInt(b, "pages"),
                                    getString(b, "genre"),
                                    getInt(b, "year"),
                                    getDouble(b, "price")
                            });
                        }
                    }
                });
            }
            return resp;
        });
    }

    private void populateMarketTable(List<Map<String, Object>> books) {
        marketModel.setRowCount(0);
        for (Map<String, Object> b : books) {
            marketModel.addRow(new Object[]{
                    getString(b, "id"),
                    getString(b, "title"),
                    getString(b, "author"),
                    getInt(b, "pages"),
                    getString(b, "genre"),
                    getInt(b, "year"),
                    getDouble(b, "price")
            });
        }
    }

    private void onConnect() {
        String host = hostField.getText().trim();
        if (host.isEmpty()) { showError("Enter a host."); return; }
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Enter a valid port.");
            return;
        }
        setEnabled(connectBtn, false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                client.setNotificationHandler(msg -> SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(ClientGui.this, msg, "Notification", JOptionPane.INFORMATION_MESSAGE)));
                client.connect(host, port);
                return null;
            }

            @Override
            protected void done() {
                setEnabled(connectBtn, true);
                try {
                    get();
                    statusLabel.setText("Connected to " + host + ":" + port);
                } catch (Exception e) {
                    statusLabel.setText("Connection failed");
                    showError(e.getMessage() != null ? e.getMessage() : "Could not connect.");
                }
            }
        }.execute();
    }

    @SuppressWarnings("unchecked")
    private void onLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) { showError("Enter username and password."); return; }
        runTask("login", () -> {
            Map<String, Object> resp = client.login(username, password);
            if (isOk(resp)) {
                Double balance = getDouble((Map<String, Object>) resp.get("data"), "balance");
                SwingUtilities.invokeLater(() -> {
                    setLoggedIn(true);
                    balanceLabel.setText("Balance: " + formatMoney(balance));
                    statusLabel.setText("Logged in as " + username);
                    refreshMarketFromServer();
                    refreshLibrary();
                    refreshClubs();
                });
            }
            return resp;
        });
    }

    private void onRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) { showError("Enter username and password."); return; }
        runTask("register", () -> client.register(username, password));
    }

    private void onLogout() {
        runTask("logout", () -> {
            Map<String, Object> resp = client.logout();
            SwingUtilities.invokeLater(() -> {
                setLoggedIn(false);
                statusLabel.setText(client.isConnected() ? "Connected (not logged in)" : "Not connected");
                balanceLabel.setText("Balance: —");
                libraryModel.setRowCount(0);
                clubsModel.setRowCount(0);
                clubDetailPanel.setVisible(false);
                if (clubsSplitPane != null) {
                    clubsSplitPane.setDividerLocation(1.0);
                }
            });
            return resp;
        });
    }

    @SuppressWarnings("unchecked")
    private void refreshBalance() {
        runTask("balance_show", () -> {
            Map<String, Object> resp = client.showBalance();
            if (isOk(resp)) {
                Double balance = getDouble((Map<String, Object>) resp.get("data"), "balance");
                SwingUtilities.invokeLater(() -> balanceLabel.setText("Balance: " + formatMoney(balance)));
            }
            return resp;
        });
    }

    @SuppressWarnings("unchecked")
    private void refreshLibrary() {
        String filter = (String) libraryFilter.getSelectedItem();
        String command = "Unread".equals(filter) ? "library_read_not_list"
                : "Reading".equals(filter) ? "library_reading_list" : "library_read_list";
        runTask(command, () -> {
            Map<String, Object> resp;
            if ("Unread".equals(filter)) resp = client.listLibraryUnread();
            else if ("Reading".equals(filter)) resp = client.listLibraryReading();
            else resp = client.listLibraryRead();
            if (isOk(resp)) {
                List<Object> books = (List<Object>) ((Map<String, Object>) resp.get("data")).get("books");
                SwingUtilities.invokeLater(() -> {
                    libraryModel.setRowCount(0);
                    if (books != null) {
                        for (Object item : books) {
                            Map<String, Object> b = (Map<String, Object>) item;
                            libraryModel.addRow(new Object[]{
                                    getString(b, "bookId"),
                                    getString(b, "title"),
                                    getInt(b, "lastPage")
                            });
                        }
                    }
                });
            }
            return resp;
        });
    }

    @SuppressWarnings("unchecked")
    private void refreshClubs() {
        boolean all = "All Clubs".equals(clubsScope.getSelectedItem());
        runTask(all ? "clubs_list_total" : "clubs_list", () -> {
            Map<String, Object> resp = all ? client.listAllClubs() : client.listMyClubs();
            if (isOk(resp)) {
                List<Object> clubs = (List<Object>) ((Map<String, Object>) resp.get("data")).get("clubs");
                SwingUtilities.invokeLater(() -> {
                    clubsModel.setRowCount(0);
                    if (clubs != null) {
                        for (Object item : clubs) {
                            Map<String, Object> c = (Map<String, Object>) item;
                            clubsModel.addRow(new Object[]{
                                    getString(c, "id"),
                                    getString(c, "name"),
                                    getString(c, "owner"),
                                    getInt(c, "memberCount")
                            });
                        }
                    }
                });
            }
            return resp;
        });
    }

    private void showClubDetails() {
        String clubId = client.getCurrentClubId();
        if (clubId == null) return;
        int row = clubsTable.getSelectedRow();
        String name = row >= 0 ? (String) clubsModel.getValueAt(row, 1) : clubId;
        clubDetailLabel.setText("Viewing club: " + name + " (" + clubId + ")");

        clubDetailPanel.setVisible(true);
        if (clubsSplitPane != null) {
            clubsSplitPane.setDividerLocation(0.5);
            clubsSplitPane.revalidate();
            clubsSplitPane.repaint();
        }

        refreshMembers();
        refreshFundraiser();
    }

    @SuppressWarnings("unchecked")
    private void refreshMembers() {
        if (client.getCurrentClubId() == null) return;
        runTask("members_club_list", () -> {
            Map<String, Object> resp = client.listClubMembers();
            if (isOk(resp)) {
                List<Object> members = (List<Object>) ((Map<String, Object>) resp.get("data")).get("members");
                SwingUtilities.invokeLater(() -> {
                    membersModel.setRowCount(0);
                    if (members != null) {
                        for (Object item : members) {
                            Map<String, Object> m = (Map<String, Object>) item;
                            membersModel.addRow(new Object[]{
                                    getString(m, "username"),
                                    Boolean.TRUE.equals(m.get("owner")) ? "yes" : "no"
                            });
                        }
                    }
                });
            }
            return resp;
        });
    }

    private void refreshFundraiser() {
        if (client.getCurrentClubId() == null) return;
        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                return client.viewFundraiserProgress();
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> resp = get();
                    if (isOk(resp)) {
                        fundraiserArea.setText(prettyGson.toJson(resp.get("data")));
                    } else {
                        String msg = getString(resp, "message");
                        fundraiserArea.setText(msg != null ? msg : "No active fundraiser in this club.");
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private String selectedClubId() {
        int row = clubsTable.getSelectedRow();
        if (row < 0) {
            showError("Select a club first.");
            return null;
        }
        return (String) clubsModel.getValueAt(row, 0);
    }

    private void runTask(String label, TaskAction action) {
        if (!client.isConnected() && !"connect".equals(label)) {
            showError("Connect to the server first.");
            return;
        }
        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                return action.run();
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> resp = get();
                    if (!isOk(resp)) {
                        String message = getString(resp, "message");
                        if (message != null && !message.isEmpty()) showError(message);
                    }
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    String msg = cause.getMessage() != null ? cause.getMessage() : cause.toString();
                    showError(msg);
                    if (cause instanceof IOException && cause.getMessage() != null
                            && cause.getMessage().contains("Connection closed")) {
                        SwingUtilities.invokeLater(() -> {
                            setLoggedIn(false);
                            statusLabel.setText("Disconnected");
                        });
                    }
                }
            }
        }.execute();
    }

    private void setLoggedIn(boolean loggedIn) {
        loginBtn.setEnabled(!loggedIn);
        registerBtn.setEnabled(!loggedIn);
        logoutBtn.setEnabled(loggedIn);
        for (int i = 0; i < mainTabs.getTabCount(); i++) {
            boolean enabled = loggedIn || "Book Market".equals(mainTabs.getTitleAt(i));
            mainTabs.setEnabledAt(i, enabled);
        }
        refreshBalanceBtn.setEnabled(loggedIn);
        chargeBtn.setEnabled(loggedIn);
        buyBookBtn.setEnabled(loggedIn);
        refreshMarketBtn.setEnabled(loggedIn);
        refreshLibraryBtn.setEnabled(loggedIn);
        submitProgressBtn.setEnabled(loggedIn);
        refreshClubsBtn.setEnabled(loggedIn);
        createClubBtn.setEnabled(loggedIn);
        joinClubBtn.setEnabled(loggedIn);
        viewClubBtn.setEnabled(loggedIn);
        refreshMembersBtn.setEnabled(loggedIn);
        removeMemberBtn.setEnabled(loggedIn);
        acceptJoinBtn.setEnabled(loggedIn);
        denyJoinBtn.setEnabled(loggedIn);
        refreshFundraiserBtn.setEnabled(loggedIn);
        createFundraiserBtn.setEnabled(loggedIn);
        donateBtn.setEnabled(loggedIn);
        exitClubBtn.setEnabled(loggedIn);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static void setEnabled(JButton button, boolean enabled) {
        button.setEnabled(enabled);
    }

    private static boolean isOk(Map<String, Object> resp) {
        return resp != null && "ok".equals(resp.get("status"));
    }

    private static String formatMoney(Double value) {
        return value == null ? "—" : String.format("%.2f", value);
    }

    private static String getString(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private static Integer getInt(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private static Double getDouble(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val != null) {
            try { return Double.parseDouble(val.toString()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    @FunctionalInterface
    private interface TaskAction {
        Map<String, Object> run() throws Exception;
    }
}
