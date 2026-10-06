package com.taskhub.application.port.in;

import com.taskhub.domain.model.Task;
import com.taskhub.domain.model.User;

import java.util.List;

public interface AdminUseCase {

    List<User> listUsers();

    List<Task> listTasks();
}
