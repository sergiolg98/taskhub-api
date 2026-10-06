package com.taskhub.admin.application.port.in;

import com.taskhub.auth.domain.model.User;
import com.taskhub.task.domain.model.Task;

import java.util.List;

public interface AdminUseCase {

    List<User> listUsers();

    List<Task> listTasks();
}
