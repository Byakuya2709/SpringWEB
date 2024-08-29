/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ctu.demo.controller;

import ctu.demo.dto.TaskDTO;
import ctu.demo.model.Task;
import ctu.demo.respone.ResponseHandler;
import ctu.demo.service.TaskService;
import ctu.demo.service.UserService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author admin
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private TaskService taskService;
    @Autowired
    private UserService userService;
    
    @PostMapping("/task")
    public ResponseEntity<?> createTask(@RequestBody TaskDTO task ){
        Task savedTask;
        try{
             savedTask = taskService.saveTask(task);
        }catch(Exception e){
           return ResponseHandler.resBuilder("Lỗi khi tạo một task mới", HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
      
         return ResponseHandler.resBuilder("Tạo task thành công", HttpStatus.CREATED,Task.toTaskDTO(savedTask));
    }
    @GetMapping
    public ResponseEntity<?> getAllTasks() {
        List<Task> tasks = taskService.getAllTasks();
        List<TaskDTO> DTO = new ArrayList<>();
        for (Task task : tasks)
            DTO.add(Task.toTaskDTO(task));
        return ResponseHandler.resBuilder("Lấy tất cả các task thành công", HttpStatus.OK,DTO);
    }

    // Lấy Task theo ID
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getTasksByUserId(@PathVariable Long userId) {
        List<Task> tasks = userService.getTasksByUserId(userId);
        List<TaskDTO> taskDTOs = new ArrayList<>();
        for (Task task : tasks) {
            taskDTOs.add(Task.toTaskDTO(task));
        }
        return ResponseHandler.resBuilder("Lấy tất cả các task theo userID thành công", HttpStatus.OK, taskDTOs);
    }

    // Cập nhật một Task
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id, @RequestBody TaskDTO taskDTO) {
        try {
            Task existingTask = taskService.getTaskById(id).orElseThrow(() -> new RuntimeException("Task không tồn tại"));
            // Cập nhật thông tin cho task
            existingTask.setTitle(taskDTO.getTitle());
            existingTask.setDescription(taskDTO.getDescription());
            existingTask.setDate(taskDTO.getDate());
            existingTask.setStatus(taskDTO.getStatus());
            existingTask.setUser(userService.getUserById(taskDTO.getUserID()));
            Task updatedTask = taskService.saveTask(existingTask);
            return ResponseHandler.resBuilder("Cập nhật task thành công", HttpStatus.OK, Task.toTaskDTO(updatedTask));
        } catch (Exception e) {
            return ResponseHandler.resBuilder("Lỗi khi cập nhật task: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    // Xóa một Task
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
        try {
            taskService.deleteTask(id);
            return ResponseHandler.resBuilder("Xóa task thành công", HttpStatus.NO_CONTENT, null);
        } catch (Exception e) {
            return ResponseHandler.resBuilder("Lỗi khi xóa task: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }
}
