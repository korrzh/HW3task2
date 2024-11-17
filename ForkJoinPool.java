package PR3.task2;

import java.io.File;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RecursiveTask;

public class ForkJoinPool {
    public static void main(String[] args) {
        // Введення директорії та розширення файлів
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter directory path: ");
        String directoryPath = scanner.nextLine();

        // Список дозволених розширень
        List<String> validExtensions = Arrays.asList(".pdf", ".txt", ".jpg", ".png", ".docx");

        String extension = "";
        boolean validExtension = false;

        // Перевірка на допустиме розширення
        while (!validExtension) {
            System.out.print("Enter file extension (e.g. .pdf): ");
            extension = scanner.nextLine().trim();

            // Перевірка, чи є введене розширення в списку дозволених
            if (validExtensions.contains(extension)) {
                validExtension = true;
            } else {
                System.out.println("Invalid extension! Please enter a valid extension from the list: " + validExtensions);
            }
        }

        // Створення ForkJoinPool для паралельного виконання задач
        java.util.concurrent.ForkJoinPool pool = new java.util.concurrent.ForkJoinPool();

        // Виконання ForkJoinTask
        int result = pool.invoke(new ForkJoinPool.FileSearchTask(new File(directoryPath), extension, new HashSet<>()));

        // Виведення результату
        System.out.println("Total number of matching files: " + result);
    }

    // Рекурсивна задача для пошуку файлів з певним розширенням
    static class FileSearchTask extends RecursiveTask<Integer> {
        private File directory;
        private String extension;
        private File[] files;
        private Set<String> processedFiles; // Глобальний набір для уникнення дублювань
        private BlockingQueue<File> queue;  // Черга для потоку

        public FileSearchTask(File directory, String extension, Set<String> processedFiles) {
            this.directory = directory;
            this.extension = extension;
            this.files = directory.listFiles(); // Отримуємо всі файли в директорії
            this.processedFiles = processedFiles; // Спільний набір для перевірки унікальності
            this.queue = new LinkedBlockingQueue<>(); // Створення черги
        }

        @Override
        protected Integer compute() {
            int count = 0;

            // Перевірка чи існує директорія і чи вона доступна
            if (directory.exists() && directory.isDirectory()) {
                System.out.println(Thread.currentThread().getName() + " is processing directory: " + directory.getAbsolutePath());
                System.out.println("Queue in " + Thread.currentThread().getName() + " before processing: " + queue);  // Виведення черги перед обробкою

                if (files != null) {
                    // Розділення задач, якщо файлів більше 3
                    if (files.length > 3) {
                        int mid = files.length / 2;

                        // Ліва частина
                        ForkJoinPool.FileSearchTask leftTask = new ForkJoinPool.FileSearchTask(directory, extension, processedFiles);
                        leftTask.files = new File[mid];
                        System.arraycopy(files, 0, leftTask.files, 0, mid);

                        // Права частина
                        ForkJoinPool.FileSearchTask rightTask = new ForkJoinPool.FileSearchTask(directory, extension, processedFiles);
                        rightTask.files = new File[files.length - mid];
                        System.arraycopy(files, mid, rightTask.files, 0, files.length - mid);

                        System.out.println(Thread.currentThread().getName() + " is splitting task at " + directory.getAbsolutePath());

                        // Паралельне виконання задач
                        leftTask.fork();
                        count += rightTask.compute(); // Обробка правої частини
                        count += leftTask.join(); // Очікування результату лівої частини

                    } else {
                        // Обробка файлів у поточному потоці
                        for (File file : files) {
                            if (file != null && processedFiles.add(file.getAbsolutePath())) { // Унікальність файлу
                                queue.add(file);  // Додаємо файл у чергу
                                System.out.println(Thread.currentThread().getName() + " added file to queue: " + file.getName());

                                if (file.isDirectory()) {
                                    // Якщо це підкаталог, рекурсивно викликаємо для цього підкаталогу
                                    System.out.println(Thread.currentThread().getName() + " is processing subdirectory: " + file.getAbsolutePath());
                                    count += new ForkJoinPool.FileSearchTask(file, extension, processedFiles).fork().join();
                                } else {
                                    // Перевіряємо чи файл має потрібне розширення
                                    if (file.getName().endsWith(extension)) {
                                        System.out.println(Thread.currentThread().getName() + " found file: " + file.getAbsolutePath());
                                        count++;
                                    }
                                }
                            }

                            // Виведення поточного стану черги після кожної операції
                            System.out.println(Thread.currentThread().getName() + " queue after processing " + file.getName() + ": " + queue);
                        }
                    }
                } else {
                    System.out.println("Unable to read directory or no files found in " + directory.getAbsolutePath());
                }
            } else {
                System.out.println("Directory " + directory.getAbsolutePath() + " is not accessible or does not exist.");
            }

            return count;
        }
    }
}
