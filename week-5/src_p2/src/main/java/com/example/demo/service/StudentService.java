package com.example.demo.service;

import com.example.demo.model.Student;
import com.example.demo.repository.StudentRepository;

import org.springframework.stereotype.Service;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final RedisService redisService;

    public StudentService(
            StudentRepository studentRepository,
            RedisService redisService) {

        this.studentRepository = studentRepository;
        this.redisService = redisService;
    }

    // GET /students/{id}
    public Student getById(Long id) {

        String key = "student:" + id;

        // Kiểm tra Redis trước
        Object cachedStudent = redisService.get(key);

        if (cachedStudent != null) {

            System.out.println("CACHE HIT: " + key);

            return (Student) cachedStudent;
        }

        System.out.println("CACHE MISS: " + key);

        // Không có cache -> tìm DB
        Student student = studentRepository.findById(id)
                .orElse(null);

        if (student == null) {
            return null;
        }

        // Cache 60 giây
        redisService.set(
                key,
                student,
                60);

        return student;
    }

    // POST /students
    public Student create(Student student) {

        // ID do Database tự sinh
        student.setId(null);

        return studentRepository.save(student);
    }

    // PUT /students/{id}
    public Student update(Long id, Student newStudent) {

        Student student = studentRepository.findById(id)
                .orElse(null);

        if (student == null) {
            return null;
        }

        student.setName(newStudent.getName());
        student.setAge(newStudent.getAge());

        Student updatedStudent = studentRepository.save(student);

        // Xóa cache cũ
        redisService.delete("Student:" + id);

        return updatedStudent;
    }

    // DELETE /students/{id}
    public boolean delete(Long id) {

        Student student = studentRepository.findById(id)
                .orElse(null);

        if (student == null) {
            return false;
        }

        studentRepository.delete(student);

        // Xóa cache
        redisService.delete("student:" + id);

        return true;
    }
}