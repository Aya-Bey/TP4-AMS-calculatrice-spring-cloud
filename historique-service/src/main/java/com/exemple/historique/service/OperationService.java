package com.exemple.historique.service;

import com.exemple.historique.model.Operation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OperationService {

    private final Map<Integer, Operation> operations = new HashMap<>();
    private int counter = 1;

    public Operation save(Operation op) {
        if (op.getId() == 0) {
            op.setId(counter++);
        }
        operations.put(op.getId(), op);
        return op;
    }

    public List<Operation> findAll() {
        return new ArrayList<>(operations.values());
    }

    public Operation findById(int id) {
        return operations.get(id);
    }

    public void deleteById(int id) {
        operations.remove(id);
    }

    public Operation supprimer(int id) {
        return operations.remove(id);
    }
}