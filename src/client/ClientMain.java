package client;

import common.Protocol;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

public class ClientMain {

    private final LibraryClient client = new LibraryClient();

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : Protocol.DEFAULT_TCP_PORT;
        new ClientMain().start(host, port);
    }

    private void start(String host, int port) throws Exception {
        client.setNotificationHandler(msg -> {
            System.out.println();
            System.out.println("[notification] " + msg);
            System.out.print("> ");
        });
        client.connect(host, port);

        System.out.println("Connected to " + host + ":" + port + ". Type 'help' for the list of commands.");
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("> ");
        String line;
        while ((line = stdin.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) break;
                try {
                    handleLine(line);
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
            System.out.print("> ");
        }
        client.disconnect();
    }

    private void handleLine(String line) throws Exception {
        String[] parts = line.split("\\s+", 3);
        String cmd = parts[0];
        Map<String, Object> resp;

        switch (cmd) {
            case "help": printHelp(); return;

            case "register":
                requireArgs(parts, 3, "register <username> <password>");
                resp = client.register(parts[1], parts[2]);
                printResponse(resp);
                return;

            case "login":
                requireArgs(parts, 3, "login <username> <password>");
                resp = client.login(parts[1], parts[2]);
                printResponse(resp);
                return;

            case "logout":
                resp = client.logout();
                printResponse(resp);
                return;

            case "account_charge":
                requireArgs(parts, 2, "account_charge <amount>");
                resp = client.chargeAccount(Double.parseDouble(parts[1]));
                printResponse(resp);
                return;

            case "balance_show":
                resp = client.showBalance();
                printResponse(resp);
                return;

            case "books_market_list":
                resp = client.listMarketBooks();
                printResponse(resp);
                return;

            case "book_buy":
                requireArgs(parts, 2, "book_buy <bookId>");
                resp = client.buyBook(parts[1]);
                printResponse(resp);
                return;

            case "progress_submit": {
                requireArgs(parts, 3, "progress_submit <bookId> <page>");
                String[] sub = parts[2].split("\\s+");
                resp = client.submitProgress(parts[1], Integer.parseInt(sub[0]));
                printResponse(resp);
                return;
            }

            case "library_read_not_list":
                resp = client.listLibraryUnread();
                printResponse(resp);
                return;

            case "library_reading_list":
                resp = client.listLibraryReading();
                printResponse(resp);
                return;

            case "library_read_list":
                resp = client.listLibraryRead();
                printResponse(resp);
                return;

            case "club_create":
                requireArgs(parts, 2, "club_create <name>");
                resp = client.createClub(parts.length > 2 ? parts[1] + " " + parts[2] : parts[1]);
                printResponse(resp);
                return;

            case "clubs_list":
                resp = client.listMyClubs();
                printResponse(resp);
                return;

            case "clubs_list_total":
                resp = client.listAllClubs();
                printResponse(resp);
                return;

            case "join":
                requireArgs(parts, 2, "join <clubId>");
                resp = client.joinClub(parts[1]);
                printResponse(resp);
                return;

            case "accept_join_request":
            case "deny_join_request": {
                requireArgs(parts, 3, cmd + " <userId> <clubId>");
                String[] sub = parts[2].split("\\s+");
                resp = cmd.equals("accept_join_request")
                        ? client.acceptJoinRequest(parts[1], sub[0])
                        : client.denyJoinRequest(parts[1], sub[0]);
                printResponse(resp);
                return;
            }

            case "club_view":
                requireArgs(parts, 2, "club_view <clubId>");
                resp = client.viewClub(parts[1]);
                printResponse(resp);
                return;

            case "club_exit":
                client.exitClubView();
                System.out.println("Left club view mode.");
                return;

            case "members_club_list":
                resp = client.listClubMembers();
                printResponse(resp);
                return;

            case "member_remove":
                requireArgs(parts, 2, "member_remove <userId>");
                resp = client.removeMember(parts[1]);
                printResponse(resp);
                return;

            case "fundraiser_create":
                requireArgs(parts, 2, "fundraiser_create <bookId>");
                resp = client.createFundraiser(parts[1]);
                printResponse(resp);
                return;

            case "progress_fundraiser_view":
                resp = client.viewFundraiserProgress();
                printResponse(resp);
                return;

            case "donate":
                requireArgs(parts, 2, "donate <amount>");
                resp = client.donate(Double.parseDouble(parts[1]));
                printResponse(resp);
                return;

            default:
                System.out.println("Unknown command. Type 'help' for the list of commands.");
        }
    }

    private void requireArgs(String[] parts, int min, String usage) {
        if (parts.length < min) throw new IllegalArgumentException("Usage: " + usage);
    }

    private void printResponse(Map<String, Object> resp) {
        System.out.println(LibraryClient.formatResponse(resp));
    }

    private void printHelp() {
        String[] lines = {
                "register <username> <password>",
                "login <username> <password>",
                "logout",
                "account_charge <amount>",
                "balance_show",
                "books_market_list",
                "book_buy <bookId>",
                "progress_submit <bookId> <page>",
                "library_read_not_list | library_reading_list | library_read_list",
                "club_create <name>",
                "clubs_list | clubs_list_total",
                "join <clubId>",
                "accept_join_request <userId> <clubId>",
                "deny_join_request <userId> <clubId>",
                "club_view <clubId>   (enters club view mode)",
                "  members_club_list",
                "  member_remove <userId>",
                "  fundraiser_create <bookId>",
                "  progress_fundraiser_view",
                "  donate <amount>",
                "club_exit",
                "exit | quit"
        };
        for (String l : lines) System.out.println("  " + l);
    }
}
