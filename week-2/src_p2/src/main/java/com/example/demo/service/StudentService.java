package com.example.demo.service;

import com.example.demo.model.PageResponse;
import com.example.demo.model.Student;
import com.example.demo.model.Department;
import com.example.demo.repository.DepartmentRepository;
import com.example.demo.repository.StudentRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;

    public StudentService(StudentRepository studentRepository, DepartmentRepository departmentRepository) {
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
    }

    // GET /students?page=1&size=10
    public PageResponse<Student> getAll(int page, int size) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").ascending());

        Page<Student> result = studentRepository.findAll(pageable);

        return new PageResponse<>(
                result.getContent(),
                page,
                size,
                result.getTotalPages(),
                result.getTotalElements());
    }

    // GET /students/{id}
    public Student getById(Long id) {

        return studentRepository
                .findById(id)
                .orElse(null);
    }

    // POST /students
    public Student create(Student student) {

        // ID do Database tự sinh
        student.setId(null);

        return studentRepository.save(student);
    }

    // PUT /students/{id}
    public Student update(
            Long id,
            Student newStudent) {

        Student student = getById(id);

        if (student == null) {
            return null;
        }

        student.setName(newStudent.getName());
        student.setAge(newStudent.getAge());

        return studentRepository.save(student);
    }

    // DELETE /students/{id}
    public boolean delete(Long id) {

        Student student = getById(id);

        if (student == null) {
            return false;
        }

        Department department = student.getDepartment();

        if (department != null) {
            department.getStudents().remove(student);
        }

        studentRepository.delete(student);

        return true;
    }

    // PUT /students/{studentId}/department/{departmentId}
    public Student changeDepartment(Long studentId, Long departmentId) {
        Student student = getById(studentId);

        Department newDepartment = departmentRepository
                .findById(departmentId)
                .orElse(null);

        if (student == null || newDepartment == null) {
            return null;
        }

        Department oldDepartment = student.getDepartment();

        if (oldDepartment != null) {
            oldDepartment.getStudents().remove(student);
        }

        newDepartment.getStudents().add(student);

        student.setDepartment(newDepartment);

        return studentRepository.save(student);
    }
}