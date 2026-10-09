package com.example.demo.controller;

import com.example.demo.model.Department;
import com.example.demo.model.Student;
import com.example.demo.repository.DepartmentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/departments")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    public DepartmentController(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    // Thêm Department
    @PostMapping
    public ResponseEntity<Department> createDepartment(@RequestBody Department department) {
        department.setId(null);

        Department created = departmentRepository.save(department);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    // Lấy danh sách Student của Department
    @GetMapping("/{id}/students")
    public ResponseEntity<?> getStudents(@PathVariable Long id) {
        Department department = departmentRepository
                .findById(id)
                .orElse(null);

        if (department == null) {
            return ResponseEntity.notFound().build();
        }

        List<Student> students = department.getStudents();

        return ResponseEntity.ok(students);
    }
}