package com.example.demo.controller;

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

    // GET /students/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getStudent(@PathVariable Long id) {

        Student student = studentService.getById(id);

        if (student == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Student not found");
        }

        return ResponseEntity.ok(student);
    }

    // POST /students
    @PostMapping
    public ResponseEntity<Student> createStudent(
            @RequestBody Student student) {

        Student created = studentService.create(student);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    // PUT /students/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> updateStudent(
            @PathVariable Long id,
            @RequestBody Student student) {

        Student updated = studentService.update(id, student);

        if (updated == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Student not found");
        }

        return ResponseEntity.ok(updated);
    }

    // DELETE /students/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStudent(
            @PathVariable Long id) {

        boolean deleted = studentService.delete(id);

        if (!deleted) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Student not found");
        }

        return ResponseEntity
                .noContent()
                .build();
    }
}