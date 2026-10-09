package com.example.demo.controller;

import com.example.demo.model.PageResponse;
import com.example.demo.model.Student;
import com.example.demo.service.StudentService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // GET /students?page=1&size=10
    @GetMapping
    public ResponseEntity<PageResponse<Student>> getStudents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<Student> students = studentService.getAll();

        int totalElements = students.size();

        int totalPages = (int) Math.ceil((double) totalElements / size);

        int start = (page - 1) * size;

        if (start >= totalElements) {

            PageResponse<Student> response = new PageResponse<>(
                    List.of(),
                    page,
                    size,
                    totalPages,
                    totalElements);

            return ResponseEntity.ok(response);
        }

        int end = Math.min(start + size, totalElements);

        List<Student> data = students.subList(start, end);

        PageResponse<Student> response = new PageResponse<>(
                data,
                page,
                size,
                totalPages,
                totalElements);

        return ResponseEntity.ok(response);
    }

    // GET /students/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudent(
            @PathVariable Long id) {

        Student student = studentService.getById(id);

        if (student == null) {
            return ResponseEntity.notFound().build();
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
    public ResponseEntity<Student> updateStudent(
            @PathVariable Long id,
            @RequestBody Student student) {

        Student updated = studentService.update(id, student);

        if (updated == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updated);
    }

    // DELETE /students/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(
            @PathVariable Long id) {

        boolean deleted = studentService.delete(id);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}