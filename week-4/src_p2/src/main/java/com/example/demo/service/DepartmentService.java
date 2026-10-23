package com.example.demo.service;

import org.springframework.stereotype.Service;
import java.util.List;

import com.example.demo.model.PageResponse;
import com.example.demo.model.Department;
import com.example.demo.model.Student;
import com.example.demo.repository.DepartmentRepository;
import com.example.demo.repository.StudentRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            StudentRepository studentRepository) {

        this.departmentRepository = departmentRepository;

        this.studentRepository = studentRepository;
    }

    // GET /departments?page=1&size=10
    public PageResponse<Department> getAll(int page, int size) {

        Pageable pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by("id").ascending());

        Page<Department> result = departmentRepository.findAll(pageable);

        return new PageResponse<>(
                result.getContent(),
                page,
                size,
                result.getTotalPages(),
                result.getTotalElements());
    }

    // GET /departments/{id}
    public Department getById(Long id) {

        return departmentRepository
                .findById(id)
                .orElse(null);
    }

    // POST /departments
    public Department create(Department department) {

        department.setId(null);

        return departmentRepository.save(department);
    }

    // PUT /departments/{id}
    public Department update(Long id, Department newDepartment) {

        Department department = getById(id);

        if (department == null) {
            return null;
        }

        department.setName(newDepartment.getName());

        return departmentRepository.save(department);
    }

    // DELETE /departments/{id}
    public boolean delete(Long id) {
        Department department = getById(id);

        if (department == null) {
            return false;
        }

        for (Student student : department.getStudents()) {
            student.setDepartment(null);
        }

        studentRepository.saveAll(department.getStudents());

        departmentRepository.delete(department);

        return true;
    }

    // GET /departments/id/students
    public List<Student> getStudents(Long id) {

        Department department = departmentRepository
                .findById(id)
                .orElse(null);

        if (department == null) {
            return null;
        }

        return department.getStudents();
    }
}
