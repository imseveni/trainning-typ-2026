package com.example.demo.service;

import com.example.demo.model.Student;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StudentService {

    private final List<Student> students = new ArrayList<>();

    private long nextId = 1;

    public StudentService() {
        students.add(new Student(nextId++, "Nguyen Van A", 20));
        students.add(new Student(nextId++, "Tran Van B", 19));
        students.add(new Student(nextId++, "Le Van C", 21));
    }

    // Lấy toàn bộ sinh viên
    public List<Student> getAll() {
        return students;
    }

    // Tìm sinh viên theo id
    public Student getById(Long id) {
        for (Student student : students) {
            if (student.getId().equals(id)) {
                return student;
            }
        }

        return null;
    }

    // Thêm sinh viên
    public Student create(Student student) {

        student.setId(nextId++);

        students.add(student);

        return student;
    }

    // Cập nhật sinh viên
    public Student update(Long id, Student newStudent) {

        Student student = getById(id);

        if (student == null) {
            return null;
        }

        student.setName(newStudent.getName());
        student.setAge(newStudent.getAge());

        return student;
    }

    // Xóa sinh viên
    public boolean delete(Long id) {

        Student student = getById(id);

        if (student == null) {
            return false;
        }

        students.remove(student);

        return true;
    }
}