package com.example.demo.controller;

import com.example.demo.model.PageResponse;
import com.example.demo.model.ApiError;
import com.example.demo.model.Department;
import com.example.demo.model.Student;
import com.example.demo.service.DepartmentService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(
            DepartmentService departmentService) {

        this.departmentService = departmentService;
    }

    // 1. GET /departments?page=1&size=10
    @GetMapping
    public ResponseEntity<?> getDepartments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (page < 1 || size < 1) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiError(
                            400,
                            "Bad request",
                            "page and size must be greater than 0"));
        }

        PageResponse<Department> response = departmentService.getAll(page, size);

        return ResponseEntity.ok(response);
    }

    // Get Department
    @GetMapping("/{id}")
    public ResponseEntity<?> getDepartment(@PathVariable Long id) {

        Department department = departmentService.getById(id);

        if (department == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ApiError(
                            404,
                            "Department not found",
                            "Department with id " + id + " dose not exist"));
        }

        return ResponseEntity.ok(department);
    }

    // Thêm Department
    @PostMapping
    public ResponseEntity<?> createDepartment(@RequestBody Department department) {

        ApiError error = validateDepartment(department);

        if (error != null) {
            return ResponseEntity
                    .badRequest()
                    .body(error);
        }

        Department created = departmentService.create(department);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    // Update Department
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDepartment(
            @PathVariable Long id,
            @RequestBody Department department) {

        ApiError error = validateDepartment(department);

        if (error != null) {
            return ResponseEntity
                    .badRequest()
                    .body(error);
        }

        Department updated = departmentService.update(id, department);

        if (updated == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ApiError(
                            404,
                            "Department not found",
                            "Department with id " + id + " does not exist"));
        }

        return ResponseEntity.ok(updated);
    }

    // Xóa Department
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDepartment(@PathVariable Long id) {

        boolean delete = departmentService.delete(id);

        if (!delete) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ApiError(
                            404,
                            "Department not found",
                            "Department with id " + id + " dose not exist"));
        }

        return ResponseEntity
                .noContent()
                .build();
    }

    private ApiError validateDepartment(Department department) {

        if (department.getName() == null || department.getName().isBlank()) {

            return new ApiError(
                    400,
                    "Bad request",
                    "department name must not be blank");
        }

        return null;
    }

    // Lấy danh sách Student của Department
    @GetMapping("/{id}/students")
    public ResponseEntity<?> getStudents(@PathVariable Long id) {

        List<Student> students = departmentService.getStudents(id);

        if (students == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ApiError(
                            404,
                            "Department not found",
                            "Department with id "
                                    + id
                                    + " does not exist"));
        }

        return ResponseEntity.ok(students);
    }
}