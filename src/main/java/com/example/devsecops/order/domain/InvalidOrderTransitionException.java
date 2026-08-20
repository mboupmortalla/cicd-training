package com.example.devsecops.order.domain;

/**
 * Levee par le domaine quand une transition d'etat est interdite.
 * Vit dans le package domain : c'est une regle metier, pas de la plomberie.
 */
public class InvalidOrderTransitionException extends RuntimeException {
    public InvalidOrderTransitionException(String message) {
        super(message);
    }
}
