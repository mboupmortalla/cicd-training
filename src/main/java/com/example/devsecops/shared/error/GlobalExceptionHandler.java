package com.example.devsecops.shared.error;

import com.example.devsecops.order.domain.InvalidOrderTransitionException;
import com.example.devsecops.order.exception.OrderNotFoundException;
import com.example.devsecops.order.exception.UnknownProductException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;

/**
 * Regle unique de ce fichier : le detail va dans les logs, le client
 * recoit un message generique. Une stack trace ou un message SQL renvoye
 * au client, c'est de la reconnaissance offerte a un attaquant.
 *
 * On etend ResponseEntityExceptionHandler pour que les exceptions internes
 * de Spring (405, media type, URL inconnue...) gardent leur traitement
 * correct au lieu de tomber dans le fourre-tout 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- 409 : conflit d'etat ----------

    @ExceptionHandler(InvalidOrderTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidOrderTransitionException exception) {
        log.warn("Invalid order transition: {}", exception.getMessage());
        return problem(HttpStatus.CONFLICT,
                "Invalid order transition",
                "This operation is not allowed for the current order state");
    }

    /**
     * Deux requetes ont modifie la meme commande en meme temps.
     * Le perdant recoit 409 : qu'il relise et rejoue s'il le souhaite.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrentUpdate(OptimisticLockingFailureException exception) {
        log.warn("Concurrent modification detected: {}", exception.getMessage());
        return problem(HttpStatus.CONFLICT,
                "Concurrent modification",
                "The order was modified concurrently, please retry");
    }

    // ---------- 404 ----------

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFound(OrderNotFoundException exception) {
        log.info("Order not found: {}", exception.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Order not found", "Order not found");
    }

    // ---------- 400 ----------

    @ExceptionHandler(UnknownProductException.class)
    public ProblemDetail handleUnknownProduct(UnknownProductException exception) {
        log.info("Unknown product referenced: {}", exception.getMessage());
        return problem(HttpStatus.BAD_REQUEST,
                "Invalid request",
                "One or more products in this order do not exist");
    }

    /** Invariants du domaine violes (quantite, prix, liste vide...). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        log.warn("Domain invariant violated: {}", exception.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", "The request is not valid");
    }

    /**
     * Echec de @Valid. Ici on peut detailler : les messages viennent de NOS
     * annotations et les noms de champs sont ceux du contrat public.
     * Aucune information interne ne fuit.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::describe)
                .toList();

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST,
                "Invalid request", "Request validation failed");
        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }

    // ---------- 500 : filet de securite ----------

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal error", "An unexpected error occurred");
    }

    private static Map<String, String> describe(FieldError fieldError) {
        String message = fieldError.getDefaultMessage();
        return Map.of("field", fieldError.getField(),
                "message", message == null ? "invalid value" : message);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
