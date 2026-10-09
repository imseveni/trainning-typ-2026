package com.example.demo.controller;

import com.example.demo.model.ApiError;
import com.example.demo.model.PageResponse;
import com.example.demo.model.Student;
import com.example.demo.service.StudentService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // 1. GET /students?page=1&size=10
    @GetMapping
    public ResponseEntity<?> getStudents(
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

        PageResponse<Student> response = studentService.getAll(page, size);

        return ResponseEntity.ok(response);
    }

    // 2. GET /students/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getStudent(@PathVariable Long id) {
        Student student = studentService.getById(id);

        if (student == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ApiError(
                            404,
                            "Student not found",
                            "Student with id "
                                    + id
                                    + " does not exist"));
        }

        return ResponseEntity.ok(student);
    }

    // 3. POST /students
    @PostMapping
    public ResponseEntity<?> createStudent(@RequestBody Student student) {
        ApiError error = validateStudent(student);

        if (error != null) {
            return ResponseEntity
                    .badRequest()
                    .body(error);
        }

        Student created = studentService.create(student);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    // 4. PUT /students/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> updateStudent(
            @PathVariable Long id,
            @RequestBody Student student) {

        ApiError error = validateStudent(student);

        if (error != null) {
            return ResponseEntity
                    .badRequest()
                    .body(error);
        }

        Student updated = studentService.update(id, student);

        if (updated == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ApiError(
                            404,
                            "Student not found",
                            "Student with id "
                                    + id
                                    + " does not exist"));
        }

        return ResponseEntity.ok(updated);
    }

    // 5. DELETE /students/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long id) {
        boolean deleted = studentService.delete(id);

        if (!deleted) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ApiError(
                            404,
                            "Student not found",
                            "Student with id "
                                    + id
                                    + " does not exist"));
        }

        return ResponseEntity
                .noContent()
                .build();
    }

    // Kiểm tra dữ liệu Student
    private ApiError validateStudent(Student student) {
        if (student.getName() == null || student.getName().isBlank()) {
            return new ApiError(
                    400,
                    "Bad request",
                    "name must not be blank");
        }

        if (student.getAge() == null || student.getAge() < 1) {
            return new ApiError(
                    400,
                    "Bad request",
                    "age must be greater than 0");
        }

        return null;
    }

    @PutMapping("/{studentId}/department/{departmentId}")
    public ResponseEntity<?> assignDepartment(
            @PathVariable Long studentId,
            @PathVariable Long departmentId) {

        Student student = studentService.assignDepartment(studentId, departmentId);

        if (student == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ApiError(
                            404,
                            "Not found",
                            "Student or Department does not exist"));
        }

        return ResponseEntity.ok(student);
    }
}