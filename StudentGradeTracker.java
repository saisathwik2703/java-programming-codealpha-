import java.io.*;
import java.util.*;

/**
 * CodeAlpha Java Programming Internship - Task 1
 * Student Grade Tracker
 *
 * Console-based application to input and manage student grades.
 * Stores students in an ArrayList, calculates average/highest/lowest
 * scores, displays a summary report, and persists data to a file
 * (students.txt) so records survive between runs.
 */
public class StudentGradeTracker {

    private static final String DATA_FILE = "students.txt";
    private final List<Student> students = new ArrayList<>();
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        StudentGradeTracker app = new StudentGradeTracker();
        app.loadFromFile();
        app.run();
        app.saveToFile();
        System.out.println("\nGoodbye! Your data has been saved to " + DATA_FILE);
    }

    private void run() {
        printBanner();
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> addStudent();
                case "2" -> viewAllStudents();
                case "3" -> showSummaryReport();
                case "4" -> searchStudent();
                case "5" -> deleteStudent();
                case "6" -> {
                    saveToFile();
                    System.out.println("Data saved successfully to " + DATA_FILE);
                }
                case "0" -> running = false;
                default -> System.out.println("Invalid choice. Please select a valid option.");
            }
        }
    }

    private void printBanner() {
        System.out.println("=========================================");
        System.out.println("     STUDENT GRADE TRACKER - CodeAlpha    ");
        System.out.println("=========================================");
    }

    private void printMenu() {
        System.out.println("\n--- MENU ---");
        System.out.println("1. Add Student & Grades");
        System.out.println("2. View All Students");
        System.out.println("3. Summary Report (Average/Highest/Lowest)");
        System.out.println("4. Search Student by Name");
        System.out.println("5. Delete Student");
        System.out.println("6. Save Data Now");
        System.out.println("0. Exit");
        System.out.print("Enter your choice: ");
    }

    private void addStudent() {
        System.out.print("Enter student name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty.");
            return;
        }

        List<Double> scores = new ArrayList<>();
        System.out.print("How many subjects/scores to enter? ");
        int count = readPositiveInt();
        for (int i = 1; i <= count; i++) {
            double score = readScore(i);
            scores.add(score);
        }

        Student student = new Student(name, scores);
        students.add(student);
        System.out.printf("Added %s with average score %.2f%n", name, student.getAverage());
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

    private double readScore(int index) {
        while (true) {
            System.out.printf("  Score for subject %d (0-100): ", index);
            try {
                double value = Double.parseDouble(scanner.nextLine().trim());
                if (value >= 0 && value <= 100) return value;
                System.out.println("  Score must be between 0 and 100.");
            } catch (NumberFormatException e) {
                System.out.println("  Invalid number, try again.");
            }
        }
    }

    private void viewAllStudents() {
        if (students.isEmpty()) {
            System.out.println("No students recorded yet.");
            return;
        }
        System.out.println("\n%-20s %-10s %-10s %-10s %-8s"
                .formatted("Name", "Average", "Highest", "Lowest", "Grade"));
        System.out.println("-".repeat(65));
        for (Student s : students) {
            System.out.printf("%-20s %-10.2f %-10.2f %-10.2f %-8s%n",
                    s.getName(), s.getAverage(), s.getHighest(), s.getLowest(), s.getLetterGrade());
        }
    }

    private void showSummaryReport() {
        if (students.isEmpty()) {
            System.out.println("No students recorded yet.");
            return;
        }

        double classAverage = students.stream().mapToDouble(Student::getAverage).average().orElse(0);
        Student topStudent = students.stream().max(Comparator.comparingDouble(Student::getAverage)).orElse(null);
        Student bottomStudent = students.stream().min(Comparator.comparingDouble(Student::getAverage)).orElse(null);

        System.out.println("\n========== SUMMARY REPORT ==========");
        System.out.println("Total students: " + students.size());
        System.out.printf("Class average:  %.2f%n", classAverage);
        if (topStudent != null)
            System.out.printf("Top performer:    %s (%.2f)%n", topStudent.getName(), topStudent.getAverage());
        if (bottomStudent != null)
            System.out.printf("Needs improvement: %s (%.2f)%n", bottomStudent.getName(), bottomStudent.getAverage());
        System.out.println("=====================================");

        viewAllStudents();
    }

    private void searchStudent() {
        System.out.print("Enter name to search: ");
        String name = scanner.nextLine().trim();
        Optional<Student> found = students.stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst();
        if (found.isPresent()) {
            Student s = found.get();
            System.out.printf("%s -> Average: %.2f | Highest: %.2f | Lowest: %.2f | Grade: %s%n",
                    s.getName(), s.getAverage(), s.getHighest(), s.getLowest(), s.getLetterGrade());
            System.out.println("Scores: " + s.getScores());
        } else {
            System.out.println("Student not found.");
        }
    }

    private void deleteStudent() {
        System.out.print("Enter name of student to delete: ");
        String name = scanner.nextLine().trim();
        boolean removed = students.removeIf(s -> s.getName().equalsIgnoreCase(name));
        System.out.println(removed ? "Student removed." : "Student not found.");
    }

    private void saveToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DATA_FILE))) {
            for (Student s : students) {
                StringBuilder sb = new StringBuilder(s.getName());
                for (double score : s.getScores()) {
                    sb.append(",").append(score);
                }
                writer.println(sb);
            }
        } catch (IOException e) {
            System.out.println("Error saving data: " + e.getMessage());
        }
    }

    private void loadFromFile() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",");
                String name = parts[0];
                List<Double> scores = new ArrayList<>();
                for (int i = 1; i < parts.length; i++) {
                    scores.add(Double.parseDouble(parts[i]));
                }
                students.add(new Student(name, scores));
            }
            if (!students.isEmpty()) {
                System.out.println("Loaded " + students.size() + " student record(s) from " + DATA_FILE);
            }
        } catch (IOException e) {
            System.out.println("Error loading data: " + e.getMessage());
        }
    }

    /** Represents a single student and their subject scores. */
    static class Student {
        private final String name;
        private final List<Double> scores;

        Student(String name, List<Double> scores) {
            this.name = name;
            this.scores = scores;
        }

        String getName() { return name; }
        List<Double> getScores() { return scores; }

        double getAverage() {
            return scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        }

        double getHighest() {
            return scores.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        }

        double getLowest() {
            return scores.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        }

        String getLetterGrade() {
            double avg = getAverage();
            if (avg >= 90) return "A";
            if (avg >= 80) return "B";
            if (avg >= 70) return "C";
            if (avg >= 60) return "D";
            return "F";
        }
    }
}
