package com.exemple.historique.controller;

import com.exemple.historique.client.CalculClient;
import com.exemple.historique.model.Operation;
import com.exemple.historique.service.OperationService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/operations")
@RequiredArgsConstructor
public class OperationController {

    private final OperationService operationService;
    private final CalculClient calculClient;

    private static final List<String> OPERATEURS =
            List.of("somme", "soustraction", "produit", "division");

    // Demande le resultat a CALCUL-SERVICE (instance choisie par LoadBalancer)
    // et le range dans l'operation. Retourne une reponse d'erreur si le calcul
    // est impossible, null sinon.
    private ResponseEntity<Object> calculer(Operation op) {
        if (!OPERATEURS.contains(op.getOperateur())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("erreur", "Operateur inconnu : " + op.getOperateur()));
        }
        try {
            String corps = calculClient.calculer(op.getOperateur(), op.getA(), op.getB());
            op.setResultat(Double.parseDouble(corps));
            return null;
        } catch (FeignException.BadRequest e) {
            // calcul-service a repondu 400 (division par 0) : on transmet le message
            return ResponseEntity.badRequest()
                    .body(Map.of("erreur", e.contentUTF8()));
        } catch (FeignException e) {
            // Aucune instance disponible ou injoignable
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("erreur", "calcul-service injoignable"));
        }
    }

    @PostMapping
    public ResponseEntity<Object> creer(@RequestBody Operation op) {
        ResponseEntity<Object> erreur = calculer(op);
        if (erreur != null) {
            return erreur;
        }
        Operation saved = operationService.save(op);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @GetMapping
    public List<Operation> lister() {
        return operationService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Operation> lire(@PathVariable("id") int id) {
        Operation op = operationService.findById(id);
        if (op == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(op);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> modifier(@PathVariable("id") int id,
                                           @RequestBody Operation op) {
        Operation existant = operationService.findById(id);
        if (existant == null) {
            return ResponseEntity.notFound().build();
        }
        op.setId(id);
        ResponseEntity<Object> erreur = calculer(op);
        if (erreur != null) {
            return erreur;
        }
        operationService.save(op);
        return ResponseEntity.ok(op);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable("id") int id) {
        Operation op = operationService.findById(id);
        if (op == null) {
            return ResponseEntity.notFound().build();
        }
        operationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}