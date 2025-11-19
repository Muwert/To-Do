import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskManager {
    private final List<Task> tasks;
    private int nextId;

    public TaskManager() {
        this.tasks = new ArrayList<>();
        this.nextId = 1;
    }

    public Task addTask(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Описание задачи не может быть пустым");
        }
        Task task = new Task(nextId++, description.trim());
        tasks.add(task);
        return task;
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    public Optional<Task> getTaskById(int id) {
        return tasks.stream()
                .filter(task -> task.getId() == id)
                .findFirst();
    }

    public boolean completeTask(int id) {
        Optional<Task> task = getTaskById(id);
        if (task.isPresent()) {
            task.get().setCompleted(true);
            return true;
        }
        return false;
    }

    public boolean deleteTask(int id) {
        return tasks.removeIf(task -> task.getId() == id);
    }

    public int getTaskCount() {
        return tasks.size();
    }

    public long getCompletedTaskCount() {
        return tasks.stream().filter(Task::isCompleted).count();
    }
}
