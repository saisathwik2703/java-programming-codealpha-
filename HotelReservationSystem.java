import java.io.*;
import java.util.*;

/**
 * CodeAlpha Java Programming Internship - Task 4
 * Hotel Reservation System
 *
 * Console-based system to search, book, and manage hotel rooms.
 * Rooms are categorized (Standard, Deluxe, Suite). Users can make and
 * cancel reservations, run a payment simulation, and view booking
 * details. Data (rooms + reservations) persists to a file (hotel_data.txt)
 * using File I/O.
 */
public class HotelReservationSystem {

    private static final String DATA_FILE = "hotel_data.txt";
    private final Hotel hotel = new Hotel();
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        HotelReservationSystem app = new HotelReservationSystem();
        app.hotel.initializeRooms();
        app.loadData();
        app.run();
        app.saveData();
        System.out.println("\nThank you for using CodeAlpha Hotel Reservation System. Data saved.");
    }

    private void run() {
        printBanner();
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> hotel.displayAllRooms();
                case "2" -> searchAvailableRooms();
                case "3" -> bookRoom();
                case "4" -> cancelReservation();
                case "5" -> viewReservationDetails();
                case "6" -> hotel.displayAllReservations();
                case "7" -> {
                    saveData();
                    System.out.println("Data saved to " + DATA_FILE);
                }
                case "0" -> running = false;
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private void printBanner() {
        System.out.println("=========================================");
        System.out.println("  HOTEL RESERVATION SYSTEM - CodeAlpha    ");
        System.out.println("=========================================");
    }

    private void printMenu() {
        System.out.println("\n--- MENU ---");
        System.out.println("1. View All Rooms");
        System.out.println("2. Search Available Rooms by Category");
        System.out.println("3. Book a Room");
        System.out.println("4. Cancel Reservation");
        System.out.println("5. View Reservation Details");
        System.out.println("6. View All Reservations");
        System.out.println("7. Save Data Now");
        System.out.println("0. Exit");
        System.out.print("Enter your choice: ");
    }

    private void searchAvailableRooms() {
        System.out.println("Categories: STANDARD, DELUXE, SUITE");
        System.out.print("Enter category to search (or press Enter for all): ");
        String input = scanner.nextLine().trim().toUpperCase();
        RoomCategory category = null;
        if (!input.isEmpty()) {
            try {
                category = RoomCategory.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.out.println("Unknown category, showing all available rooms.");
            }
        }
        hotel.displayAvailableRooms(category);
    }

    private void bookRoom() {
        hotel.displayAvailableRooms(null);
        System.out.print("Enter room number to book: ");
        int roomNumber = readPositiveInt();
        Room room = hotel.getRoom(roomNumber);
        if (room == null) {
            System.out.println("Room not found.");
            return;
        }
        if (!room.isAvailable()) {
            System.out.println("Room " + roomNumber + " is already booked.");
            return;
        }

        System.out.print("Enter guest name: ");
        String guestName = scanner.nextLine().trim();
        if (guestName.isEmpty()) {
            System.out.println("Guest name cannot be empty.");
            return;
        }
        System.out.print("Enter number of nights: ");
        int nights = readPositiveInt();

        double totalCost = room.getPricePerNight() * nights;
        System.out.printf("Room %d (%s) costs $%.2f/night. Total for %d night(s): $%.2f%n",
                room.getRoomNumber(), room.getCategory(), room.getPricePerNight(), nights, totalCost);

        System.out.print("Proceed with payment simulation? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!confirm.equals("yes") && !confirm.equals("y")) {
            System.out.println("Booking cancelled.");
            return;
        }

        boolean paymentSuccess = simulatePayment(totalCost);
        if (!paymentSuccess) {
            System.out.println("Payment failed. Booking not completed.");
            return;
        }

        Reservation reservation = hotel.createReservation(room, guestName, nights, totalCost);
        System.out.println("Payment successful! Booking confirmed.");
        System.out.println(reservation.getDetails());
    }

    /** Simulates a payment gateway call (always succeeds for this simulation). */
    private boolean simulatePayment(double amount) {
        System.out.printf("Processing payment of $%.2f...%n", amount);
        try {
            Thread.sleep(400); // brief simulated delay
        } catch (InterruptedException ignored) {
        }
        System.out.println("Payment approved. Confirmation code: PAY-" + (1000 + new Random().nextInt(9000)));
        return true;
    }

    private void cancelReservation() {
        System.out.print("Enter reservation ID to cancel: ");
        String id = scanner.nextLine().trim();
        boolean cancelled = hotel.cancelReservation(id);
        System.out.println(cancelled ? "Reservation " + id + " has been cancelled and the room is now available."
                : "Reservation ID not found.");
    }

    private void viewReservationDetails() {
        System.out.print("Enter reservation ID: ");
        String id = scanner.nextLine().trim();
        Reservation reservation = hotel.getReservation(id);
        if (reservation == null) {
            System.out.println("Reservation not found.");
        } else {
            System.out.println(reservation.getDetails());
        }
    }

    private int readPositiveInt() {
        while (true) {
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value > 0) return value;
                System.out.print("Please enter a positive number: ");
            } catch (NumberFormatException e) {
                System.out.print("Invalid number, try again: ");
            }
        }
    }

    private void saveData() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DATA_FILE))) {
            for (Room room : hotel.getAllRooms()) {
                writer.println("ROOM," + room.getRoomNumber() + "," + room.getCategory() + ","
                        + room.getPricePerNight() + "," + room.isAvailable());
            }
            for (Reservation r : hotel.getAllReservations()) {
                writer.println("RES," + r.toCsv());
            }
        } catch (IOException e) {
            System.out.println("Error saving data: " + e.getMessage());
        }
    }

    private void loadData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",");
                if (parts[0].equals("ROOM")) {
                    int roomNum = Integer.parseInt(parts[1]);
                    boolean available = Boolean.parseBoolean(parts[4]);
                    hotel.setRoomAvailability(roomNum, available);
                } else if (parts[0].equals("RES")) {
                    hotel.loadReservation(Reservation.fromCsv(parts));
                }
            }
            System.out.println("Loaded existing hotel data from " + DATA_FILE);
        } catch (IOException e) {
            System.out.println("Error loading data: " + e.getMessage());
        }
    }

    // ===================== Domain Classes =====================

    enum RoomCategory { STANDARD, DELUXE, SUITE }

    /** Represents a single hotel room. */
    static class Room {
        private final int roomNumber;
        private final RoomCategory category;
        private final double pricePerNight;
        private boolean available = true;

        Room(int roomNumber, RoomCategory category, double pricePerNight) {
            this.roomNumber = roomNumber;
            this.category = category;
            this.pricePerNight = pricePerNight;
        }

        int getRoomNumber() { return roomNumber; }
        RoomCategory getCategory() { return category; }
        double getPricePerNight() { return pricePerNight; }
        boolean isAvailable() { return available; }
        void setAvailable(boolean available) { this.available = available; }
    }

    /** Represents a confirmed booking. */
    static class Reservation {
        private final String id;
        private final int roomNumber;
        private final RoomCategory category;
        private final String guestName;
        private final int nights;
        private final double totalCost;

        Reservation(String id, int roomNumber, RoomCategory category, String guestName, int nights, double totalCost) {
            this.id = id;
            this.roomNumber = roomNumber;
            this.category = category;
            this.guestName = guestName;
            this.nights = nights;
            this.totalCost = totalCost;
        }

        String getId() { return id; }
        int getRoomNumber() { return roomNumber; }

        String getDetails() {
            return String.format("""
                    ----- Reservation Details -----
                    Reservation ID: %s
                    Guest Name:     %s
                    Room Number:    %d (%s)
                    Nights Booked:  %d
                    Total Cost:     $%.2f
                    --------------------------------""",
                    id, guestName, roomNumber, category, nights, totalCost);
        }

        String toCsv() {
            return id + "," + roomNumber + "," + category + "," + guestName + "," + nights + "," + totalCost;
        }

        static Reservation fromCsv(String[] parts) {
            // parts[0]="RES", 1=id,2=roomNumber,3=category,4=guestName,5=nights,6=totalCost
            return new Reservation(parts[1], Integer.parseInt(parts[2]), RoomCategory.valueOf(parts[3]),
                    parts[4], Integer.parseInt(parts[5]), Double.parseDouble(parts[6]));
        }
    }

    /** Manages the collection of rooms and reservations for the hotel. */
    static class Hotel {
        private final Map<Integer, Room> rooms = new TreeMap<>();
        private final Map<String, Reservation> reservations = new LinkedHashMap<>();
        private int reservationCounter = 1;

        void initializeRooms() {
            int roomNum = 101;
            for (int i = 0; i < 5; i++) rooms.put(roomNum, new Room(roomNum++, RoomCategory.STANDARD, 60.00));
            roomNum = 201;
            for (int i = 0; i < 3; i++) rooms.put(roomNum, new Room(roomNum++, RoomCategory.DELUXE, 110.00));
            roomNum = 301;
            for (int i = 0; i < 2; i++) rooms.put(roomNum, new Room(roomNum++, RoomCategory.SUITE, 220.00));
        }

        Room getRoom(int roomNumber) { return rooms.get(roomNumber); }
        Collection<Room> getAllRooms() { return rooms.values(); }
        Collection<Reservation> getAllReservations() { return reservations.values(); }

        void setRoomAvailability(int roomNumber, boolean available) {
            Room room = rooms.get(roomNumber);
            if (room != null) room.setAvailable(available);
        }

        void displayAllRooms() {
            System.out.printf("%n%-8s %-10s %-12s %-10s%n", "Room#", "Category", "Price/Night", "Status");
            System.out.println("-".repeat(45));
            for (Room r : rooms.values()) {
                System.out.printf("%-8d %-10s $%-11.2f %-10s%n", r.getRoomNumber(), r.getCategory(),
                        r.getPricePerNight(), r.isAvailable() ? "Available" : "Booked");
            }
        }

        void displayAvailableRooms(RoomCategory filter) {
            System.out.printf("%n%-8s %-10s %-12s%n", "Room#", "Category", "Price/Night");
            System.out.println("-".repeat(35));
            boolean any = false;
            for (Room r : rooms.values()) {
                if (!r.isAvailable()) continue;
                if (filter != null && r.getCategory() != filter) continue;
                System.out.printf("%-8d %-10s $%-11.2f%n", r.getRoomNumber(), r.getCategory(), r.getPricePerNight());
                any = true;
            }
            if (!any) System.out.println("No available rooms match your criteria.");
        }

        Reservation createReservation(Room room, String guestName, int nights, double totalCost) {
            room.setAvailable(false);
            String id = "RES" + String.format("%04d", reservationCounter++);
            Reservation reservation = new Reservation(id, room.getRoomNumber(), room.getCategory(), guestName, nights, totalCost);
            reservations.put(id, reservation);
            return reservation;
        }

        void loadReservation(Reservation reservation) {
            reservations.put(reservation.getId(), reservation);
            // keep counter ahead of loaded IDs so new bookings don't collide
            try {
                int num = Integer.parseInt(reservation.getId().replace("RES", ""));
                if (num >= reservationCounter) reservationCounter = num + 1;
            } catch (NumberFormatException ignored) {
            }
        }

        boolean cancelReservation(String id) {
            Reservation reservation = reservations.remove(id);
            if (reservation == null) return false;
            Room room = rooms.get(reservation.getRoomNumber());
            if (room != null) room.setAvailable(true);
            return true;
        }

        Reservation getReservation(String id) { return reservations.get(id); }

        void displayAllReservations() {
            if (reservations.isEmpty()) {
                System.out.println("No reservations yet.");
                return;
            }
            System.out.printf("%n%-10s %-20s %-8s %-10s %-8s %-10s%n",
                    "ResID", "Guest", "Room#", "Category", "Nights", "Total");
            System.out.println("-".repeat(70));
            for (Reservation r : reservations.values()) {
                System.out.printf("%-10s %-20s %-8d %-10s %-8d $%-10.2f%n",
                        r.getId(), r.guestName, r.getRoomNumber(), r.category, r.nights, r.totalCost);
            }
        }
    }
}
