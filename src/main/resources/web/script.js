class TaskManagerUI {
    constructor() {
        this.tasks = [];
        this.init();
    }

    init() {
        this.loadTasks();
        this.setupEventListeners();
    }

    setupEventListeners() {
        const taskInput = document.getElementById('taskInput');
        taskInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.addTask();
            }
        });
    }

    async loadTasks() {
        try {
            const response = await fetch('/api/tasks');
            const data = await response.json();
            this.tasks = data.tasks || [];
            this.renderTasks();
            this.updateStats();
        } catch (error) {
            this.showError('Ошибка при загрузке задач: ' + error.message);
        }
    }

    async addTask() {
        const input = document.getElementById('taskInput');
        const description = input.value.trim();

        if (!description) {
            this.showError('Введите описание задачи');
            return;
        }

        try {
            const response = await fetch('/api/tasks', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ description: description })
            });

            const data = await response.json();

            if (data.success) {
                input.value = '';
                this.hideError();
                await this.loadTasks();
            } else {
                this.showError(data.error || 'Ошибка при добавлении задачи');
            }
        } catch (error) {
            this.showError('Ошибка при добавлении задачи: ' + error.message);
        }
    }

    async completeTask(taskId) {
        try {
            const response = await fetch(`/api/tasks/complete?id=${taskId}`, {
                method: 'POST'
            });

            const data = await response.json();

            if (data.success) {
                await this.loadTasks();
            } else {
                this.showError('Ошибка при выполнении задачи');
            }
        } catch (error) {
            this.showError('Ошибка при выполнении задачи: ' + error.message);
        }
    }

    async deleteTask(taskId) {
        if (!confirm('Вы уверены, что хотите удалить эту задачу?')) {
            return;
        }

        try {
            const response = await fetch(`/api/tasks/delete?id=${taskId}`, {
                method: 'POST'
            });

            const data = await response.json();

            if (data.success) {
                await this.loadTasks();
            } else {
                this.showError('Ошибка при удалении задачи');
            }
        } catch (error) {
            this.showError('Ошибка при удалении задачи: ' + error.message);
        }
    }

    renderTasks() {
        const tasksList = document.getElementById('tasksList');
        
        if (this.tasks.length === 0) {
            tasksList.innerHTML = '<div class="no-tasks">Задачи отсутствуют</div>';
            return;
        }

        tasksList.innerHTML = this.tasks.map(task => `
            <div class="task-item ${task.completed ? 'completed' : ''}">
                <div class="task-content">
                    <div class="task-description">${this.escapeHtml(task.description)}</div>
                </div>
                <div class="task-actions">
                    <button class="complete-btn" 
                            onclick="taskManagerUI.completeTask(${task.id})"
                            ${task.completed ? 'disabled' : ''}>
                        ${task.completed ? '✓ Выполнено' : 'Выполнить'}
                    </button>
                    <button class="delete-btn" 
                            onclick="taskManagerUI.deleteTask(${task.id})">
                        Удалить
                    </button>
                </div>
            </div>
        `).join('');
    }

    updateStats() {
        const totalCount = this.tasks.length;
        const completedCount = this.tasks.filter(task => task.completed).length;

        document.getElementById('totalCount').textContent = totalCount;
        document.getElementById('completedCount').textContent = completedCount;
    }

    showError(message) {
        const errorDiv = document.getElementById('errorMessage');
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
    }

    hideError() {
        const errorDiv = document.getElementById('errorMessage');
        errorDiv.style.display = 'none';
    }

    escapeHtml(unsafe) {
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }
}

const taskManagerUI = new TaskManagerUI();

function addTask() {
    taskManagerUI.addTask();
}