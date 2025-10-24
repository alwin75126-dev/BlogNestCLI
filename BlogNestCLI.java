import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BlogNestCLI implements Serializable {
    private static final String DATA_FILE = "blognest.data";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static class Post implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String id;
        private String title;
        private String author;
        private String content;
        private final LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Post(String title, String author, String content) {
            this.id = UUID.randomUUID().toString();
            this.title = title;
            this.author = author;
            this.content = content;
            this.createdAt = LocalDateTime.now();
            this.updatedAt = createdAt;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getContent() { return content; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }

        public void setTitle(String title) { this.title = title; touch(); }
        public void setAuthor(String author) { this.author = author; touch(); }
        public void setContent(String content) { this.content = content; touch(); }
        private void touch() { this.updatedAt = LocalDateTime.now(); }

        public String summary() {
            return String.format("[%s] %s by %s (%s)", id.substring(0,8), title, author, createdAt.format(FMT));
        }

        public String details() {
            return "ID: " + id + System.lineSeparator() +
                   "Title: " + title + System.lineSeparator() +
                   "Author: " + author + System.lineSeparator() +
                   "Created: " + createdAt.format(FMT) + System.lineSeparator() +
                   "Updated: " + updatedAt.format(FMT) + System.lineSeparator() +
                   "Content:" + System.lineSeparator() + content;
        }
    }

    private final List<Post> posts = new ArrayList<>();
    private final Scanner scanner = new Scanner(System.in);

    private void load() {
        File f = new File(DATA_FILE);
        if (!f.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            Object o = ois.readObject();
            if (o instanceof List) {
                List<?> list = (List<?>) o;
                for (Object item : list) {
                    if (item instanceof Post) posts.add((Post) item);
                }
            }
        } catch (Exception e) {
            System.out.println("Could not load data: " + e.getMessage());
        }
    }

    private void save() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(posts);
        } catch (IOException e) {
            System.out.println("Could not save data: " + e.getMessage());
        }
    }

    private void run() {
        load();
        while (true) {
            System.out.println();
            System.out.println("=== BlogNest CLI ===");
            System.out.println("1. List posts");
            System.out.println("2. Create post");
            System.out.println("3. View post");
            System.out.println("4. Update post");
            System.out.println("5. Delete post");
            System.out.println("6. Search by title/author");
            System.out.println("7. Export posts to text file");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1": listPosts(); break;
                case "2": createPost(); break;
                case "3": viewPost(); break;
                case "4": updatePost(); break;
                case "5": deletePost(); break;
                case "6": searchPosts(); break;
                case "7": exportPosts(); break;
                case "0": save(); System.out.println("Goodbye."); return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private void listPosts() {
        if (posts.isEmpty()) {
            System.out.println("No posts found.");
            return;
        }
        for (int i = posts.size() - 1; i >= 0; i--) {
            System.out.println(posts.get(i).summary());
        }
    }

    private void createPost() {
        System.out.print("Title: ");
        String title = scanner.nextLine().trim();
        System.out.print("Author: ");
        String author = scanner.nextLine().trim();
        System.out.println("Enter content (end with a single line with only 'END'):");
        String content = readMultiline();
        Post p = new Post(title, author, content);
        posts.add(p);
        save();
        System.out.println("Post created with ID: " + p.getId());
    }

    private String readMultiline() {
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = scanner.nextLine();
            if ("END".equals(line)) break;
            sb.append(line).append(System.lineSeparator());
        }
        return sb.toString().trim();
    }

    private Post findByIdShort(String shortId) {
        for (Post p : posts) {
            if (p.getId().startsWith(shortId)) return p;
        }
        for (Post p : posts) {
            if (p.getId().equals(shortId)) return p;
        }
        return null;
    }

    private void viewPost() {
        System.out.print("Enter post ID (or first 8 chars): ");
        String id = scanner.nextLine().trim();
        Post p = findByIdShort(id);
        if (p == null) { System.out.println("Post not found."); return; }
        System.out.println();
        System.out.println(p.details());
    }

    private void updatePost() {
        System.out.print("Enter post ID (or first 8 chars) to update: ");
        String id = scanner.nextLine().trim();
        Post p = findByIdShort(id);
        if (p == null) { System.out.println("Post not found."); return; }
        System.out.print("New title (leave blank to keep): ");
        String title = scanner.nextLine();
        if (!title.isBlank()) p.setTitle(title.trim());
        System.out.print("New author (leave blank to keep): ");
        String author = scanner.nextLine();
        if (!author.isBlank()) p.setAuthor(author.trim());
        System.out.println("New content (type 'SKIP' to keep, or enter new content ending with 'END'):");
        String first = scanner.nextLine();
        if ("SKIP".equals(first)) { save(); System.out.println("Post updated."); return; }
        if ("END".equals(first)) {
            p.setContent("");
        } else {
            StringBuilder sb = new StringBuilder();
            if (!first.isBlank()) sb.append(first).append(System.lineSeparator());
            while (true) {
                String line = scanner.nextLine();
                if ("END".equals(line)) break;
                sb.append(line).append(System.lineSeparator());
            }
            p.setContent(sb.toString().trim());
        }
        save();
        System.out.println("Post updated.");
    }

    private void deletePost() {
        System.out.print("Enter post ID (or first 8 chars) to delete: ");
        String id = scanner.nextLine().trim();
        Post p = findByIdShort(id);
        if (p == null) { System.out.println("Post not found."); return; }
        System.out.print("Type DELETE to confirm: ");
        String confirm = scanner.nextLine().trim();
        if ("DELETE".equals(confirm)) {
            posts.remove(p);
            save();
            System.out.println("Post deleted.");
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    private void searchPosts() {
        System.out.print("Enter search keyword: ");
        String q = scanner.nextLine().trim().toLowerCase();
        List<Post> results = new ArrayList<>();
        for (Post p : posts) {
            if (p.getTitle().toLowerCase().contains(q) || p.getAuthor().toLowerCase().contains(q) || p.getContent().toLowerCase().contains(q)) {
                results.add(p);
            }
        }
        if (results.isEmpty()) {
            System.out.println("No matching posts.");
            return;
        }
        for (Post p : results) System.out.println(p.summary());
    }

    private void exportPosts() {
        System.out.print("Export filename (e.g. export.txt): ");
        String fname = scanner.nextLine().trim();
        if (fname.isBlank()) { System.out.println("Invalid filename."); return; }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fname))) {
            for (Post p : posts) {
                bw.write(p.details());
                bw.write(System.lineSeparator());
                bw.write("------------------------------------------------------------");
                bw.write(System.lineSeparator());
            }
            System.out.println("Exported to " + fname);
        } catch (IOException e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new BlogNestCLI().run();
    }
}
